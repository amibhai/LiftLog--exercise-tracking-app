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
