"""The one fixed taxonomy (SPEC §3) and the mappings from each source into it.

The value names must match the Kotlin enums in core:domain exactly; the app's asset test
fails on any value it cannot parse.
"""

MUSCLES = (
    "CHEST", "FRONT_DELTS", "SIDE_DELTS", "REAR_DELTS", "BICEPS", "TRICEPS", "FOREARMS",
    "LATS", "UPPER_BACK", "LOWER_BACK", "ABS", "OBLIQUES", "GLUTES", "QUADS", "HAMSTRINGS",
    "ADDUCTORS", "ABDUCTORS", "CALVES", "NECK",
)
EQUIPMENT = (
    "BARBELL", "DUMBBELL", "KETTLEBELL", "MACHINE", "CABLE", "SMITH", "BODYWEIGHT", "BAND",
    "EZ_BAR", "TRAP_BAR", "PLATE", "OTHER",
)
CATEGORIES = ("STRENGTH", "OLYMPIC", "PLYOMETRIC", "STRONGMAN", "CARDIO", "MOBILITY")
EXERCISE_TYPES = (
    "WEIGHT_REPS", "BODYWEIGHT_ADDED", "BODYWEIGHT_ASSISTED", "REPS_ONLY", "DURATION",
    "DISTANCE_DURATION", "WEIGHT_DISTANCE",
)
MECHANICS = ("COMPOUND", "ISOLATION")

# Placeholder for "shoulders"; classify.resolve_muscles() splits it into front/side/rear delts.
DELTS = "DELTS"

FEDB_MUSCLES = {
    "abdominals": "ABS",
    "abductors": "ABDUCTORS",
    "adductors": "ADDUCTORS",
    "biceps": "BICEPS",
    "calves": "CALVES",
    "chest": "CHEST",
    "forearms": "FOREARMS",
    "glutes": "GLUTES",
    "hamstrings": "HAMSTRINGS",
    "lats": "LATS",
    "lower back": "LOWER_BACK",
    "middle back": "UPPER_BACK",
    "neck": "NECK",
    "quadriceps": "QUADS",
    "shoulders": DELTS,
    "traps": "UPPER_BACK",
    "triceps": "TRICEPS",
}

# wger /api/v2/muscle/ ids.
WGER_MUSCLES = {
    1: "BICEPS",       # Biceps brachii
    2: DELTS,          # Anterior deltoid (wger has no side/rear delt entries)
    3: "CHEST",        # Serratus anterior
    4: "CHEST",        # Pectoralis major
    5: "TRICEPS",      # Triceps brachii
    6: "ABS",          # Rectus abdominis
    7: "CALVES",       # Gastrocnemius
    8: "GLUTES",       # Gluteus maximus
    9: "UPPER_BACK",   # Trapezius
    10: "QUADS",       # Quadriceps femoris
    11: "HAMSTRINGS",  # Biceps femoris
    12: "LATS",        # Latissimus dorsi
    13: "BICEPS",      # Brachialis
    14: "OBLIQUES",    # Obliquus externus abdominis
    15: "CALVES",      # Soleus
}

FEDB_EQUIPMENT = {
    "barbell": "BARBELL",
    "dumbbell": "DUMBBELL",
    "kettlebells": "KETTLEBELL",
    "machine": "MACHINE",
    "cable": "CABLE",
    "body only": "BODYWEIGHT",
    "bands": "BAND",
    "e-z curl bar": "EZ_BAR",
    "medicine ball": "OTHER",
    "exercise ball": "OTHER",
    "foam roll": "OTHER",
    "other": "OTHER",
}

# wger /api/v2/equipment/ ids. None = an accessory (bench, mat) that is not the loading implement.
WGER_EQUIPMENT = {
    1: "BARBELL",
    2: "EZ_BAR",       # "SZ-Bar"
    3: "DUMBBELL",
    4: None,           # Gym mat
    5: "OTHER",        # Swiss Ball
    6: "BODYWEIGHT",   # Pull-up bar
    7: "BODYWEIGHT",   # none (bodyweight exercise)
    8: None,           # Bench
    9: None,           # Incline bench
    10: "KETTLEBELL",
    11: "BAND",
    12: "CABLE",
}

# When an exercise lists several implements, the most specific loading implement wins.
EQUIPMENT_PRIORITY = (
    "SMITH", "TRAP_BAR", "EZ_BAR", "BARBELL", "DUMBBELL", "KETTLEBELL", "CABLE", "MACHINE",
    "BAND", "PLATE", "OTHER", "BODYWEIGHT",
)

FEDB_CATEGORIES = {
    "strength": "STRENGTH",
    "powerlifting": "STRENGTH",
    "olympic weightlifting": "OLYMPIC",
    "strongman": "STRONGMAN",
    "plyometrics": "PLYOMETRIC",
    "cardio": "CARDIO",
    "stretching": "MOBILITY",
}

WGER_CARDIO_CATEGORY_ID = 15

# wger /api/v2/license/ ids -> (short name, URL).
WGER_LICENSES = {
    1: ("CC-BY-SA 3.0", "https://creativecommons.org/licenses/by-sa/3.0/"),
    2: ("CC-BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/"),
    3: ("CC0 1.0", "https://creativecommons.org/publicdomain/zero/1.0/"),
    4: ("CC-BY 4.0", "https://creativecommons.org/licenses/by/4.0/"),
    5: ("ODbL", "https://opendatacommons.org/licenses/odbl/"),
}

FEDB_LICENSE = ("Unlicense", "https://unlicense.org/")
