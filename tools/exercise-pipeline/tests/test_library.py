import unittest

from pipeline.library import build_library, stable_id
from tests.fixtures import FEDB_CONFIG, fedb, wger

NO_OVERRIDES = {}
NO_POPULAR = {"ranked": []}


def build(fedb_records, wger_records, overrides=NO_OVERRIDES, popular=NO_POPULAR, min_count=1):
    return build_library(fedb_records, wger_records, FEDB_CONFIG, overrides, popular, min_count)


class BuildLibraryTest(unittest.TestCase):
    def test_wger_duplicate_merges_into_free_exercise_db_and_keeps_its_data(self):
        result = build(
            [fedb("Barbell_Bench_Press_-_Medium_Grip", "Barbell Bench Press - Medium Grip")],
            [wger("w1", "Bench Press", equipment=[1])],
        )

        self.assertEqual(len(result.exercises), 1)
        bench = result.exercises[0]
        self.assertEqual(bench.source_ref, "fedb:Barbell_Bench_Press_-_Medium_Grip")
        self.assertEqual(bench.license, "Unlicense")
        self.assertIn("Bench Press", bench.aliases)
        self.assertEqual(bench.merged_from, ["wger:w1"])
        self.assertEqual(result.errors, [])

    def test_wger_record_without_equipment_merges_by_movement_name(self):
        result = build([fedb("Sumo_Deadlift", "Sumo Deadlift", primary=("hamstrings",))], [wger("w1", "Sumo Deadlift", equipment=[])])

        self.assertEqual([e.source_ref for e in result.exercises], ["fedb:Sumo_Deadlift"])

    def test_same_name_with_a_different_known_implement_is_a_separate_variant(self):
        result = build(
            [fedb("Russian_Twist", "Russian Twist", equipment="body only", primary=("abdominals",))],
            [wger("w1", "Russian Twist", equipment=[3], muscles=[14], category="Abs", category_id=10)],
        )

        self.assertEqual(sorted(e.name for e in result.exercises), ["Russian Twist", "Russian Twist (Dumbbell)"])

    def test_ids_are_deterministic_uuids_of_the_source_ref(self):
        result = build([fedb("Plank", "Plank", equipment="body only", primary=("abdominals",))], [])

        self.assertEqual(result.exercises[0].id, stable_id("exercise", "fedb:Plank"))
        self.assertEqual(stable_id("exercise", "fedb:Plank"), stable_id("exercise", "fedb:Plank"))

    def test_non_english_and_placeholder_wger_names_are_excluded(self):
        result = build([], [wger("w1", "Remo alto polea alta"), wger("w2", "Test 2"), wger("w3", "Cable Fly")])

        self.assertEqual([e.name for e in result.exercises], ["Cable Fly"])
        self.assertEqual(sorted(reason for _, _, reason in result.excluded), ["name is not English", "placeholder name"])

    def test_curated_rename_keeps_the_source_name_searchable(self):
        result = build([fedb("Pullups", "Pullups", equipment="body only", primary=("lats",))], [], overrides={"rename": {"fedb:Pullups": "Pull-Up"}})

        pull_up = result.exercises[0]
        self.assertEqual(pull_up.name, "Pull-Up")
        self.assertEqual(pull_up.exercise_type, "BODYWEIGHT_ADDED")
        self.assertIn("Pullups", pull_up.aliases)

    def test_popularity_comes_from_the_ranked_list(self):
        result = build(
            [fedb("A", "Barbell Curl", primary=("biceps",)), fedb("B", "Cable Fly", equipment="cable")],
            [],
            popular={"ranked": ["fedb:B", "fedb:A"]},
        )

        self.assertEqual({e.source_ref: e.popularity_rank for e in result.exercises}, {"fedb:A": 2, "fedb:B": 1})

    def test_validation_reports_problems(self):
        result = build(
            [fedb("A", "Barbell Curl", primary=("biceps",))],
            [],
            overrides={"rename": {"fedb:Missing": "Nope"}},
            popular={"ranked": ["fedb:Gone"]},
            min_count=5,
        )

        joined = "\n".join(result.errors)
        self.assertIn("only 1 exercises", joined)
        self.assertIn("unknown exercise fedb:Missing", joined)
        self.assertIn("fedb:Gone", joined)


if __name__ == "__main__":
    unittest.main()
