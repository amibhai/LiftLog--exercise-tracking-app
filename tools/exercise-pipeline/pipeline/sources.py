"""Downloads and caches the two data sources. All network access in the pipeline happens here."""

import json
import time
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

USER_AGENT = "LiftLog-exercise-pipeline/1.0"
WGER_ENGLISH = 2


def _get(url: str, retries: int = 3, timeout: int = 60) -> bytes:
    last_error = None
    for attempt in range(retries):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(request, timeout=timeout) as response:
                return response.read()
        except OSError as error:
            last_error = error
            time.sleep(2 * (attempt + 1))
    raise RuntimeError(f"Download failed after {retries} attempts: {url}: {last_error}")


def fedb_exercises_url(fedb: dict) -> str:
    return f"https://raw.githubusercontent.com/{fedb['repo']}/{fedb['commit']}/dist/exercises.json"


def fedb_image_url(fedb: dict, path: str) -> str:
    """Pinned to a commit, so the URL never changes or breaks."""
    return f"https://raw.githubusercontent.com/{fedb['repo']}/{fedb['commit']}/exercises/{path}"


def load_fedb(fedb: dict, cache_dir: Path) -> list[dict]:
    """free-exercise-db at the pinned commit (cached; the commit makes it reproducible)."""
    cache_dir.mkdir(parents=True, exist_ok=True)
    path = cache_dir / f"free-exercise-db-{fedb['commit']}.json"
    if not path.exists():
        print(f"Downloading free-exercise-db @ {fedb['commit'][:7]} ...")
        path.write_bytes(_get(fedb_exercises_url(fedb)))
    return json.loads(path.read_text(encoding="utf-8"))


def refresh_wger_snapshot(api_base: str, snapshot_path: Path) -> None:
    """Fetches every wger exercise and stores the English fields we use, sorted for stable diffs.

    The live API changes over time, so the committed snapshot is what makes builds reproducible.
    """
    records = []
    url = f"{api_base}exerciseinfo/?limit=100&offset=0"
    while url:
        print(f"Fetching {url}")
        page = json.loads(_get(url))
        records.extend(page["results"])
        url = page.get("next")
    exercises = sorted((e for e in map(_slim_wger, records) if e), key=lambda e: e["uuid"])
    snapshot = {
        "source": f"{api_base}exerciseinfo/",
        "fetchedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "exercises": exercises,
    }
    snapshot_path.parent.mkdir(parents=True, exist_ok=True)
    snapshot_path.write_text(
        json.dumps(snapshot, indent=1, ensure_ascii=False) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def load_wger_snapshot(snapshot_path: Path) -> dict:
    return json.loads(snapshot_path.read_text(encoding="utf-8"))


def _slim_wger(record: dict) -> dict | None:
    english = next((t for t in record.get("translations", []) if t.get("language") == WGER_ENGLISH), None)
    if english is None:
        return None
    category = record.get("category") or {}
    license_ = record.get("license") or {}
    images = sorted(record.get("images", []), key=lambda i: (not i.get("is_main"), i["id"]))
    return {
        "uuid": record["uuid"],
        "id": record["id"],
        "category": category.get("name"),
        "categoryId": category.get("id"),
        "muscles": sorted(m["id"] for m in record.get("muscles", [])),
        "musclesSecondary": sorted(m["id"] for m in record.get("muscles_secondary", [])),
        "equipment": sorted(e["id"] for e in record.get("equipment", [])),
        "license": license_.get("id"),
        "licenseAuthor": record.get("license_author") or None,
        "name": english["name"],
        "description": english.get("description") or "",
        "aliases": sorted(a["alias"] for a in english.get("aliases", [])),
        "translationLicense": english.get("license"),
        "translationLicenseAuthor": english.get("license_author") or None,
        "images": [
            {
                "url": image["image"],
                "thumbnail": (image.get("thumbnails") or {}).get("small"),
                "license": image.get("license"),
                "licenseAuthor": image.get("license_author") or None,
                "aiGenerated": bool(image.get("is_ai_generated")),
            }
            for image in images
        ],
    }
