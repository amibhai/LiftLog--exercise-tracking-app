# LiftLog

**A fast, offline-first strength-training logger for Android.**
Log a set in a couple of taps, see what you lifted last time, and let the app suggest the next weight. No accounts, no ads, no network required.

![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203%20Expressive-4285F4?logo=jetpackcompose&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-blue)

---

## Features

**Logging**
- Workout screen built for the gym floor: large touch targets, a docked number pad, one-handed entry.
- Previous performance and ghost values on every set; completed sets fill the next one in.
- Set types (warm-up, working, drop, failure), RPE/RIR, supersets, notes, seven exercise types (weight × reps, bodyweight, assisted, duration, distance, carries, cardio).
- Rest timer that survives process death, with a Live Update countdown on Android 16+ and notification actions.
- Plate calculator and warm-up generator, kg and lb with exact round-trips.

**Planning**
- Routines, six built-in programs and your own rotations, with the next workout ready on Home.
- Progression engine (linear, double progression, and more) that suggests the next weight and reps.
- Plateau detection with gentle insight cards.

**Progress**
- Per-exercise charts: estimated 1RM, top set, volume and reps over 1M / 3M / 6M / 1Y / All.
- Automatic personal records with in-workout badges and a finish summary; rep-max tables.
- Dashboard with weekly volume, muscle heatmap and streaks; body weight, measurements and progress photos.

**Library**
- 1,600+ exercises from open datasets, with muscles, equipment, instructions and attribution.
- Instant full-text search that understands abbreviations (`rdl`, `ohp`, `db bench`) and ranks by what you use most.
- Custom exercises; built-ins can be hidden, never lost.

**Your data**
- Everything stays on the phone. Automatic JSON backups to a folder you choose, CSV export.
- Import from Strong and Hevy CSV exports.

**Polish**
- Home-screen widget, app shortcuts, onboarding, dynamic color with a custom dark palette.
- TalkBack labels on every control, layouts that hold at 200% font size.
- Baseline profile for fast startup and smooth scrolling.

## Tech stack

| Area | Choice |
|---|---|
| Language / UI | Kotlin 2.4, Jetpack Compose, Material 3 Expressive, single activity |
| Architecture | MVVM with unidirectional data flow, multi-module, convention plugins |
| DI | Hilt (KSP) |
| Navigation | Navigation Compose with type-safe `@Serializable` routes |
| Storage | Room 3 with bundled SQLite, FTS4 search, prepopulated asset; JSON DataStore |
| Background | WorkManager (backups), Glance (widget) |
| Charts / images | Vico, Coil 3 |
| Quality | detekt + ktlint + compose-rules, Android Lint, warnings as errors |
| Tests | JUnit 6 + Truth (domain), JUnit 4 + Robolectric + Turbine (Android), Compose UI and Room migration tests |
| Performance | Baseline Profile + Macrobenchmark |

minSdk 26 · targetSdk 37 · compileSdk 37.1 · AGP 9 · Gradle 9

## Project structure

```
app/                 Application, MainActivity, navigation host, DI wiring
core/domain/         Pure Kotlin: models, use cases and all algorithms
core/database/       Room entities, DAOs, migrations, exercise asset
core/data/           Repositories, settings, backup and import
core/ui/             Theme and reusable components
core/testing/        Shared test utilities
feature/*            home, workout, exercises, routines, history, progress, settings, onboarding, widget
baselineprofile/     Profile journeys and benchmarks
tools/               Exercise library data pipeline (Python)
docs/                Product spec and design decisions
```

Features depend only on `core:domain` and `core:ui`; the build enforces it.

## Getting started

Requirements: JDK 17+, Android SDK with platform 37.1. To open the project in an IDE you need Android Studio 2026.2.2 or newer.

```bash
git clone https://github.com/amibhai/LiftLog--exercise-tracking-app.git
cd LiftLog--exercise-tracking-app

./gradlew installRelease   # fast build with R8 and the baseline profile, on a phone with USB debugging
./gradlew installDebug     # debug build
```

Use `.\gradlew.bat` on Windows PowerShell. Debug and release builds share an application ID and signing key, so either one updates the other and keeps your data. To sign with your own key, add a `keystore.properties` file.

## Development

| Task | Command |
|---|---|
| Full check | `./gradlew test lint detekt assembleDebug` |
| Domain tests | `./gradlew :core:domain:test` |
| Auto-format | `./gradlew detekt --auto-correct` |
| Baseline profile (Android 13+ device or emulator) | `./gradlew :app:generateBaselineProfile` |
| Benchmarks (real phone) | `./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest` |
| Compose stability reports | `./gradlew assembleRelease -PcomposeReports=true` |
| Rebuild the exercise library | `python tools/exercise-pipeline/build_library.py` |

Database schemas are exported to `core/database/schemas/`; every schema change ships with a migration and a migration test. See [`docs/SPEC.md`](docs/SPEC.md) for the product spec and [`docs/DECISIONS.md`](docs/DECISIONS.md) for design decisions, and [`tools/exercise-pipeline/README.md`](tools/exercise-pipeline/README.md) for the data pipeline.

## Acknowledgements

Exercise data comes from [free-exercise-db](https://github.com/yuhonas/free-exercise-db) (Unlicense) and [wger](https://wger.de) (CC BY-SA and other open licenses, credited per exercise in the app). Icons are [Material Symbols](https://fonts.google.com/icons) (Apache 2.0).

## License

[MIT](LICENSE) © 2026 Swastik
