import unittest

from pipeline import classify


class EquipmentTest(unittest.TestCase):
    def test_named_implement_beats_the_source(self):
        cases = [
            ("Smith Machine Squat", "MACHINE", "SMITH"),
            ("Close-Grip EZ Bar Curl", "BARBELL", "EZ_BAR"),
            ("Trap Bar Deadlift", "BARBELL", "TRAP_BAR"),
            ("Kettlebell One Legged Deadlift", "DUMBBELL", "KETTLEBELL"),
            ("Plate Twist", "OTHER", "PLATE"),
            ("Bodyweight Walking Lunge", None, "BODYWEIGHT"),
        ]
        for name, source, expected in cases:
            with self.subTest(name=name):
                self.assertEqual(classify.refine_equipment(name, source), expected)

    def test_bands_or_chains_as_accessory_keep_the_main_implement(self):
        self.assertEqual(classify.refine_equipment("Squat with Bands", "BARBELL"), "BARBELL")
        self.assertEqual(classify.refine_equipment("Bench Press with Chains", "BARBELL"), "BARBELL")

    def test_bodyweight_movements_without_equipment_are_bodyweight(self):
        self.assertEqual(classify.refine_equipment("Ring Dips", "OTHER"), "BODYWEIGHT")
        self.assertEqual(classify.refine_equipment("Hip Flexor Stretch", None, "MOBILITY"), "BODYWEIGHT")

    def test_loaded_strength_movement_with_unknown_implement_is_other(self):
        self.assertEqual(classify.refine_equipment("Lateral Raise", None, "STRENGTH"), "OTHER")


class MuscleTest(unittest.TestCase):
    def test_shoulders_split_into_front_side_and_rear_delts(self):
        self.assertEqual(classify.resolve_muscles("Side Lateral Raise", ["DELTS"], []), (["SIDE_DELTS"], []))
        self.assertEqual(classify.resolve_muscles("Reverse Dumbbell Fly", ["DELTS"], []), (["REAR_DELTS"], []))
        self.assertEqual(classify.resolve_muscles("Face Pull", ["DELTS"], []), (["REAR_DELTS"], []))
        self.assertEqual(classify.resolve_muscles("Front Raise", ["DELTS"], []), (["FRONT_DELTS"], []))

    def test_overhead_presses_also_train_side_delts(self):
        self.assertEqual(
            classify.resolve_muscles("Barbell Overhead Press", ["DELTS"], ["TRICEPS"]),
            (["FRONT_DELTS"], ["TRICEPS", "SIDE_DELTS"]),
        )

    def test_rotation_moves_target_obliques(self):
        self.assertEqual(classify.resolve_muscles("Russian Twist", ["ABS"], []), (["OBLIQUES"], ["ABS"]))

    def test_missing_muscles_are_inferred_from_name_then_category(self):
        self.assertEqual(classify.infer_muscles("Hammer Curl"), (["BICEPS"], ["FOREARMS"]))
        self.assertEqual(classify.infer_muscles("Mystery Move", "Calves"), (["CALVES"], []))
        self.assertEqual(classify.infer_muscles("Mystery Move"), ([], []))


class ExerciseTypeTest(unittest.TestCase):
    def test_type_decides_the_set_fields(self):
        cases = [
            ("Band Assisted Pull-Up", "STRENGTH", "BAND", "BODYWEIGHT_ASSISTED"),
            ("Farmer's Walk", "STRONGMAN", "OTHER", "WEIGHT_DISTANCE"),
            ("Plank", "STRENGTH", "BODYWEIGHT", "DURATION"),
            ("Pull-Up", "STRENGTH", "BODYWEIGHT", "BODYWEIGHT_ADDED"),
            ("Chest Dip", "STRENGTH", "BODYWEIGHT", "BODYWEIGHT_ADDED"),
            ("Crunch", "STRENGTH", "BODYWEIGHT", "REPS_ONLY"),
            ("Running, Treadmill", "CARDIO", "MACHINE", "DISTANCE_DURATION"),
            ("Burpees", "CARDIO", "BODYWEIGHT", "REPS_ONLY"),
            ("Hip Flexor Stretch", "MOBILITY", "BODYWEIGHT", "DURATION"),
            ("Barbell Back Squat", "STRENGTH", "BARBELL", "WEIGHT_REPS"),
            ("Hang Clean", "OLYMPIC", "BARBELL", "WEIGHT_REPS"),
            ("Band Pull Apart", "STRENGTH", "BAND", "REPS_ONLY"),
        ]
        for name, category, equipment, expected in cases:
            with self.subTest(name=name):
                self.assertEqual(classify.infer_type(name, category, equipment), expected)


class OtherFlagsTest(unittest.TestCase):
    def test_unilateral(self):
        self.assertTrue(classify.infer_unilateral("One-Arm Dumbbell Row"))
        self.assertTrue(classify.infer_unilateral("Bulgarian Split Squat"))
        self.assertFalse(classify.infer_unilateral("Barbell Bench Press"))

    def test_mechanic(self):
        self.assertEqual(classify.infer_mechanic("Barbell Row", None), "COMPOUND")
        self.assertEqual(classify.infer_mechanic("Cable Curl", None), "ISOLATION")
        self.assertEqual(classify.infer_mechanic("Cable Curl", "COMPOUND"), "COMPOUND")
        self.assertIsNone(classify.infer_mechanic("Mystery Move", None))


if __name__ == "__main__":
    unittest.main()
