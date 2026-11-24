"""Text normalization: search text, display-name cleanup and the dedupe key."""

import re
import unicodedata

SMALL_WORDS = {"a", "an", "and", "at", "by", "for", "in", "of", "on", "or", "the", "to", "vs", "with"}

# Multi-word spellings unified before tokenizing (applied to search-normalized text).
PHRASE_SYNONYMS = (
    (r"\bpull ?ups?\b", "pullup"),
    (r"\bchin ?ups?\b", "chinup"),
    (r"\bpush ?ups?\b|\bpress ?ups?\b", "pushup"),
    (r"\bsit ?ups?\b", "situp"),
    (r"\bstep ?ups?\b", "stepup"),
    (r"\bmuscle ?ups?\b", "muscleup"),
    (r"\blat pull ?downs?\b", "pulldown"),
    (r"\bpull ?downs?\b|\bpull downs?\b", "pulldown"),
    (r"\bside laterals?\b", "lateral"),
    (r"\bsitting\b", "seated"),
    (r"\bbicep\b", "biceps"),
    (r"\btricep\b", "triceps"),
    (r"\bpush ?downs?\b", "pushdown"),
    (r"\bskull ?crushers?\b", "skullcrusher"),
    (r"\bkick ?backs?\b", "kickback"),
    (r"\bbench ?press(es)?\b", "benchpress"),
    (r"\bdead ?lifts?\b", "deadlift"),
    (r"\bbent ?over\b", "bentover"),
    (r"\bt ?bar\b", "tbar"),
    (r"\bstiff ?legg?e?d?\b", "stiffleg"),
    (r"\bsingle ?arm\b|\bone ?arm\b|\bone ?handed\b|\bsingle ?handed\b", "onearm"),
    (r"\bsingle ?leg\b|\bone ?leg(ged)?\b", "oneleg"),
    (r"\bclose ?grip\b|\bnarrow ?grip\b", "closegrip"),
    (r"\bwide ?grip\b", "widegrip"),
    (r"\bmilitary press\b", "overhead press"),
    (r"\bsz ?bar\b|\bez ?curl ?bar\b|\bez ?bar\b|\be ?z ?bar\b|\be ?z curl bar\b", "ezbar"),
    (r"\bhex ?bar\b|\btrap ?bar\b", "trapbar"),
    (r"\bsmith ?machine\b", "smith"),
)

# Phrases that do not change which exercise it is ("standard" bench press == bench press).
NOISE_PHRASES = (r"\bmedium grip\b", r"\bstandard\b", r"\bregular\b", r"\bbasic\b", r"\bclassic\b", r"\bnormal\b")

# Implement words are removed from the name because equipment is its own part of the key. Bands and
# chains are deliberately kept: "Squat with Bands" is a different exercise from a plain squat.
EQUIPMENT_WORDS = {
    "barbell", "barbells", "bb", "dumbbell", "dumbbells", "db", "dbs", "kettlebell", "kettlebells",
    "kb", "cable", "cables", "machine", "lever", "leverage", "smith", "ezbar", "trapbar", "plate",
    "bodyweight", "weighted", "with", "using",
}
FILLER_WORDS = {"a", "an", "and", "the", "on", "of", "to", "for", "in", "at", "exercise", "movement", "version"}

SINGULAR_EXCEPTIONS = {
    "press", "abs", "triceps", "biceps", "lats", "glutes", "quads", "hamstrings", "delts",
    "traps", "pecs", "obliques", "adductors", "abductors", "calves", "forearms", "cross", "pass",
    "chess", "ss", "stress", "jacks", "gorilla",
}
IRREGULAR_PLURALS = {"flies": "fly", "flyes": "fly", "flye": "fly", "calves": "calf", "lunges": "lunge"}


def ascii_fold(text: str) -> str:
    return unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode("ascii")


def search_normalize(text: str) -> str:
    """Lowercase ASCII words separated by single spaces. Mirrors the app's query normalizer."""
    text = ascii_fold(text).lower()
    text = re.sub(r"[^a-z0-9]+", " ", text)
    return " ".join(text.split())


def singular(token: str) -> str:
    if token in IRREGULAR_PLURALS:
        return IRREGULAR_PLURALS[token]
    if token in SINGULAR_EXCEPTIONS or len(token) <= 3:
        return token
    if token.endswith("ies") and len(token) > 4:
        return token[:-3] + "y"
    if token.endswith("ches") or token.endswith("shes") or token.endswith("sses"):
        return token[:-2]
    if token.endswith("s") and not token.endswith("ss") and not token.endswith("us"):
        return token[:-1]
    return token


def canonical_tokens(name: str) -> list[str]:
    """Tokens that identify the movement, independent of implement, plurals and spelling."""
    text = search_normalize(name)
    for pattern, replacement in PHRASE_SYNONYMS:
        text = re.sub(pattern, replacement, text)
    for pattern in NOISE_PHRASES:
        text = re.sub(pattern, " ", text)
    tokens = [singular(t) for t in text.split()]
    return [t for t in tokens if t not in EQUIPMENT_WORDS and t not in FILLER_WORDS]


def dedupe_key(name: str, equipment: str) -> str:
    """Normalized name + equipment (SPEC §3). Word order does not matter."""
    return " ".join(sorted(set(canonical_tokens(name)))) + "|" + equipment


def _capitalize_part(part: str) -> str:
    """Uppercases the first letter even behind punctuation: "(bar" -> "(Bar"."""
    for i, char in enumerate(part):
        if char.isalpha():
            return part[:i] + char.upper() + part[i + 1 :]
    return part


def _title_word(word: str, first: bool) -> str:
    if not word:
        return word
    lower = word.lower()
    if not first and lower in SMALL_WORDS:
        return lower
    if word.isupper() and len(word) <= 4:
        return word  # acronyms: EZ, DB, TRX, RDL
    if re.fullmatch(r"\d+[a-z]*", lower):
        return lower  # "45-degree", "2x"
    return "-".join(_capitalize_part(p) for p in word.split("-"))


def clean_display_name(name: str) -> str:
    """Light cleanup: whitespace, user-variant markers, common misspellings, consistent title case."""
    name = " ".join(name.replace("’", "'").split())
    name = re.sub(r"\s*\((custom|copy)\)\s*$", "", name, flags=re.IGNORECASE)
    name = name.replace(" | ", " - ")
    name = re.sub(r"\bdumbell", "dumbbell", name, flags=re.IGNORECASE)
    name = re.sub(r"\bsz[- ]?bar\b", "EZ-Bar", name, flags=re.IGNORECASE)
    name = re.sub(r"\bez[- ]bar\b", "EZ-Bar", name, flags=re.IGNORECASE)
    words = name.split(" ")
    out = []
    first = True
    for word in words:
        out.append(_title_word(word, first))
        first = word in ("-", "/", "(", "–")
    return " ".join(out).strip()
