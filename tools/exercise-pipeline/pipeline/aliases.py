"""Aliases (shown to the user) and hidden search terms (abbreviations, spellings) per exercise (SPEC §3)."""

import re

from .normalize import clean_display_name, search_normalize

# (pattern over the search-normalized name, display aliases, search terms). Display aliases are only
# true synonyms of the same movement; implement-specific nicknames stay hidden search terms.
RULES = (
    (r"\bromanian deadlifts?\b", [], ["rdl"]),
    (r"\bstiff legg?e?d?\b.*\bdeadlifts?\b|\bstiff leg deadlifts?\b", ["Stiff-Leg Deadlift"], ["sldl", "sl dl"]),
    (r"\bbulgarian split squats?\b|\brear foot elevated split squats?\b", [], ["bss", "rfess"]),
    (r"\b(military|overhead) press\b|\bbarbell shoulder press\b", [], ["ohp", "overhead press", "military press"]),
    (r"\bshoulder press\b", [], ["ohp"]),
    (r"\bclose grip\b.*\bbench press\b", [], ["cgbp", "cg bench", "close grip bench"]),
    (r"\blat pull ?downs?\b|\bpull ?downs?\b|\bpulldowns?\b", [], ["pulldown", "lat pulldown", "pull down"]),
    (r"\bskull ?crushers?\b", ["Lying Triceps Extension"], ["skullcrusher", "skull crusher"]),
    (r"\blying triceps? (extensions?|press)\b", ["Skullcrusher"], ["skullcrusher", "skull crusher"]),
    (r"\bpull ?ups?\b|\bpullups?\b", [], ["pullup", "pull up"]),
    (r"\bchin ?ups?\b|\bchinups?\b", [], ["chinup", "chin up"]),
    (r"\bpush ?ups?\b|\bpushups?\b|\bpress ?ups?\b", [], ["pushup", "push up", "press up"]),
    (r"\bsit ?ups?\b|\bsitups?\b", [], ["situp", "sit up"]),
    (r"\bstep ?ups?\b", [], ["stepup", "step up"]),
    (r"\bmuscle ?ups?\b", [], ["muscleup", "muscle up"]),
    (r"\bgood ?mornings?\b", [], ["gm", "good morning"]),
    (r"\bdeadlifts?\b", [], ["dl", "deadlift"]),
    (r"\bglute ham raises?\b", [], ["ghr"]),
    (r"\bfarmers?\b.*\b(walk|carry)\b", ["Farmer's Carry"], ["farmers walk", "farmer walk", "farmers carry"]),
    (r"\bkettlebell swings?\b", [], ["kb swing"]),
    (r"\bleg curls?\b", [], ["hamstring curl", "ham curl"]),
    (r"\bleg extensions?\b", [], ["quad extension"]),
    (r"\btriceps? push ?downs?\b|\bpushdowns?\b", [], ["pushdown", "tricep pushdown", "triceps pushdown"]),
    (r"\bhip thrusts?\b", [], ["hip thruster"]),
    (r"\bt ?bar rows?\b", [], ["tbar row", "t bar row"]),
    (r"\bfl(y|ye|yes|ies)\b", [], ["fly", "flye", "flyes"]),
    (r"\bbent over (barbell )?rows?\b", [], ["bb row", "barbell row", "bent over row"]),
    (r"\bpec deck\b|\bbutterfly\b", [], ["pec deck", "machine fly", "butterfly"]),
    (r"\breverse fl(y|ye|yes|ies)\b|\brear delt fl", [], ["rear delt fly", "reverse fly"]),
    (r"\bshrugs?\b", [], ["shrug"]),
    (r"\bdips?\b", [], ["dip", "dips"]),
    (r"\bseated cable rows?\b|\bcable rows?\b", [], ["cable row", "seated row"]),
    (r"\bbarbell (full )?squats?\b|\bback squats?\b", [], ["back squat", "squat"]),
    (r"\bcalf raises?\b", [], ["calf raise"]),
    (r"\bdumbbell bench press\b", [], ["db bench", "db press"]),
    (r"\bbench press\b", [], ["bench", "bp"]),
    (r"\bhammer curls?\b", [], ["hammer curl"]),
    (r"\bface pulls?\b", [], ["face pull"]),
)

EQUIPMENT_TERMS = {
    "BARBELL": ["bb", "barbell"],
    "DUMBBELL": ["db", "dumbbell", "dumbbells"],
    "KETTLEBELL": ["kb", "kettlebell"],
    "MACHINE": ["machine"],
    "CABLE": ["cable"],
    "SMITH": ["smith", "smith machine"],
    "BODYWEIGHT": ["bodyweight", "bw"],
    "BAND": ["band", "banded"],
    "EZ_BAR": ["ez", "ez bar", "ezbar", "ez curl bar"],
    "TRAP_BAR": ["trap bar", "hex bar"],
    "PLATE": ["plate"],
    "OTHER": [],
}

MUSCLE_TERMS = {
    "CHEST": ["chest", "pecs"],
    "FRONT_DELTS": ["shoulders", "delts", "front delts"],
    "SIDE_DELTS": ["shoulders", "delts", "side delts"],
    "REAR_DELTS": ["shoulders", "delts", "rear delts"],
    "BICEPS": ["biceps", "bicep"],
    "TRICEPS": ["triceps", "tricep"],
    "FOREARMS": ["forearms", "grip"],
    "LATS": ["lats", "back"],
    "UPPER_BACK": ["upper back", "traps", "back"],
    "LOWER_BACK": ["lower back", "back"],
    "ABS": ["abs", "core"],
    "OBLIQUES": ["obliques", "core"],
    "GLUTES": ["glutes"],
    "QUADS": ["quads", "legs"],
    "HAMSTRINGS": ["hamstrings", "hams", "legs"],
    "ADDUCTORS": ["adductors", "inner thigh"],
    "ABDUCTORS": ["abductors", "outer thigh"],
    "CALVES": ["calves", "calf"],
    "NECK": ["neck"],
}


def generate(name: str, equipment: str, primary: list[str], extra_names: list[str]) -> tuple[list[str], str]:
    """Returns (display aliases, space-separated search terms) for one exercise."""
    text = search_normalize(name)
    candidates: list[str] = []
    terms: list[str] = []
    for pattern, alias_list, term_list in RULES:
        if re.search(pattern, text):
            candidates.extend(alias_list)
            terms.extend(term_list)
    candidates.extend(clean_display_name(n) for n in extra_names)
    terms.extend(EQUIPMENT_TERMS[equipment])
    for muscle in primary:
        terms.extend(MUSCLE_TERMS[muscle])

    seen = {text}
    aliases = []
    for alias in candidates:
        key = search_normalize(alias)
        if key and key not in seen:
            seen.add(key)
            aliases.append(alias)

    normalized_terms = sorted({t for t in (search_normalize(term) for term in terms) if t})
    return aliases, " ".join(normalized_terms)
