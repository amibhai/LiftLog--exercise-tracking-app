import json
import sqlite3
import tempfile
import unittest
from pathlib import Path

from pipeline.library import build_library
from pipeline.outputs import find_schema, write_database
from tests.fixtures import FEDB_CONFIG, fedb

REPO_ROOT = Path(__file__).resolve().parents[3]
SCHEMA_DIR = REPO_ROOT / "core/database/schemas/io.github.amibhai.liftlog.core.database.LiftLogDatabase"
SOURCES = [
    {
        "key": "fedb",
        "name": "free-exercise-db",
        "url": "https://example.com",
        "license": "Unlicense",
        "exerciseCount": 2,
    },
]


@unittest.skipUnless(SCHEMA_DIR.exists(), "Room schema not exported yet")
class WriteDatabaseTest(unittest.TestCase):
    """Writes a small asset with the real exported Room schema and reads it back like the app does."""

    def setUp(self):
        library = build_library(
            [
                fedb("Romanian_Deadlift", "Romanian Deadlift", primary=("hamstrings",), secondary=("glutes",)),
                fedb("Dumbbell_Flyes", "Dumbbell Flyes", equipment="dumbbell"),
            ],
            [],
            FEDB_CONFIG,
            {},
            {"ranked": []},
            min_count=1,
        )
        self.schema_path = find_schema(SCHEMA_DIR)
        self.schema = json.loads(self.schema_path.read_text(encoding="utf-8"))["database"]
        self.tmp = tempfile.TemporaryDirectory()
        self.db_path = Path(self.tmp.name) / "liftlog.db"
        write_database(self.db_path, self.schema_path, library.exercises, SOURCES, 1_700_000_000_000, 1)
        self.connection = sqlite3.connect(self.db_path)

    def tearDown(self):
        self.connection.close()
        self.tmp.cleanup()

    def test_room_identity_and_version_match_the_schema(self):
        version = self.connection.execute("PRAGMA user_version").fetchone()[0]
        identity = self.connection.execute("SELECT identity_hash FROM room_master_table").fetchone()[0]

        self.assertEqual(version, self.schema["version"])
        self.assertEqual(identity, self.schema["identityHash"])

    def test_fts_index_finds_names_and_abbreviations(self):
        def search(match):
            return [
                row[0]
                for row in self.connection.execute(
                    "SELECT e.name FROM exercise e JOIN exercise_fts ON exercise_fts.rowid = e.rowid "
                    "WHERE exercise_fts MATCH ? ORDER BY e.name",
                    (match,),
                )
            ]

        self.assertEqual(search("rdl*"), ["Romanian Deadlift"])
        self.assertEqual(search("db* fly*"), ["Dumbbell Flyes"])

    def test_muscles_images_and_sources_are_written(self):
        primaries = self.connection.execute("SELECT muscle FROM exercise_muscle WHERE is_primary = 1 ORDER BY muscle")

        self.assertEqual([row[0] for row in primaries], ["CHEST", "HAMSTRINGS"])
        self.assertEqual(self.connection.execute("SELECT COUNT(*) FROM exercise_image").fetchone()[0], 2)
        self.assertEqual(self.connection.execute("SELECT name FROM library_source").fetchone()[0], "free-exercise-db")


if __name__ == "__main__":
    unittest.main()
