# Build "LiftLog" — a state-of-the-art Android strength-training tracker

You are building a personal, offline-first Android app for logging gym workouts (exercises, sets, reps, weight). It is for one user (me), so there are no accounts, ads, paywalls, social features or nutrition. The bar is: **faster and more pleasant to log with than Strong or Hevy**, with better progress insight.

Before writing code:
1. Propose the Phase 0 plan and wait for my approval. Work **one phase at a time**; at the end of each phase, run the tests and a build, give me a short summary plus how to try it on my phone, then stop for review.
2. Where an Android/Jetpack API detail below might be outdated, check the current official documentation and use the current recommended approach — tell me when you deviate from this spec and why.

---

## 1. Platform & tech stack

- **Android only.** Native **Kotlin + Jetpack Compose**, single-activity.
- minSdk 26, target/compile the latest stable SDK. Gradle Kotlin DSL + version catalog (`libs.versions.toml`).
- **UI:** Material 3 (dynamic color on Android 12+, a polished custom fallback palette, full dark mode — dark is the default). Use Material 3 Expressive components/motion if stable in the current Compose Material3 release.
- **Architecture:** MVVM + unidirectional data flow, `StateFlow` UI state, Kotlin coroutines/Flow. Modules (or at minimum packages): `core:database`, `core:domain` (pure Kotlin, no Android deps), `core:ui` (design system), `feature:*` (workout, exercises, routines, history, progress, settings).
- **DI:** Hilt. **Navigation:** Navigation Compose with type-safe routes.
- **Database:** Room (SQLite) with exported schemas, versioned migrations, and an **FTS4 table for exercise search**. Ship the exercise library as a **prepopulated database asset** (`createFromAsset`) so first launch is instant.
- **Settings:** Proto or Preferences DataStore.
- **Charts:** Vico (Compose). **Images:** Coil 3 with disk cache.
- **Background work:** WorkManager (backups). **Widgets:** Jetpack Glance.
- **Testing:** JUnit5/JUnit4 + Turbine + Truth for domain/ViewModels, Room in-memory tests, Compose UI tests for the logging flow. Add a **Baseline Profile** module for startup and scroll performance.
- **Quality:** ktlint/detekt, no compiler warnings, R8 enabled for release, Compose compiler stability reports clean in hot paths (set rows must not recompose unnecessarily).

## 2. Units

- **Default unit: kg.** Settings toggle for **lb**.
- **Store all weights canonically in kg** (Double) in the DB; convert only at the UI layer. Entering 45 lb must display back as exactly 45 lb (round on display to the active unit's increment, never drift).
- Per-exercise weight step (default 2.5 kg / 5 lb; dumbbells 2 kg / 5 lb; configurable). Bar weight per exercise (default 20 kg / 45 lb).
- Plate inventory in settings (default kg plates: 25, 20, 15, 10, 5, 2.5, 1.25; lb: 45, 35, 25, 10, 5, 2.5).

## 3. Exercise library (1,000+ exercises)

- Build a reproducible data pipeline in `/tools` (a Kotlin script or Python, your choice) that:
  1. Downloads **free-exercise-db** (github.com/yuhonas/free-exercise-db, public domain/Unlicense, ~870 exercises with images) as the base.
  2. Merges in exercises from the **wger** public API (wger.de) that are not duplicates — respect per-exercise licenses, keep license + author fields, and list attributions on an About screen.
  3. Normalizes everything to **one fixed taxonomy**: muscles (chest, front delts, side delts, rear delts, biceps, triceps, forearms, lats, upper back/traps, lower back, abs, obliques, glutes, quads, hamstrings, adductors, abductors, calves, neck), equipment (barbell, dumbbell, kettlebell, machine, cable, smith, bodyweight, band, EZ bar, trap bar, plate, other), category, exercise type.
  4. **Dedupes** by normalized name + equipment, and generates **aliases/abbreviations** (e.g. "db" = dumbbell, "bb" = barbell, "kb" = kettlebell, "ohp" = overhead press, "rdl" = romanian deadlift, "bss" = bulgarian split squat, "lat pulldown"/"pulldown", "skullcrusher"/"lying triceps extension").
  5. Outputs the prepopulated Room DB asset + a JSON copy, with a validation report (counts, duplicates removed, missing fields).
- Images: do **not** bundle all images in the APK. Store image URLs; load lazily via Coil with disk caching; add a Settings option "Download all exercise images for offline use".
- **Exercise types** (determine which fields a set row shows): weight × reps, bodyweight + added weight, assisted bodyweight (weight subtracted), reps only, duration, distance + duration, weight + distance (carries).
- Each exercise: name, aliases, type, primary/secondary muscles, equipment, instructions, images, unilateral flag, default rest, weight step, bar weight, notes, favorite flag, is_custom, archived flag.
- **Custom exercises without limits**; edit/archive any exercise (built-in ones can be hidden, not deleted).

## 4. Exercise picker (very important)

When adding an exercise to a workout or routine, **never dump the full library as one long list**.

Screen layout:
- **Search bar at the top** (tap to type; keyboard opens on tap, not on screen open).
- Below it, when the query is empty, show **suggestions only**:
  1. **"Frequently done"** — ranked by *frecency* (usage count weighted by recency, e.g. exponential decay with a ~30-day half-life). Top ~8.
  2. **"Goes with this workout"** — exercises that historically co-occur with the exercises already in the current workout / same routine, excluding ones already added.
  3. **"Recent"** — last ~5 distinct exercises.
  4. **Favorites** (if any).
  5. Compact **muscle-group chips** and an **equipment filter** to browse; selecting a chip shows only that filtered set (sorted by frecency, then name).
- For a brand-new user with no history, show a curated starter set of ~12 popular compound lifts + the muscle chips.
- **Search behavior:**
  - Instant results as I type (debounced ~100 ms), using FTS prefix matching over names + aliases.
  - Typo tolerance: if FTS returns few results, fall back to in-memory fuzzy matching (e.g. Damerau-Levenshtein / trigram) over names and aliases.
  - Ranking: exact match > prefix match > word-prefix match > fuzzy; within ties, boost by my frecency.
  - Matched text highlighted.
- Each result row: name, equipment, primary muscle, small thumbnail, and **"Last: 80 kg × 8 · 3 days ago"** if I've done it.
- **Multi-select**: tap several exercises, then "Add (3)". Option to add selected as a superset.
- Empty results → "Create '<query>' as custom exercise" prefilled with the query.
- Long-press a result → exercise detail (instructions, history, charts).

## 5. Logging a workout (the core — optimize every tap)

Target: **logging a set that matches the plan = 1 tap.**

- **Start options** on Home: "Repeat last <routine name>", start a routine, start empty workout. A suggested "next workout" if a program/rotation is active.
- **Set rows are pre-filled**:
  - "Previous" column shows what I did last time for that set index (from the most recent completed workout containing that exercise).
  - Weight/reps inputs are pre-filled with the **progression suggestion** (see §7), or previous values if no rule applies, shown as accepted ghost values. A small badge like "+2.5 kg" explains the suggestion.
- **Tap ✓ to complete a set** → haptic tick, row animates to completed, **rest timer starts automatically** (per-exercise default rest, overridable per routine exercise).
- **Fast editing:**
  - Tapping weight/reps opens a **custom in-app number pad** (bottom sheet, not the system keyboard) with: digits, decimal, backspace, **−/+ step buttons** (using the exercise's weight step), "Same as previous", and "Next" to jump to the next field. Big keys, one-handed reachable.
  - Swipe right on a set → duplicate it below (auto-filled). Swipe left → delete with undo snackbar.
  - "+ Add set" copies the last set.
  - Long-press a set → set type: **Warm-up (W), Working, Drop (D), Failure (F)**; working sets are numbered, warm-ups excluded from PRs/volume.
- **Effort**: optional inline **RPE or RIR** (setting chooses which) as a one-tap chip row that appears right after completing a set; never a blocking dialog.
- **Supersets/circuits**: group exercises; after completing a set in a superset, focus jumps to the next exercise in the group and rest starts only after the round.
- Reorder exercises with drag handles; replace an exercise (keeps sets); per-exercise and per-workout notes; exercise notes "sticky" (shown next time).
- **Plate calculator** (tap the weight → see plates per side using my inventory and bar weight) and **warm-up generator** (e.g. bar×10, 40%×5, 60%×3, 80%×1, rounded to available plates; configurable).
- Workout header: elapsed time, total volume, sets done. Screen stays on during a workout (setting).
- **Finish workout** → summary screen: duration, volume, sets per muscle, PRs hit (with celebration animation), muscle heatmap. Option to save as/update a routine.
- **Crash-proof**: the in-progress workout is persisted to the DB on every change; survives process death, reboot and app updates; "Resume workout" banner. Discarding requires confirmation.
- Editing past workouts must be fully supported (including dates/times).

## 6. Rest timer

- Timer state is based on **absolute end timestamps** (never a ticking counter), so it is correct after backgrounding/process death.
- In-app: prominent but non-blocking timer bar with −15s / +15s / skip.
- When the app is backgrounded during a workout: an **ongoing notification** showing the countdown (use the notification chronometer countdown) with +15s/skip actions; on Android 16+, use **Live Updates / promoted ongoing notifications** if available so it shows as a status-bar chip. Verify the current API and foreground-service-type requirements and pick the most correct approach.
- At the end of rest: vibration pattern + optional sound, even if the screen is off. Request only the permissions truly needed (POST_NOTIFICATIONS; handle exact-alarm restrictions gracefully with a fallback).

## 7. Progression & smart features

- Per routine exercise: target sets, **rep range** (e.g. 8–12), optional target RPE/RIR, progression rule.
- Progression rules (pure Kotlin in `core:domain`, fully unit-tested):
  - **Double progression** (default): when all working sets hit the top of the rep range → suggest +1 weight step and reset to the bottom of the range; otherwise suggest same weight, +1 rep.
  - **Linear**: +X per successful session; after N consecutive failures → deload by Y% (configurable).
  - **None** (just repeat previous).
- **Plateau detection**: e1RM flat or declining across ~3–4 sessions → gentle insight card ("Bench has stalled for 3 weeks — consider a deload or rep-range change").
- **Built-in program templates** (as editable routines with a rotation/schedule): Push/Pull/Legs, Upper/Lower, Full Body 3×, 5×5, GZCLP, 5/3/1 (with training-max math). A program has an ordered rotation; Home suggests the next day.

## 8. Progress & analytics

- **Estimated 1RM**: Epley (and Brzycki as an option), only from working sets with reps ≤ 12; best e1RM per session.
- **Automatic PR detection** (excluding warm-ups): heaviest weight, best e1RM, most reps at a given weight, best set volume, best session volume. PR badges in-workout and on the summary.
- **Exercise detail screen**: charts (e1RM, top set weight, session volume, reps — selectable ranges 1M/3M/6M/1Y/All), full history list, PR table, rep-max table (best weight for 1–12 reps).
- **Weekly sets per muscle group** (primary = 1 set, secondary = 0.5 set, configurable) vs. a target range (default 10–20/week); a **front/back body heatmap** drawn as an original vector (do not copy proprietary artwork).
- Dashboard: calendar heatmap of workouts, weekly streak, workouts per week, total volume trend, time trained.
- **Body metrics**: body weight (with trend line/moving average), measurements (waist, chest, arms, thighs, etc.), progress photos stored privately in app storage.

## 9. Data ownership

- **Android Auto Backup** enabled with explicit backup rules (DB + settings), so data survives reinstall/new phone.
- **Automatic local backups**: WorkManager job (daily/weekly, configurable) writing a JSON backup into a folder I choose once via the Storage Access Framework (persisted URI) — e.g. a Google Drive-synced folder. Keep the last N backups.
- Manual **export** (JSON full backup, CSV of all sets) and **restore** from JSON.
- **Import from Strong and Hevy CSV exports**, with exercise-name mapping (auto-match via search + manual fix-up screen for unmatched names).
- Every row has a UUID primary key, `createdAt`, `updatedAt`, and soft-delete `deletedAt` so cloud sync can be added later without migration pain.

## 10. Data model (starting point — refine as needed)

- `exercise`, `exercise_fts` (name + aliases), `exercise_muscle` (exercise_id, muscle, is_primary)
- `routine`, `routine_exercise` (order, superset_group, target_sets, rep_min, rep_max, target_effort, rest_seconds, progression_rule, rule_params)
- `program`, `program_day` (rotation order → routine)
- `workout` (started_at, ended_at, routine_id, program_day_id, notes, is_in_progress)
- `workout_exercise` (workout_id, exercise_id, order, superset_group, notes)
- `workout_set` (workout_exercise_id, index, type, weight_kg, reps, duration_s, distance_m, rpe, rir, completed_at, is_completed)
- `personal_record` (derived cache, rebuildable from history)
- `body_metric` (date, type, value), `progress_photo`
- Indexes for: last performance by exercise, sets by date range, frecency computation. Keep "previous set" and frecency queries well under 16 ms.

## 11. Home screen & widgets

- Home: resume banner, suggested next workout, quick start, this week's stats (workouts, sets, volume), recent PRs, insight cards.
- **Glance widget**: "Start/Resume workout" + weekly streak.
- App shortcuts (long-press icon): Start empty workout, Repeat last workout.

## 12. Settings

Units (kg/lb), weight steps, plate inventory, bar weights, default rest time, auto-start rest timer, timer sound/vibration, RPE vs RIR vs off, warm-up generator percentages, e1RM formula, weekly set targets, first day of week, keep screen on, theme (system/dark/light/dynamic color), backups, import/export, download images offline, about/attributions.

## 13. UX & quality bar

- Feels instant: cold start < 1 s on a mid-range phone, 0 dropped frames while scrolling a workout with 10 exercises × 5 sets.
- Large touch targets (≥ 48 dp, primary actions ≥ 56 dp), one-handed reachability, bottom-anchored primary actions, edge-to-edge with correct insets, predictive back gesture.
- Meaningful motion (set completion, PR celebration) but never slowing input.
- Haptics on complete/PR/timer end (respect system settings).
- Accessibility: TalkBack labels on every control, sufficient contrast, font scaling up to 200% without breaking layouts.
- Empty states and first-run onboarding (unit choice, plate setup, optionally pick a program) — max 3 screens, skippable.

## 14. Phases

- **Phase 0 — Foundation:** project setup, modules, design system (colors, typography, components: set row, number pad, timer bar), Room schema + migrations, exercise data pipeline + prepopulated DB, navigation shell, CI-style Gradle tasks (`./gradlew test lint detekt assembleDebug`).
- **Phase 1 — Logging core (MVP):** exercise picker (§4 in full), workout logging (§5), rest timer (§6), routines CRUD, history list + workout detail/edit, settings for units/steps/rest, crash-proof in-progress workouts.
- **Phase 2 — Progress:** e1RM, PR detection, exercise detail charts, weekly muscle sets + body heatmap, calendar/streaks, body metrics, finish-workout summary.
- **Phase 3 — Smart & data:** progression rules + suggestions, plate calculator, warm-up generator, program templates, plateau insights, backups/export/import (incl. Strong/Hevy CSV).
- **Phase 4 — Polish:** Glance widget, app shortcuts, Live Updates timer, onboarding, baseline profiles, performance + accessibility pass, Health Connect export of workout sessions (optional).

## 15. Working rules

- Domain logic (progression, e1RM, PRs, plate math, warm-ups, frecency, unit conversion, CSV parsing) lives in `core:domain` with thorough unit tests — write tests first for these.
- Commit after each meaningful step with clear messages. Never leave the build broken.
- Ask me before adding any new third-party dependency not listed here, and before any schema change after Phase 1 (always with a migration).
- After each phase, tell me exactly how to install it on my phone (`./gradlew installDebug` over USB debugging) and what to test in a real gym session.
