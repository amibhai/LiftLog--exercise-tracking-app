#!/usr/bin/env python3
"""Builds LiftLog's built-in exercise library (SPEC §3).

Reads free-exercise-db (pinned commit) and the committed wger snapshot, normalizes and dedupes them
into the app's taxonomy, then writes:
  core/database/src/main/assets/database/liftlog.db   prepopulated Room database
  tools/exercise-pipeline/out/exercises.json          JSON copy of the library
  tools/exercise-pipeline/out/report.md               validation report
Exits non-zero when validation fails. See README.md.
"""

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path

from pipeline import outputs, report, sources
from pipeline.library import build_library

TOOL_DIR = Path(__file__).resolve().parent
REPO_ROOT = TOOL_DIR.parents[1]
SCHEMA_DIR = REPO_ROOT / "core/database/schemas/io.github.amibhai.liftlog.core.database.LiftLogDatabase"
ASSET_PATH = REPO_ROOT / "core/database/src/main/assets/database/liftlog.db"
OUT_DIR = TOOL_DIR / "out"


def _read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def library_sources(config: dict, snapshot: dict, counts) -> list[dict]:
    fedb = config["freeExerciseDb"]
    wger = config["wger"]
    return [
        {
            "key": "fedb",
            "name": "free-exercise-db",
            "url": fedb["url"],
            "license": "Unlicense (public domain)",
            "licenseUrl": "https://unlicense.org/",
            "revision": f"commit {fedb['commit'][:7]} ({fedb['commitDate']})",
            "notice": "Exercise data and images by the free-exercise-db contributors, dedicated to the public domain.",
            "exerciseCount": counts["fedb"],
        },
        {
            "key": "wger",
            "name": "wger",
            "url": wger["url"],
            "license": "CC BY-SA 3.0 / CC BY-SA 4.0 / CC0 (stated per exercise)",
            "licenseUrl": "https://creativecommons.org/licenses/by-sa/4.0/",
            "revision": f"API snapshot {snapshot['fetchedAt'][:10]}",
            "notice": "Exercise names, descriptions and images from wger.de, © their authors (credited on each "
            "exercise) and used under the license listed with each exercise.",
            "exerciseCount": counts["wger"],
        },
    ]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--refresh-wger", action="store_true", help="download a fresh wger snapshot first")
    parser.add_argument("--skip-db", action="store_true", help="write the JSON and report but not the database")
    args = parser.parse_args(argv)

    config = _read_json(TOOL_DIR / "data/sources.json")
    snapshot_path = TOOL_DIR / config["wger"]["snapshot"]
    if args.refresh_wger or not snapshot_path.exists():
        sources.refresh_wger_snapshot(config["wger"]["api"], snapshot_path)
    snapshot = sources.load_wger_snapshot(snapshot_path)
    fedb_records = sources.load_fedb(config["freeExerciseDb"], TOOL_DIR / ".cache")

    result = build_library(
        fedb_records,
        snapshot["exercises"],
        config["freeExerciseDb"],
        _read_json(TOOL_DIR / "data/overrides.json"),
        _read_json(TOOL_DIR / "data/popular.json"),
        min_count=config["minimumExercises"],
    )
    source_list = library_sources(config, snapshot, result.source_counts)

    OUT_DIR.mkdir(exist_ok=True)
    outputs.write_json(OUT_DIR / "exercises.json", result.exercises, source_list, config["libraryVersion"])
    (OUT_DIR / "report.md").write_text(report.render(result, source_list, config), encoding="utf-8")

    if result.errors:
        print(f"Validation FAILED with {len(result.errors)} problems (see out/report.md):", file=sys.stderr)
        for error in result.errors[:40]:
            print(f"  - {error}", file=sys.stderr)
        return 1

    if not args.skip_db:
        built_at_ms = int(datetime.fromisoformat(config["builtAt"].replace("Z", "+00:00")).timestamp() * 1000)
        schema_path = outputs.find_schema(SCHEMA_DIR)
        outputs.write_database(
            ASSET_PATH,
            schema_path,
            result.exercises,
            source_list,
            built_at_ms,
            config["libraryVersion"],
        )
        print(f"Wrote {ASSET_PATH.relative_to(REPO_ROOT)} (schema {schema_path.name})")

    print(
        f"{len(result.exercises)} exercises: {result.source_counts['fedb']} free-exercise-db + "
        f"{result.source_counts['wger']} wger; {len(result.duplicates)} duplicates merged, "
        f"{len(result.excluded)} excluded.",
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
