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
11. **ktlint uses the `android_studio` code style**, the standard Android Kotlin style.
12. **New dependencies approved by the user on 2026-11-23: Robolectric and compose-rules.** Both are test-time or build-time only and never ship in the APK.

## Product and data decisions

- **Weight display precision.** The spec says to "round on display to the active unit's increment". We round to a fixed 0.01 of the active unit, never to the plate step, so every typed value displays back exactly. The per-exercise step drives only the −/+ buttons and plate math.
- **One applicationId for debug and release.** Each phase's `installDebug` upgrades in place and keeps logged data. Release builds are signed with your key from `keystore.properties` if that file exists, otherwise with the debug key, so the two builds can replace each other.
- **Schema changes.** We never use destructive migration fallback. Once a build is installed, every schema change gets a version bump, a migration and a migration test.
