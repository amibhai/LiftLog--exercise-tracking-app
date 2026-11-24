"""Name-based classification into the fixed taxonomy.

Sources are inconsistent (free-exercise-db lists Smith-machine lifts as "machine"; wger has only an
"anterior deltoid" and no machines), so the exercise name refines what each source says. All patterns
run against search-normalized text (lowercase ASCII words).
"""

import re

from . import taxonomy as tx
from .normalize import search_normalize

# An implement named in the exercise name beats the source's equipment field.
NAME_EQUIPMENT = (
    (r"\bsmith\b", "SMITH"),
    (r"\b(trap|hex) ?bar\b", "TRAP_BAR"),
    (r"\b(ez|e z|sz)( curl)? ?bar\b|\bez\b", "EZ_BAR"),
)

# Implement words in the name also beat the source (wger lists "Kettlebell One Legged Deadlift" with
# dumbbells). Checked in order, so "Cable Machine Row" is a cable exercise.
EQUIPMENT_HINTS = (
    (r"\bbarbells?\b", "BARBELL"),
    (r"\bdumbbells?\b|\bdb\b", "DUMBBELL"),
    (r"\bkettlebells?\b|\bkb\b", "KETTLEBELL"),
    (r"\bcables?\b|\bpulley\b", "CABLE"),
    (r"\bmachine\b|\blever(age)?\b|\bleg press\b|\bpec deck\b|\bhack squats?\b", "MACHINE"),
    (r"\bbands?\b|\bbanded\b", "BAND"),
    (r"\bplates?\b", "PLATE"),
    (r"\bbody ?weight\b|\bfreehand\b", "BODYWEIGHT"),
)

# Accessories that do not change the main implement ("Squat with Bands" is still a barbell squat).
ACCESSORY = r"\bwith (resistance )?(bands?|chains?)\b"

# Bodyweight movements commonly loaded with a belt or vest (SPEC §3 "bodyweight + added weight").
WEIGHTED_BODYWEIGHT = (
    r"\bpull ?ups?\b|\bpullups?\b|\bchin ?ups?\b|\bchinups?\b|\bdips?\b|\bpush ?ups?\b|\bpushups?\b"
    r"|\bmuscle ?ups?\b|\binverted rows?\b|\bback extensions?\b|\bhyperextensions?\b"
    r"|\bglute ham raises?\b|\bnordic\b|\bpistols?\b"
)

REAR_DELT = (
    r"\brear\b|\breverse (\w+ )?fl(y|ye|yes|ies)\b|\bface pulls?\b|\bbent over (low pulley )?(side )?laterals?\b"
    r"|\breverse pec|\bposterior delt|\bpull ?aparts?\b"
)
SIDE_DELT = r"\blaterals?\b|\bside raises?\b|\bupright rows?\b|\by raises?\b|\blu raises?\b"
OBLIQUE = (
    r"\bobliques?\b|\bside bends?\b|\brussian twists?\b|\bwood ?chop|\bside plank|\bwindmills?\b"
    r"|\bpallof\b|\btwists?\b|\brotations?\b|\bsaxon\b"
)
OVERHEAD_PRESS = r"\b(overhead|military|shoulder|arnold|push) press(es)?\b|\bpress overhead\b"

# Used when a source lists no muscles: first match wins -> (primary, secondary).
NAME_MUSCLES = (
    (r"\bcalf\b|\bcalves\b", ["CALVES"], []),
    (r"\bwrists?\b|\bforearms?\b|\bgrip\b|\bfingers?\b", ["FOREARMS"], []),
    (r"\bneck\b", ["NECK"], ["UPPER_BACK"]),
    (r"\bshrugs?\b", ["UPPER_BACK"], ["FOREARMS"]),
    (r"\btriceps?\b|\bpush ?downs?\b|\bpushdowns?\b|\bskull ?crushers?\b|\bfrench press\b", ["TRICEPS"], []),
    (REAR_DELT, ["REAR_DELTS"], ["UPPER_BACK"]),
    (r"\blateral raises?\b|\bside raises?\b|\bupright rows?\b", ["SIDE_DELTS"], ["UPPER_BACK"]),
    (r"\bfront raises?\b", ["FRONT_DELTS"], []),
    (OVERHEAD_PRESS, ["FRONT_DELTS"], ["SIDE_DELTS", "TRICEPS"]),
    (
        r"\bhip thrusts?\b|\bglutes?\b|\bkickbacks?\b|\bbridges?\b|\bdonkey kicks?\b|\bfire hydrants?\b",
        ["GLUTES"],
        ["HAMSTRINGS"],
    ),
    (
        r"\bromanian\b|\brdl\b|\bstiff ?legg?e?d?\b|\bgood ?mornings?\b|\bleg curls?\b|\bhamstrings?\b|\bnordic\b",
        ["HAMSTRINGS"],
        ["GLUTES", "LOWER_BACK"],
    ),
    (r"\bdeadlifts?\b", ["HAMSTRINGS"], ["GLUTES", "LOWER_BACK", "UPPER_BACK"]),
    (r"\bback extensions?\b|\bhyperextensions?\b|\bsupermans?\b", ["LOWER_BACK"], ["GLUTES"]),
    (r"\badduct", ["ADDUCTORS"], []),
    (r"\babduct|\bclams?(hells?)?\b|\bmonster walks?\b|\bband walks?\b", ["ABDUCTORS"], ["GLUTES"]),
    (
        r"\bsquats?\b|\blunges?\b|\bleg press\b|\bleg extensions?\b|\bstep ?ups?\b|\bpistols?\b|\bwall sits?\b|\bsissy\b",
        ["QUADS"],
        ["GLUTES"],
    ),
    (
        r"\bpull ?ups?\b|\bpullups?\b|\bchin ?ups?\b|\bchinups?\b|\bpull ?downs?\b|\bpulldowns?\b|\blats?\b|\bpullovers?\b",
        ["LATS"],
        ["BICEPS"],
    ),
    (r"\brows?\b|\browing\b", ["UPPER_BACK"], ["LATS", "BICEPS"]),
    (r"\bcurls?\b", ["BICEPS"], ["FOREARMS"]),
    (r"\bdips?\b", ["TRICEPS"], ["CHEST", "FRONT_DELTS"]),
    (
        r"\bbench\b|\bchest\b|\bpush ?ups?\b|\bpushups?\b|\bfl(y|ye|yes|ies)\b|\bpecs?\b|\bcrossovers?\b"
        r"|\bbutterfly\b|\bpress\b",
        ["CHEST"],
        ["TRICEPS", "FRONT_DELTS"],
    ),
    (OBLIQUE, ["OBLIQUES"], ["ABS"]),
    (
        r"\bcrunch(es)?\b|\bsit ?ups?\b|\bsitups?\b|\bplanks?\b|\bleg raises?\b|\babs?\b|\babdominals?\b"
        r"|\bhollow\b|\brollouts?\b|\bab wheel\b|\bv ups?\b|\bmountain climbers?\b|\bdead bugs?\b|\bcore\b",
        ["ABS"],
        [],
    ),
)

# wger category -> fallback primary muscle when neither muscles nor the name say anything.
WGER_CATEGORY_MUSCLES = {
    "Abs": "ABS",
    "Arms": "BICEPS",
    "Back": "UPPER_BACK",
    "Calves": "CALVES",
    "Cardio": "QUADS",
    "Chest": "CHEST",
    "Legs": "QUADS",
    "Shoulders": "FRONT_DELTS",
}

CARRY = (
    r"\bfarmers?\b|\bcarry\b|\bcarries\b|\byoke\b|\bsuitcase (walk|carry)\b|\bwaiters? walk\b"
    r"|\bsled (push|pull|drag)(es|s)?\b|\bprowler\b|\bhusafell\b|\bconan\b"
)
DISTANCE_CARDIO = (
    r"\brun(ning)?\b|\bjog(ging)?\b|\bsprints?\b|\btreadmill\b|\bbike\b|\bbicycl|\bcycling\b|\belliptical\b"
    r"|\browing machine\b|\brower\b|\bergometer\b|\bski ?erg\b|\bswim|\bwalk(ing)?\b|\bstairs?\b|\bhik(e|ing)\b"
)
REPS_CARDIO = r"\bburpees?\b|\bjumping jacks?\b|\bmountain climbers?\b|\bhigh knees\b|\bbox jumps?\b"
HOLD = r"\bplanks?\b|\bholds?\b|\bwall sits?\b|\bl sits?\b|\bdead hangs?\b|\bisometric\b|\bstatic\b|\bstretch(es|ing)?\b"

UNILATERAL = (
    r"\bone ?arm(ed)?\b|\bsingle ?arm\b|\bone ?leg(ged)?\b|\bsingle ?leg\b|\bunilateral\b|\balternat(e|ing)\b"
    r"|\bsplit squats?\b|\blunges?\b|\bstep ?ups?\b|\bpistols?\b|\bbulgarian\b|\bone hand(ed)?\b"
    r"|\bsingle hand(ed)?\b|\bsuitcase\b|\bkickstand\b"
)

COMPOUND = (
    r"\bpress(es)?\b|\bsquats?\b|\bdeadlifts?\b|\brows?\b|\bpull ?ups?\b|\bpullups?\b|\bchin ?ups?\b|\bchinups?\b"
    r"|\bdips?\b|\blunges?\b|\bcleans?\b|\bsnatch(es)?\b|\bjerks?\b|\bthrusts?\b|\bthrusters?\b|\bpull ?downs?\b"
    r"|\bpulldowns?\b|\bpush ?ups?\b|\bpushups?\b|\bstep ?ups?\b|\bgood ?mornings?\b|\bswings?\b|\bcarry\b"
    r"|\bfarmers?\b|\bburpees?\b|\bmuscle ?ups?\b"
)
ISOLATION = (
    r"\bcurls?\b|\braises?\b|\bextensions?\b|\bfl(y|ye|yes|ies)\b|\bkickbacks?\b|\bpush ?downs?\b|\bpushdowns?\b"
    r"|\bshrugs?\b|\bcrunch(es)?\b|\bcrossovers?\b|\bpullovers?\b"
)


def _matches(pattern: str, text: str) -> bool:
    return re.search(pattern, text) is not None


def _unique(values):
    seen = []
    for value in values:
        if value not in seen:
            seen.append(value)
    return seen


# Strength movements that are normally loaded; used when a source does not say which implement.
LOADED = (
    r"\bsquats?\b|\blunges?\b|\bdeadlifts?\b|\bpress(es)?\b|\brows?\b|\bcurls?\b|\braises?\b|\bextensions?\b"
    r"|\bfl(y|ye|yes|ies)\b|\bthrusts?\b|\bshrugs?\b|\bpullovers?\b|\bkickbacks?\b|\bswings?\b|\bcleans?\b"
    r"|\bsnatch(es)?\b"
)


def equipment_from_name(name: str) -> str | None:
    """The implement the exercise name states, if any."""
    text = re.sub(ACCESSORY, " ", search_normalize(name))
    for pattern, value in NAME_EQUIPMENT + EQUIPMENT_HINTS:
        if _matches(pattern, text):
            return value
    return None


def refine_equipment(name: str, equipment: str | None, category: str = "STRENGTH") -> str:
    named = equipment_from_name(name)
    if named:
        return named
    text = search_normalize(name)
    if equipment in (None, "OTHER") and _matches(WEIGHTED_BODYWEIGHT, text):
        return "BODYWEIGHT"
    if equipment is None and category == "STRENGTH" and _matches(LOADED, text):
        return "OTHER"  # loaded, implement unknown: log weight x reps rather than reps only
    return equipment or "BODYWEIGHT"


def split_delts(text: str) -> str:
    if _matches(REAR_DELT, text):
        return "REAR_DELTS"
    if _matches(SIDE_DELT, text):
        return "SIDE_DELTS"
    return "FRONT_DELTS"


def resolve_muscles(name: str, primary: list[str], secondary: list[str]) -> tuple[list[str], list[str]]:
    """Resolves placeholders (shoulders -> front/side/rear delts, abs -> obliques) and fixes gaps."""
    text = search_normalize(name)

    def resolve(muscle: str) -> str:
        if muscle == tx.DELTS:
            return split_delts(text)
        if muscle == "ABS" and _matches(OBLIQUE, text):
            return "OBLIQUES"
        return muscle

    resolved_primary = _unique(resolve(m) for m in primary)
    resolved_secondary = [m for m in _unique(resolve(m) for m in secondary) if m not in resolved_primary]
    if "OBLIQUES" in resolved_primary and "ABS" in primary and "ABS" not in resolved_secondary:
        resolved_secondary.append("ABS")
    involved = resolved_primary + resolved_secondary
    if "FRONT_DELTS" in resolved_primary and _matches(OVERHEAD_PRESS, text) and "SIDE_DELTS" not in involved:
        resolved_secondary.append("SIDE_DELTS")
    if not resolved_primary and resolved_secondary:
        resolved_primary = [resolved_secondary.pop(0)]
    return resolved_primary, resolved_secondary


def infer_muscles(name: str, wger_category: str | None = None) -> tuple[list[str], list[str]]:
    text = search_normalize(name)
    for pattern, primary, secondary in NAME_MUSCLES:
        if _matches(pattern, text):
            return list(primary), list(secondary)
    if wger_category in WGER_CATEGORY_MUSCLES:
        return [WGER_CATEGORY_MUSCLES[wger_category]], []
    return [], []


def infer_type(name: str, category: str, equipment: str, force: str | None = None) -> str:
    """Which fields the set row shows (SPEC §3 exercise types)."""
    text = search_normalize(name)
    if _matches(r"\bassist(ed)?\b", text):
        return "BODYWEIGHT_ASSISTED"
    if _matches(CARRY, text):
        return "WEIGHT_DISTANCE"
    if category == "CARDIO":
        if _matches(DISTANCE_CARDIO, text):
            return "DISTANCE_DURATION"
        if _matches(REPS_CARDIO, text):
            return "REPS_ONLY"
        return "DURATION"
    if category == "MOBILITY" or _matches(HOLD, text):
        return "DURATION"
    if force == "static" and equipment in ("BODYWEIGHT", "OTHER"):
        return "DURATION"
    if equipment == "BODYWEIGHT":
        return "BODYWEIGHT_ADDED" if _matches(WEIGHTED_BODYWEIGHT, text) else "REPS_ONLY"
    if equipment == "BAND" or (category == "PLYOMETRIC" and equipment == "OTHER"):
        return "REPS_ONLY"
    return "WEIGHT_REPS"


def infer_unilateral(name: str) -> bool:
    return _matches(UNILATERAL, search_normalize(name))


def infer_mechanic(name: str, given: str | None) -> str | None:
    if given in tx.MECHANICS:
        return given
    text = search_normalize(name)
    if _matches(COMPOUND, text):
        return "COMPOUND"
    if _matches(ISOLATION, text):
        return "ISOLATION"
    return None
