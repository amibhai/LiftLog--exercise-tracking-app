"""Writes the JSON copy and the prepopulated Room database asset."""

import json
import os
import sqlite3
from pathlib import Path

from .library import Exercise, stable_id


def find_schema(schema_dir: Path) -> Path:
    """The exported Room schema of the newest database version."""
    files = sorted(schema_dir.glob("*.json"), key=lambda p: int(p.stem))
    if not files:
        raise FileNotFoundError(
            f"No exported Room schema in {schema_dir}. Build core:database first: ./gradlew :core:database:kspDebugKotlin",
        )
    return files[-1]


def exercise_json(exercise: Exercise) -> dict:
    return {
        "id": exercise.id,
        "name": exercise.name,
        "aliases": exercise.aliases,
        "searchTerms": exercise.search_terms,
        "type": exercise.exercise_type,
        "equipment": exercise.equipment,
        "category": exercise.category,
        "mechanic": exercise.mechanic,
        "primaryMuscles": exercise.primary,
        "secondaryMuscles": exercise.secondary,
        "unilateral": exercise.unilateral,
        "instructions": exercise.instructions,
        "images": exercise.images,
        "popularityRank": exercise.popularity_rank,
        "sourceRef": exercise.source_ref,
        "license": exercise.license,
        "licenseUrl": exercise.license_url,
        "licenseAuthor": exercise.license_author,
        "mergedFrom": exercise.merged_from,
    }


def write_json(path: Path, exercises: list[Exercise], sources: list[dict], library_version: int) -> None:
    payload = {
        "libraryVersion": library_version,
        "count": len(exercises),
        "sources": sources,
        "exercises": [exercise_json(e) for e in sorted(exercises, key=lambda e: (e.name.lower(), e.id))],
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")


def write_database(
    path: Path,
    schema_path: Path,
    exercises: list[Exercise],
    sources: list[dict],
    built_at_ms: int,
    library_version: int,
) -> None:
    """Creates the database with Room's own exported SQL, so the asset matches the compiled schema.

    The identity hash in room_master_table and PRAGMA user_version are what Room checks when it copies
    the asset on first launch.
    """
    schema = json.loads(schema_path.read_text(encoding="utf-8"))["database"]
    entities = {e["tableName"]: e for e in schema["entities"]}
    tmp = path.with_name(path.name + ".tmp")
    tmp.unlink(missing_ok=True)
    path.parent.mkdir(parents=True, exist_ok=True)

    connection = sqlite3.connect(tmp)
    try:
        for entity in schema["entities"]:
            connection.execute(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
            for index in entity.get("indices", []):
                connection.execute(index["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
        for view in schema.get("views", []):
            connection.execute(view["createSql"].replace("${VIEW_NAME}", view["viewName"]))
        for entity in schema["entities"]:
            for trigger in entity.get("contentSyncTriggers", []):
                connection.execute(trigger)
        for query in schema["setupQueries"]:
            connection.execute(query)
        connection.execute(f"PRAGMA user_version = {int(schema['version'])}")

        # Room's triggers fill exercise_fts as exercises are inserted. No VACUUM afterwards: it may
        # renumber exercise rowids, which the external-content FTS index is keyed on.
        for table, rows in _rows(exercises, sources, built_at_ms, library_version).items():
            _insert(connection, entities[table], rows)
        connection.execute("INSERT INTO exercise_fts(exercise_fts) VALUES('optimize')")
        connection.commit()
    finally:
        connection.close()
    os.replace(tmp, path)


def _insert(connection: sqlite3.Connection, entity: dict, rows: list[dict]) -> None:
    table = entity["tableName"]
    columns = [f["columnName"] for f in entity["fields"]]
    required = {f["columnName"] for f in entity["fields"] if f.get("notNull")}
    for row in rows:
        unknown = set(row) - set(columns)
        missing = {c for c in required if row.get(c) is None}
        if unknown or missing:
            raise ValueError(
                f"{table}: row does not match the Room schema (unknown {sorted(unknown)}, missing {sorted(missing)})",
            )
    column_list = ", ".join(f"`{c}`" for c in columns)
    placeholders = ", ".join("?" for _ in columns)
    connection.executemany(
        f"INSERT INTO `{table}` ({column_list}) VALUES ({placeholders})",
        [tuple(row.get(c) for c in columns) for row in rows],
    )


def _rows(exercises: list[Exercise], sources: list[dict], built_at_ms: int, library_version: int) -> dict:
    stamp = {"created_at": built_at_ms, "updated_at": built_at_ms, "deleted_at": None}
    exercise_rows, muscle_rows, image_rows = [], [], []
    for exercise in sorted(exercises, key=lambda e: e.id):
        exercise_rows.append(
            {
                "id": exercise.id,
                "name": exercise.name,
                "aliases": "\n".join(exercise.aliases),
                "search_terms": exercise.search_terms,
                "type": exercise.exercise_type,
                "equipment": exercise.equipment,
                "category": exercise.category,
                "mechanic": exercise.mechanic,
                "instructions": "\n".join(exercise.instructions),
                "is_unilateral": exercise.unilateral,
                "default_rest_s": None,
                "weight_step_kg": None,
                "weight_step_lb": None,
                "bar_weight_kg": None,
                "notes": None,
                "is_favorite": False,
                "is_archived": False,
                "is_custom": False,
                "popularity_rank": exercise.popularity_rank,
                "source_ref": exercise.source_ref,
                "license": exercise.license,
                "license_url": exercise.license_url,
                "license_author": exercise.license_author,
                "library_version": library_version,
                "user_modified_at": None,
                **stamp,
            },
        )
        # Primary muscles first, in source order: the app shows the first as "the" primary muscle.
        for muscle, is_primary in [(m, True) for m in exercise.primary] + [(m, False) for m in exercise.secondary]:
            muscle_rows.append(
                {
                    "id": stable_id("exercise-muscle", exercise.source_ref, muscle),
                    "exercise_id": exercise.id,
                    "muscle": muscle,
                    "is_primary": is_primary,
                    **stamp,
                },
            )
        for position, image in enumerate(exercise.images):
            image_rows.append(
                {
                    "id": stable_id("exercise-image", exercise.source_ref, str(position)),
                    "exercise_id": exercise.id,
                    "position": position,
                    "url": image["url"],
                    "thumbnail_url": image["thumbnailUrl"],
                    "license": image["license"],
                    "author": image["author"],
                    **stamp,
                },
            )
    source_rows = [
        {
            "id": stable_id("library-source", source["key"]),
            "name": source["name"],
            "url": source["url"],
            "license": source["license"],
            "license_url": source.get("licenseUrl"),
            "revision": source.get("revision"),
            "notice": source.get("notice"),
            "exercise_count": source["exerciseCount"],
            **stamp,
        }
        for source in sources
    ]
    return {
        "exercise": exercise_rows,
        "exercise_muscle": muscle_rows,
        "exercise_image": image_rows,
        "library_source": source_rows,
    }
