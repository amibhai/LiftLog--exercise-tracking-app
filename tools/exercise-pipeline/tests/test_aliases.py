import unittest

from pipeline.aliases import generate


class AliasesTest(unittest.TestCase):
    def test_common_abbreviations_become_search_terms(self):
        _, terms = generate("Romanian Deadlift", "BARBELL", ["HAMSTRINGS"], [])
        for term in ("rdl", "bb", "barbell", "hamstrings"):
            self.assertIn(term, terms.split())

    def test_skullcrusher_and_lying_triceps_extension_find_each_other(self):
        aliases, terms = generate("EZ-Bar Skullcrusher", "EZ_BAR", ["TRICEPS"], [])
        self.assertIn("Lying Triceps Extension", aliases)
        self.assertIn("ez", terms.split())

    def test_source_names_are_kept_as_aliases_but_never_repeat_the_name(self):
        aliases, terms = generate(
            "Barbell Overhead Press",
            "BARBELL",
            ["FRONT_DELTS"],
            ["Standing Military Press", "barbell overhead press"],
        )
        self.assertEqual(aliases, ["Standing Military Press"])
        self.assertIn("ohp", terms.split())

    def test_implement_nicknames_stay_hidden_search_terms(self):
        aliases, terms = generate("Smith Machine Close-Grip Bench Press", "SMITH", ["TRICEPS"], [])
        self.assertEqual(aliases, [])
        self.assertIn("cgbp", terms.split())

    def test_dumbbell_exercises_are_findable_as_db(self):
        _, terms = generate("Dumbbell Fly", "DUMBBELL", ["CHEST"], [])
        self.assertIn("db", terms.split())


if __name__ == "__main__":
    unittest.main()
