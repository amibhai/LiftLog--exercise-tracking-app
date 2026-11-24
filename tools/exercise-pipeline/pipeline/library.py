"""Merges both sources into one deduplicated library in the LiftLog taxonomy (SPEC §3)."""

from __future__ import annotations

import re
import uuid
from collections import Counter, defaultdict
from dataclasses import dataclass, field

from . import aliases as alias_rules
from . import classify
from . import taxonomy as tx
from .htmltext import html_to_steps
from .normalize import clean_display_name, dedupe_key, search_normalize
from .sources import fedb_image_url

LIBRARY_NAMESPACE = uuid.uuid5(uuid.NAMESPACE_URL, "liftlog:exercise-library")

EQUIPMENT_LABELS = {
    "BARBELL": "Barbell",
    "DUMBBELL": "Dumbbell",
    "KETTLEBELL": "Kettlebell",
    "MACHINE": "Machine",
    "CABLE": "Cable",
    "SMITH": "Smith Machine",
    "BODYWEIGHT": "Bodyweight",
    "BAND": "Band",
    "EZ_BAR": "EZ-Bar",
    "TRAP_BAR": "Trap Bar",
    "PLATE": "Plate",
    "OTHER": "Other",
}

PLACEHOLDER_NAME = re.compile(r"(?i)^(test|exercise|uebung|übung)\s*\d*$")

# wger's English field sometimes holds Spanish or German text.
NON_ENGLISH = re.compile(
    r"(?i)\b(con|mit|und|avec|sur|maquina|máquina|agarre|barra|polea|mancuerna|sentado|pierna|brazo|banco"
    r"|jal[oó]n|remo|pecho|muñeca)\b",
)


def stable_id(*parts: str) -> str:
    """Deterministic UUIDv5, so rebuilds and future library updates keep the same IDs."""
    return str(uuid.uuid5(LIBRARY_NAMESPACE, "/".join(parts)))


@dataclass
class Exercise:
    source_ref: str
    original_name: str
    name: str
    category: str
    equipment: str | None
    primary: list[str]
    secondary: list[str]
    mechanic: str | None
    instructions: list[str]
    images: list[dict]
    license: str
    license_url: str
    license_author: str | None
    force: str | None = None
    wger_category: str | None = None
    source_aliases: list[str] = field(default_factory=list)
    exercise_type: str = ""
    unilateral: bool = False
    aliases: list[str] = field(default_factory=list)
    search_terms: list[str] = field(default_factory=list)
    popularity_rank: int | None = None
    merged_from: list[str] = field(default_factory=list)
    merged_names: list[str] = field(default_factory=list)
    equipment_known: bool = True

    @property
    def source(self) -> str:
        return self.source_ref.split(":", 1)[0]

    @property
    def id(self) -> str:
        return stable_id("exercise", self.source_ref)

    def keys(self) -> list[str]:
        """Dedupe keys (normalized name + equipment) for the display and the source name."""
        return list(dict.fromkeys(dedupe_key(n, self.equipment) for n in (self.name, self.original_name)))

    def token_keys(self) -> list[str]:
        """Equipment-agnostic keys, used only when the source did not say which implement."""
        return list(dict.fromkeys(dedupe_key(n, "") for n in (self.name, self.original_name)))


@dataclass
class LibraryResult:
    exercises: list[Exercise]
    excluded: list[tuple[str, str, str]] = field(default_factory=list)
    duplicates: list[tuple[str, str, str, str]] = field(default_factory=list)
    renamed_for_collision: list[tuple[str, str, str]] = field(default_factory=list)
    inferred_muscles: list[tuple[str, str]] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)
    source_counts: Counter = field(default_factory=Counter)


def from_fedb(record: dict, fedb_config: dict) -> Exercise:
    license_name, license_url = tx.FEDB_LICENSE
    images = [
        {"url": fedb_image_url(fedb_config, path), "thumbnailUrl": None, "license": license_name, "author": None}
        for path in record.get("images") or []
    ]
    return Exercise(
        source_ref="fedb:" + record["id"],
        original_name=record["name"],
        name=clean_display_name(record["name"]),
        category=tx.FEDB_CATEGORIES[record["category"]],
        equipment=tx.FEDB_EQUIPMENT.get(record.get("equipment")),
        primary=[tx.FEDB_MUSCLES[m] for m in record.get("primaryMuscles") or []],
        secondary=[tx.FEDB_MUSCLES[m] for m in record.get("secondaryMuscles") or []],
        mechanic={"compound": "COMPOUND", "isolation": "ISOLATION"}.get(record.get("mechanic")),
        instructions=[" ".join(step.split()) for step in record.get("instructions") or [] if step.strip()],
        images=images,
        license=license_name,
        license_url=license_url,
        license_author=None,
        force=record.get("force"),
    )


def from_wger(record: dict) -> Exercise:
    license_name, license_url = tx.WGER_LICENSES[record.get("translationLicense") or record["license"]]
    implements = [tx.WGER_EQUIPMENT.get(e) for e in record["equipment"]]
    implements = [e for e in implements if e]
    equipment = min(implements, key=tx.EQUIPMENT_PRIORITY.index) if implements else None
    name = clean_display_name(record["name"])
    if record.get("categoryId") == tx.WGER_CARDIO_CATEGORY_ID:
        category = "CARDIO"
    elif re.search(r"\bstretch", search_normalize(name)):
        category = "MOBILITY"
    else:
        category = "STRENGTH"
    images = []
    for image in record["images"]:
        if image.get("aiGenerated"):
            continue
        image_license = tx.WGER_LICENSES.get(image.get("license"), (license_name, license_url))[0]
        images.append(
            {
                "url": image["url"],
                "thumbnailUrl": image.get("thumbnail"),
                "license": image_license,
                "author": image.get("licenseAuthor"),
            },
        )
    return Exercise(
        source_ref="wger:" + record["uuid"],
        original_name=record["name"],
        name=name,
        category=category,
        equipment=equipment,
        primary=[tx.WGER_MUSCLES[m] for m in record["muscles"] if m in tx.WGER_MUSCLES],
        secondary=[tx.WGER_MUSCLES[m] for m in record["musclesSecondary"] if m in tx.WGER_MUSCLES],
        mechanic=None,
        instructions=html_to_steps(record.get("description")),
        images=images,
        license=license_name,
        license_url=license_url,
        license_author=record.get("translationLicenseAuthor") or record.get("licenseAuthor"),
        wger_category=record.get("category"),
        source_aliases=list(record.get("aliases") or []),
    )


def wger_name_problem(name: str) -> str | None:
    stripped = name.strip()
    if len(stripped) < 3:
        return "name too short"
    if not re.search(r"[A-Za-z]", stripped):
        return "name has no letters"
    if PLACEHOLDER_NAME.match(stripped):
        return "placeholder name"
    if NON_ENGLISH.search(stripped):
        return "name is not English"
    return None


def classify_exercise(exercise: Exercise, fields: dict) -> bool:
    """Fills taxonomy fields in place; returns True when the muscles had to be inferred from the name.

    Classification reads the display name and the source name together, so a curated rename
    ("Reverse Flyes" -> "Reverse Dumbbell Fly") never loses information.
    """
    names = exercise.name if exercise.name == exercise.original_name else f"{exercise.name} {exercise.original_name}"
    named_equipment = classify.equipment_from_name(names)
    exercise.equipment_known = exercise.equipment is not None or named_equipment is not None or "equipment" in fields
    exercise.equipment = fields.get("equipment") or classify.refine_equipment(
        names,
        exercise.equipment,
        exercise.category,
    )
    primary, secondary = classify.resolve_muscles(names, exercise.primary, exercise.secondary)
    inferred = False
    if not primary:
        primary, extra_secondary = classify.infer_muscles(names, exercise.wger_category)
        secondary = [m for m in dict.fromkeys(secondary + extra_secondary) if m not in primary]
        inferred = True
    exercise.primary = fields.get("primary", primary)
    exercise.secondary = [m for m in fields.get("secondary", secondary) if m not in exercise.primary]
    exercise.category = fields.get("category", exercise.category)
    exercise.mechanic = fields.get("mechanic", classify.infer_mechanic(names, exercise.mechanic))
    exercise.exercise_type = fields.get("type") or classify.infer_type(
        names,
        exercise.category,
        exercise.equipment,
        exercise.force,
    )
    exercise.unilateral = fields.get("unilateral", classify.infer_unilateral(names))
    return inferred


def _merge_into(target: Exercise, duplicate: Exercise) -> None:
    """Keeps the target's data and licensing; only the duplicate's names become search aliases."""
    target.merged_from.append(duplicate.source_ref)
    target.merged_names.extend([duplicate.original_name, *duplicate.source_aliases])


def _richness(record: dict) -> tuple:
    return (-len(record.get("images") or []), -len(record.get("description") or ""), record["uuid"])


def build_library(
    fedb_records: list[dict],
    wger_records: list[dict],
    fedb_config: dict,
    overrides: dict,
    popular: dict,
    min_count: int,
) -> LibraryResult:
    result = LibraryResult(exercises=[])
    exclude = overrides.get("exclude", {})
    renames = overrides.get("rename", {})
    field_overrides = overrides.get("fields", {})
    merges = overrides.get("merge", {})

    candidates = [from_fedb(r, fedb_config) for r in sorted(fedb_records, key=lambda r: r["id"])]
    for record in sorted(wger_records, key=_richness):
        exercise = from_wger(record)
        problem = wger_name_problem(exercise.name)
        if problem:
            result.excluded.append((exercise.source_ref, exercise.name, problem))
        else:
            candidates.append(exercise)

    by_key: dict[str, Exercise] = {}
    by_tokens: dict[str, Exercise] = {}
    by_ref: dict[str, Exercise] = {}
    for exercise in candidates:
        result.source_counts[exercise.source + "_input"] += 1
        if exercise.source_ref in exclude:
            result.excluded.append((exercise.source_ref, exercise.name, exclude[exercise.source_ref]))
            continue
        if exercise.source_ref in renames:
            exercise.name = renames[exercise.source_ref]
        if classify_exercise(exercise, field_overrides.get(exercise.source_ref, {})):
            result.inferred_muscles.append((exercise.source_ref, exercise.name))

        target = by_ref.get(merges.get(exercise.source_ref, ""))
        if target is None:
            target = next((by_key[k] for k in exercise.keys() if k in by_key), None)
        if target is None and not exercise.equipment_known:
            # wger often omits equipment; then the same movement name is enough to call it a duplicate.
            target = next((by_tokens[k] for k in exercise.token_keys() if k in by_tokens), None)
        if target is not None:
            _merge_into(target, exercise)
            result.duplicates.append((exercise.source_ref, exercise.name, target.source_ref, target.name))
            continue

        for key in exercise.keys():
            by_key.setdefault(key, exercise)
        for key in exercise.token_keys():
            by_tokens.setdefault(key, exercise)
        by_ref[exercise.source_ref] = exercise
        result.exercises.append(exercise)

    _resolve_name_collisions(result)
    _apply_popularity(result, popular, by_ref)
    for exercise in result.exercises:
        extra_names = [exercise.original_name, *exercise.source_aliases, *exercise.merged_names]
        exercise.aliases, exercise.search_terms = alias_rules.generate(
            exercise.name,
            exercise.equipment,
            exercise.primary,
            extra_names,
        )
        result.source_counts[exercise.source] += 1

    known_refs = {c.source_ref for c in candidates}
    for section in ("exclude", "rename", "fields", "merge"):
        for ref in overrides.get(section, {}):
            if ref not in known_refs:
                result.errors.append(f"overrides.{section} refers to unknown exercise {ref}")
    result.errors.extend(validate(result.exercises, min_count))
    return result


def _resolve_name_collisions(result: LibraryResult) -> None:
    """Different exercises sharing a display name get their equipment appended (first one keeps it)."""
    groups: dict[str, list[Exercise]] = defaultdict(list)
    for exercise in result.exercises:
        groups[search_normalize(exercise.name)].append(exercise)
    for group in groups.values():
        for exercise in group[1:]:
            new_name = f"{exercise.name} ({EQUIPMENT_LABELS[exercise.equipment]})"
            result.renamed_for_collision.append((exercise.source_ref, exercise.name, new_name))
            exercise.merged_names.append(exercise.name)
            exercise.name = new_name


def _apply_popularity(result: LibraryResult, popular: dict, by_ref: dict[str, Exercise]) -> None:
    for rank, ref in enumerate(popular.get("ranked", []), start=1):
        if ref in by_ref:
            by_ref[ref].popularity_rank = rank
        else:
            result.errors.append(f"popular.json ranks {ref}, which is not in the library (excluded or merged?)")


def validate(exercises: list[Exercise], min_count: int) -> list[str]:
    errors = []
    if len(exercises) < min_count:
        errors.append(f"only {len(exercises)} exercises; at least {min_count} are required")
    for exercise in exercises:
        where = f"{exercise.source_ref} ({exercise.name})"
        if not exercise.name.strip():
            errors.append(f"{where}: empty name")
        if not exercise.primary:
            errors.append(f"{where}: no primary muscle")
        for muscle in exercise.primary + exercise.secondary:
            if muscle not in tx.MUSCLES:
                errors.append(f"{where}: unknown muscle {muscle}")
        if exercise.equipment not in tx.EQUIPMENT:
            errors.append(f"{where}: unknown equipment {exercise.equipment}")
        if exercise.category not in tx.CATEGORIES:
            errors.append(f"{where}: unknown category {exercise.category}")
        if exercise.exercise_type not in tx.EXERCISE_TYPES:
            errors.append(f"{where}: unknown type {exercise.exercise_type}")
        if exercise.mechanic is not None and exercise.mechanic not in tx.MECHANICS:
            errors.append(f"{where}: unknown mechanic {exercise.mechanic}")
    for counter, label in (
        (Counter(search_normalize(e.name) for e in exercises), "display name"),
        (Counter(e.id for e in exercises), "id"),
    ):
        errors.extend(f"duplicate {label}: {value}" for value, count in counter.items() if count > 1)
    return errors
