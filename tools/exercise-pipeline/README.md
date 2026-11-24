# Exercise library pipeline

This builds LiftLog's built-in library of 1,000+ exercises (SPEC §3). It runs on Python 3.12+ and uses only the standard library, so there is nothing to install.

## Run it

From the repository root:

```sh
python tools/exercise-pipeline/build_library.py                  # rebuild from the pinned sources
python tools/exercise-pipeline/build_library.py --refresh-wger   # download a fresh wger snapshot first
python -m unittest discover -s tools/exercise-pipeline/tests -t tools/exercise-pipeline
```

The script exits with an error if validation fails. Re-running it with the same inputs produces byte-identical outputs.

## Sources

| Source | Version used | License |
|---|---|---|
| [free-exercise-db](https://github.com/yuhonas/free-exercise-db) | Pinned commit in `data/sources.json`, downloaded once into `.cache/` (git-ignored) | Unlicense (public domain) |
| [wger](https://wger.de) API | `snapshots/wger-exerciseinfo.json`, a committed snapshot of English names, descriptions, aliases and images | Per exercise: CC BY-SA 3.0, CC BY-SA 4.0 or CC0 |

The live wger API changes over time, so the committed snapshot is what keeps builds reproducible. `--refresh-wger` rewrites it, and you can review the change in the git diff.

Licenses travel with the data. Each exercise keeps its license, license URL and author. Each image keeps its own license and author. The app's About screen lists both sources from the `library_source` table. When a wger record duplicates a free-exercise-db one, only its name is kept, as a search alias. The free-exercise-db text, images and license are what remain.

Images are never bundled in the APK; only URLs are stored. free-exercise-db image URLs point at the pinned commit, so they never change. AI-generated wger images are skipped.

## What it does

1. **Load** both sources. wger records are dropped if they are placeholders or not in English.
2. **Normalize** to the fixed taxonomy (`pipeline/taxonomy.py`), which must match the Kotlin enums in `core:domain`.
   - The exercise name refines the source data:
     - "Smith Machine Squat" → `SMITH`.
     - "shoulders" is split into front, side or rear delts.
     - "Russian Twist" → obliques.
   - The exercise type (which set fields the app shows) comes from the name, category and equipment (`pipeline/classify.py`).
3. **Dedupe** by normalized name + equipment (`pipeline/normalize.py`). Normalization:
   - lowercases, singularizes and sorts the words;
   - unifies spellings (pullup/pull-up, bicep/biceps, side lateral/lateral, lat pulldown/pulldown);
   - drops implement words and filler ("medium grip").

   If wger gives no equipment, the movement name alone decides. Free-exercise-db wins every merge. Near-duplicates that were not merged are listed in the report for review.
4. **Aliases and search terms** (`pipeline/aliases.py`):
   - Display aliases are source names and true synonyms (Skullcrusher ↔ Lying Triceps Extension).
   - Hidden search terms are abbreviations and spellings: db, bb, kb, ez, ohp, rdl, sldl, bss, cgbp, pulldown, pullup, plus equipment and muscle words. They are stored one phrase per line, because a search for a whole term ("ohp", "chest") ranks above a match on part of one.
5. **Curate** from `data/`:
   - `overrides.json`: renames, field overrides, exclusions and manual merges, keyed by `fedb:<id>` or `wger:<uuid>`. A key that refers to nothing is a validation error.
   - `popular.json`: ranked list. The first 12 entries are the starter set for a brand-new user; the rest break search ties.
6. **Write** the outputs.

## Outputs

| File | What |
|---|---|
| `core/database/src/main/assets/database/liftlog.db` | The prepopulated Room database (`createFromAsset`) |
| `out/exercises.json` | JSON copy of the library, sorted for readable diffs |
| `out/report.md` | Validation report: counts, merged duplicates, near-duplicates, exclusions, missing fields |

IDs are deterministic UUIDv5 values derived from the source reference. A rebuild therefore keeps every exercise's ID, and workout history keeps pointing at the right exercise.

## The database must match the Room schema

`pipeline/outputs.py` creates the database by replaying the exported Room schema (`core/database/schemas/…/<version>.json`) for the newest version:
- the create-table statements;
- the FTS4 content-sync triggers;
- the identity hash and `user_version`.

Rows missing a NOT NULL column fail loudly.

After any change to the Room entities:
1. Build `core:database` so KSP exports the new schema.
2. Re-run the pipeline.
3. Run `./gradlew :core:database:testDebugUnitTest`. `PrepackagedDatabaseTest` opens the asset exactly like first launch does, and Room's identity check fails if the asset is stale.

Never `VACUUM` the asset or the on-device database without rebuilding `exercise_fts` afterwards. A VACUUM can renumber the exercise rowids that the external-content FTS index is keyed on.
