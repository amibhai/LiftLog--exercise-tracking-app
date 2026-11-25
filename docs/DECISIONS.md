# Decisions and deviations

`docs/SPEC.md` is the source of truth. This file records where the implementation deliberately differs from it, or settles something the spec leaves open, and why. Add an entry whenever you deviate.

## Toolchain and libraries (Phase 0, 2026-11-23)

Each entry was checked against current official documentation on 2026-11-23.

1. **Room 3 (`androidx.room3`) with `BundledSQLiteDriver`.** The spec says "Room". Room 2.x is in maintenance mode, and Room 3 still has FTS4, `createFromAsset`, schema export and migrations. Room 3 requires DAO functions to be `suspend` or return `Flow`. The bundled driver ships the same modern SQLite on every Android version, which brings window functions and math functions. It adds roughly 1 MB per ABI.
2. **JSON DataStore (kotlinx-serialization).** The spec says Proto or Preferences DataStore. JSON is one of the three flavours in the official DataStore guide. It is typed like Proto but needs no protobuf toolchain. Lists such as plate inventories and warm-up percentages fit naturally.
3. **compileSdk 37.1, targetSdk 37.** The spec says "latest stable SDK". Compose UI 1.13's AAR metadata requires compileSdk 37 with minor version 1. Android 17 (API 37) has been stable since June 2026.
4. **Material 3 Expressive is in use.** The spec says to use it if stable, and it is stable in material3 1.5.0. We avoid the APIs that are still experimental there: `LoadingIndicator`, `MaterialShapes` and `AppBarWithSearch`.
5. **detekt 2.0.0-alpha.6, plus the ktlint wrapper and compose-rules.** The spec says "ktlint/detekt". detekt 2 is the only line that supports Kotlin 2.4 and AGP 9. A single `detekt` task covers detekt rules, ktlint formatting and Compose rules.
6. **Test frameworks.** The spec says JUnit5/JUnit4. JUnit 6, the current Jupiter line, is used in pure-JVM modules. JUnit 4 is used in Android modules, because Robolectric and the Compose test rule are JUnit 4 based.
7. **Icons are Material Symbols vector drawables.** `material-icons-extended` is frozen at 1.7.8 and no longer recommended.
8. **AGP 9 built-in Kotlin.** AGP 9 rejects `org.jetbrains.kotlin.android`. Kotlin compiler options are set on the Kotlin compile tasks instead.
9. **Extra modules: `feature:home`, `core:data` and `core:testing`.**
   - Home aggregates several areas.
   - Repository implementations and DataStore live in `core:data`, so features see only `core:domain` interfaces.
   - Shared test utilities live in `core:testing`.
10. **Navigation Compose 2.10 with type-safe routes, as specified.** The official navigation overview still says to use Navigation Compose for Compose apps. Navigation 3 (1.2) was considered.
11. **ktlint uses the `intellij_idea` code style** (the official Kotlin coding conventions): 120 columns and trailing commas. The `android_studio` style was tried first. Its wrapping rules assume 100 columns regardless of the configured line length.
12. **New dependencies approved by the user on 2026-11-23: Robolectric and compose-rules.** Both are test-time or build-time only and never ship in the APK.

## Product and data decisions

- **Weight display precision.** The spec says to "round on display to the active unit's increment". We round to a fixed 0.01 of the active unit, never to the plate step, so every typed value displays back exactly. The per-exercise step drives only the −/+ buttons and plate math.
- **One applicationId for debug and release.** Each phase's `installDebug` upgrades in place and keeps logged data. Release builds are signed with your key from `keystore.properties` if that file exists, otherwise with the debug key, so the two builds can replace each other.
- **Schema changes.** We never use destructive migration fallback. Once a build is installed, every schema change gets a version bump, a migration and a migration test.
- **Search ranking.** SPEC §4 ranks exact > prefix > word-prefix > fuzzy. An alias or generated search term that equals the whole query ("bench press", "ohp", "chest") counts as exact, just below an exercise with that exact name, and a prefix of an alias counts like a prefix of the name. A well-known lift, or one that was done before, counts as a prefix match wherever its name contains the word: "curl" finds Barbell Curl before "Curl with Shoulder Elevated". Equal matches are ordered by frecency, then popularity, then name. Ranking and typo tolerance are in `core:domain` (`SearchIndex`); SQL only finds the full-text matches.

## Phase 1 (2026-11-24)

The user asked to go on with Phase 1 and left open decisions to us ("do whatever you feel would be best").

1. **Coil's OkHttp network module** (`coil-network-okhttp`, which brings in OkHttp). Coil 3 needs a network module, and the spec does not name one. Coil picks it up by itself.
2. **Schema version 2** adds `target_duration_s` and `target_distance_m` to `workout_set`, so that timed and distance sets have ghost values like weights and reps do. Version 1 installs are migrated.
3. **Edits are pure functions.** Every change to a workout or routine is a pure function in `core:domain`. The repository applies it to the stored row tree in one transaction and writes only the rows that changed.
4. **Previous sets.** Warm-ups are matched with warm-ups and working sets with working sets, so an extra warm-up does not shift the "Previous" column.
5. **Carried-over weight.** When a set is done with a different weight than planned, the following sets that expected the planned weight follow. Reps never carry over.
6. **Finishing opens the workout's detail page**, which is also the finish summary of §5: one page instead of two. Phase 1 gave it the "Workout complete" header and the "Save as routine" / "Update routine" choice; Phase 2 added the records, the sets per muscle and the body heatmap. Finishing drops sets that were not done; a workout with nothing done can only be discarded.
7. **Rest timer without a foreground service** (SPEC §6, checked against the Android 16/17 docs on 2026-11-24). The timer is stored as an end time. An exact alarm (`USE_EXACT_ALARM`, granted at install for timer apps from Android 13; `SCHEDULE_EXACT_ALARM` up to Android 12, with an inexact fallback when it is off) wakes the app when the rest ends. While the app is in the background, an ongoing notification shows a chronometer countdown with +15s and Skip, and asks to be promoted (`setRequestPromotedOngoing`, `POST_PROMOTED_NOTIFICATIONS`). On Android 16+ that makes it a Live Update in the status bar. Android 17 mutes sounds that background apps play themselves, so the end-of-rest alert is a high-importance notification. Sound and vibration belong to its channel, so there is one channel per combination of the two settings. With the app on screen, it vibrates and plays the sound itself instead. `POST_NOTIFICATIONS` is asked for on the workout screen. Note: Google Play limits `USE_EXACT_ALARM` to alarm and timer apps; LiftLog is sideloaded.
8. **Routine targets from presets.** Reps per set are picked from common targets (8, 8–12, …) rather than typed. Sets use a stepper; rest and RPE/RIR targets use the same one-tap dialogs as the workout screen.
9. **Deleting a workout asks first.** Workouts are deleted from their detail page after a confirmation, not with an undo snackbar. The page closes at once, so there would be nowhere to show the snackbar. Sets, exercises and routines keep the undo snackbar.
10. **One workout at a time.** Starting a workout while one is in progress offers to resume it instead of starting a second one.
11. **A new routine is created before its editor opens**, so that the exercise picker can add to it. If it is left without a name and without exercises, it is deleted again.

## Phase 2 (2026-11-24)

The user asked for Phases 2 to 4 in one go, without Health Connect (SPEC §14 lists it as optional).

1. **A record beats an earlier value.** The first session of an exercise is the baseline, not a list of five records. The records table still shows every best, including those of a first session.
2. **Records are a cache.** `personal_record` is worked out from the finished sets, per exercise, in the transaction that finishes, edits, deletes or restores a workout. It is rebuilt as a whole when it is empty or the 1RM formula changes. Nothing else is stored, so it can always be rebuilt (SPEC §10).
3. **Record types follow the exercise type.** SPEC §8 lists weight-based records. Exercises without a weight get most reps, longest time or longest distance instead, and assisted exercises get least assistance.
4. **e1RM: sets of 1 to 12 reps**, as in §8. A bodyweight exercise with added weight has no e1RM, because the body weight at the time is not part of the set.
5. **Vico 3.3.1** for charts, as named in the spec. Charts use Vico's line and column builders with the app's colors; TalkBack reads one sentence per chart.
6. **The body heatmap is drawn from our own outlines**, written as simple vector paths in code. A muscle is shaded by its weekly sets compared with the lower end of the target range.
7. **The calendar heatmap shows 17 weeks**, one column per week, and shades a day by its sets.
8. **A week counts for the streak with one workout.** The current week never breaks a streak; it can only extend it.
9. **Body measurements are stored in canonical units** (kg, cm, percent) and shown in the unit system of the weights, like everything else.
10. **Progress photos stay in the app's private storage**, out of the gallery and out of cloud backups (Android limits those to 25 MB). They do move along when you set up a new phone directly from the old one. The camera app writes to a `FileProvider` URI, so no camera or storage permission is needed. A deleted photo's file is removed after 7 days, so the undo works.
11. **The body page can be opened before the first workout**, from the Progress tab.
12. **Celebration.** A record gets a trophy on its set badge and a haptic while logging. The confetti plays once, on the finish summary, and not at all when animations are turned off.

## Phase 3 (2026-11-24)

1. **Progression rules: double progression, linear, stages (GZCLP), 5/3/1, none.** SPEC §7 names the first two and "none"; the GZCLP and 5/3/1 templates need the other two. They are pure functions of the history. Nothing but the rule and its settings is stored (`routine_exercise.progression_rule` and `rule_params`), so there is no schema change and no state that could drift.
2. **What a suggestion builds on.** The sessions of the same routine slot, of other slots with the same plan (rep range and rule), and of workouts without a routine whose reps fit the range. So the squat of both days of a 5×5 moves on together, and a heavy day does not push a light one. The "Previous" column still shows the last time the exercise was done anywhere.
3. **Linear: a session is missed** when a set is missing, a rep is missing or weight was taken off along the way. Weights that only go up are a ramp and keep their shape. Deloads are whole weight steps, at least one, and never below the bar.
4. **5/3/1: every fourth session is the deload week.** Sessions count from the moment the training max was set. After each round the training max grows by 2.5 kg / 5 lb (twice that for squat and deadlift in the template).
5. **New routine exercises start with 8–12 reps**, because double progression needs a range. Any range, or none, can be chosen.
6. **Badges and hints.** A badge marks a ghost value that is higher than last time ("+2.5 kg", "+1"). A line under the exercise explains what the numbers do not say: more weight, a retry, a deload, a stage or a 5/3/1 week.
7. **Plates.** Loaded heaviest first; when that does not add up with the plates there are, any combination that does. A weight that cannot be loaded shows the closest one below it. Smith machines count with the barbell's weight, EZ bars with 10 kg / 25 lb, trap bars with 25 kg / 55 lb. Every exercise can have its own bar weight.
8. **Warm-ups** are rounded to what can be loaded; a step that would not be heavier than the one before, or as heavy as the working weight, is left out.
9. **Programs are copies.** A template becomes ordinary routines and a rotation, all of it editable. Its exercises are referred to by their stable library ids; a test checks that all of them exist. Starting a routine of the followed program counts for its day, wherever it is started from. GZCLP's third tier is double progression in 15–25 reps.
10. **Plateau.** No estimated 1RM more than 0.5% higher for three sessions after the best one, over at least two weeks, with the last session in the last two weeks. After a break of four weeks the count starts again. At most two hints at a time, on Home; each can be dismissed until the lift moves.
11. **Backups are the database rows as JSON**, through `@Serializable` on the Room entities. Records are left out (they are worked out again) and so are progress photos (they would not fit a JSON file). Restoring replaces everything else in one transaction and can be undone; library exercises keep the app's own, possibly newer, data and only get their personal settings back.
12. **Automatic backups** use WorkManager 2.12.0 (named in the spec). Workers ask Hilt through an entry point, so `androidx.hilt:hilt-work` is not needed. The folder, the schedule and the state of the last backup are in their own DataStore file, outside every backup, because the access to a folder belongs to one phone. The first backup is made when the folder is chosen, and the schedule begins one period later.
13. **CSV export** has one row per completed set, weights in the chosen unit.
14. **Strong and Hevy imports.** Headers were taken from the sample files of an open-source importer ([gossamr/swift-workout-importer](https://github.com/gossamr/swift-workout-importer)), because neither app documents its format. Both comma and semicolon files are read. Names are matched by their words and by the equipment in parentheses. An exercise that cannot hold what was logged is no match: "Running" with distances never becomes an exercise that only counts reps. Weights are in the unit the file says, or the one you pick. Ids follow from the file, so importing it again adds nothing. Names that match nothing become custom exercises unless you pick another one or leave them out. Custom exercises made this way have no muscles until you edit them.
15. **Offline pictures.** Coil's disk cache moved from the cache folder to `noBackupFilesDir`, where the system does not empty it, and may hold 512 MB. All pictures take about 300 MB (estimated from 20 of them), so they are only downloaded over Wi-Fi. `core:data` depends on `io.coil-kt.coil3:coil`, part of the library that is already in use.

## Phase 4 (2026-11-24)

1. **No Health Connect.** The spec lists it as optional, and the user asked to leave it out for now.
2. **Shortcuts and the widget share `LaunchAction`** (`core:ui`). There is only ever one workout in progress, so while one is, every shortcut and the widget open it. "Repeat last" with no history starts an empty workout.
3. **The widget's button starts the next day of the followed program**, or an empty workout when no program is followed. The "Start empty workout" shortcut always starts an empty one. The widget uses the app's theme and dynamic color. The app redraws it when what it shows changes, and the system asks for an update every six hours, because a streak can end while the app is closed. A Settings row places it on the home screen where the launcher supports that. Its button says only "Start" or "Resume" so it fits at 200% font size; the lines above say what it starts, and TalkBack hears the full label. At large font sizes the third line is left out, and the widget redraws when the font size changes while the app runs. `androidx.glance:glance-appwidget-testing` is used in tests only.
4. **Live Updates.** On Android 16 the countdown is a promoted notification with a chronometer, which the status bar chip shows. On Android 17 it also uses `MetricStyle` (`androidx.core` 1.19): the next exercise is the title, the time left a large countdown and the next set's numbers a second metric, because that style shows no text. On Android 16 the text names the next set. A countdown the user swiped away doesn't come back for the same rest, as the Live Update guidelines ask. Their examples include "starting a workout", and a rest is short, started by the user and time-sensitive.
5. **Onboarding has three pages: unit, bar and plates, program.** Choices are saved immediately, so skipping loses nothing. The last page leads to the existing programs page, which shows a program's content before it is chosen. Anyone who has already trained with the app never sees the onboarding.
6. **The baseline profile is generated on demand and committed** (`app/src/release/generated/baselineProfiles`), not on every build, because it needs a phone or an emulator. The builds it drives use their own application ID (`….benchmark`): Gradle uninstalls the app under test afterwards, and that must never be the app with the training log. `:baselineprofile` uses `androidx.benchmark`, the `androidx.baselineprofile` plugin and UiAutomator, none of which ship in the APK. `androidx.profileinstaller` does ship, so the profile also takes effect for a release APK installed with `installRelease` or `adb`, not only for one from a store. Debug builds don't use it.
7. **Startup.** WorkManager is initialized on first use, off the main thread, instead of at app start. `IntRange` counts as stable for Compose.
8. **Accessibility pass.** Every control has a TalkBack label, every card title is a heading, touch targets are at least 48 dp, and the new screens were checked at 200% font size.
