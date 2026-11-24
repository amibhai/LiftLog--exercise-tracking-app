"""Tiny source records in the shape of free-exercise-db and the slimmed wger snapshot."""

FEDB_CONFIG = {"repo": "owner/free-exercise-db", "commit": "0123456789abcdef"}


def fedb(record_id, name, equipment="barbell", primary=("chest",), secondary=(), category="strength", mechanic=None):
    return {
        "id": record_id,
        "name": name,
        "force": None,
        "level": "beginner",
        "mechanic": mechanic,
        "equipment": equipment,
        "primaryMuscles": list(primary),
        "secondaryMuscles": list(secondary),
        "instructions": ["Do the thing."],
        "category": category,
        "images": [f"{record_id}/0.jpg"],
    }


def wger(uuid, name, equipment=(1,), muscles=(4,), aliases=(), category="Chest", category_id=11):
    return {
        "uuid": uuid,
        "id": 1,
        "category": category,
        "categoryId": category_id,
        "muscles": list(muscles),
        "musclesSecondary": [],
        "equipment": list(equipment),
        "license": 2,
        "licenseAuthor": "someone",
        "name": name,
        "description": "<p>Step one.</p><p>Step two.</p>",
        "aliases": list(aliases),
        "translationLicense": 2,
        "translationLicenseAuthor": "author",
        "images": [],
    }
