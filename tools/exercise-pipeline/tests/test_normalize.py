import unittest

from pipeline.normalize import clean_display_name, dedupe_key, search_normalize


class SearchNormalizeTest(unittest.TestCase):
    def test_lowercase_ascii_words(self):
        self.assertEqual(search_normalize("Pull-Up"), "pull up")
        self.assertEqual(search_normalize("Développé  Couché"), "developpe couche")
        self.assertEqual(search_normalize("Farmer's Walk"), "farmer s walk")
        self.assertEqual(search_normalize("!!!"), "")


class DedupeKeyTest(unittest.TestCase):
    def test_same_movement_spelled_differently_shares_a_key(self):
        pairs = [
            ("Barbell Bench Press - Medium Grip", "Bench Press"),
            ("Pullups", "Pull-Up"),
            ("Dumbbell Curls", "Curl"),
            ("Incline Dumbbell Press", "Dumbbell Incline Press"),
            ("Side Lateral Raise", "Lateral Raises"),
            ("Wide-Grip Lat Pulldown", "Wide Grip Pulldown"),
            ("Bicep Curl", "Biceps Curls"),
            ("Weighted Pull Ups", "Pull-Up"),
            ("Leg Curls (Sitting)", "Seated Leg Curl"),
        ]
        for a, b in pairs:
            with self.subTest(a=a, b=b):
                self.assertEqual(dedupe_key(a, "X"), dedupe_key(b, "X"))

    def test_different_equipment_or_variant_gets_a_different_key(self):
        self.assertNotEqual(dedupe_key("Bench Press", "BARBELL"), dedupe_key("Bench Press", "DUMBBELL"))
        self.assertNotEqual(dedupe_key("Close-Grip Bench Press", "BARBELL"), dedupe_key("Bench Press", "BARBELL"))
        self.assertNotEqual(dedupe_key("Squat with Bands", "BARBELL"), dedupe_key("Squat", "BARBELL"))
        self.assertNotEqual(dedupe_key("Front Squat", "BARBELL"), dedupe_key("Squat", "BARBELL"))


class CleanDisplayNameTest(unittest.TestCase):
    def test_title_case_keeps_small_words_and_acronyms(self):
        self.assertEqual(clean_display_name("biceps curls with sz-bar"), "Biceps Curls with EZ-Bar")
        self.assertEqual(
            clean_display_name("Straight-arm pull down (bar attachment)"),
            "Straight-Arm Pull Down (Bar Attachment)",
        )
        self.assertEqual(clean_display_name("Incline DB Y-Raise"), "Incline DB Y-Raise")
        self.assertEqual(clean_display_name("Dumbell  Row"), "Dumbbell Row")

    def test_drops_user_variant_markers_and_pipes(self):
        self.assertEqual(clean_display_name("Dips (Custom)"), "Dips")
        self.assertEqual(clean_display_name("Push-Ups | Decline"), "Push-Ups - Decline")


if __name__ == "__main__":
    unittest.main()
