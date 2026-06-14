# Look4Sat AI Agent Instructions

This is the canonical project guide for all AI assistants working on Look4Sat.
All assistant-specific files (`CLAUDE.md`, `.github/copilot-instructions.md`) point here.

---

## Project Overview

Look4Sat is an open-source, fully offline Android satellite tracker and pass predictor. It tracks 9000+ active
satellites using TLE/OMM data from Celestrak/SatNOGS, calculates orbital positions via SGP4/SDP4 models, and displays
passes relative to the user's location. Features include polar radar visualization, SSTV image decoding, satellite
ground track mapping, and pass predictions up to 10 days ahead. No ads, no tracking, no network required after initial
data download.

## Architecture

**MVI (Model-View-Intent)** with unidirectional data flow:
- `State` data class → exposed via `StateFlow` from ViewModel
- `Action` sealed interface → user intents dispatched to ViewModel's `onAction()`
- Jetpack Compose UI observes state and recomposes reactively

**Clean Architecture layers:**

| Module               | Responsibility                                                      |
|----------------------|---------------------------------------------------------------------|
| `app`                | Entry point. Aggregates all modules                                 |
| `core:data`          | Android library. Room DB, OkHttp networking, repo implementations   |
| `core:domain`        | Pure Kotlin (JVM). Orbital math (SGP4/SDP4), models, repo contracts |
| `core:presentation`  | Android library. Compose theme, shared UI components, NavKeys       |
| `feature:map`        | OSMDroid map with ground tracks                                     |
| `feature:passes`     | Pass predictions and upcoming events                                |
| `feature:radar`      | Polar radar view of satellite positions, SSTV image decoding        |
| `feature:satellites` | Satellite list, filtering, selection                                |
| `feature:settings`   | User preferences                                                    |

- `feature:*` modules depend only on `core:domain` + `core:presentation`. Features never depend on each other.

## Build & Run

```shell
# Debug build
./gradlew assembleDebug

# Release build (minified, shrunk resources)
./gradlew assembleRelease

# Run tests
./gradlew test
```

- **Min SDK**: 24 | **Target SDK**: 36 | **JDK**: 17
- **Gradle**: Uses version catalog (`gradle/libs.versions.toml`) + convention plugins in `build-logic/`

## Key Libraries

- **Compose** (BOM 2026.05.01) + Material3 Adaptive
- **Navigation3** (type-safe, uses `@Serializable` NavKeys)
- **Room** (KSP code generation) for local satellite/TLE storage
- **OkHttp** 5.x for TLE downloads
- **OSMDroid** for map rendering
- **Kotlin Serialization** for navigation args and data parsing
- **Coroutines** + `StateFlow` for async/reactive patterns

## Conventions

- **Minimal dependencies**: Avoid adding libraries when a simple manual solution exists. Fewer deps = less maintenance.
- **DI**: Manual — ViewModels use companion `factory()` methods with `IMainContainer` interface.
- **Navigation**: Type-safe Compose Navigation3 with `@Serializable` data classes as nav keys.
- **State naming**: `<Feature>State` data class + `<Feature>Action` sealed interface per feature.
- **No feature-to-feature deps**: All cross-feature communication goes through core layers.
- **Localization**: 7 languages (en, es, ru, si, tr, uk, zh).

## Data Formats & Migration

**TLE vs. OMM/CSV format:**

Look4Sat supports both TLE and OMM (Orbit Mean-Elements Message) formats for backward compatibility:

- **TLE format**: Traditional 3-line element format (deprecated). NORAD catalog numbers are 5-digit integers, which
  are running out of space. Celestrak has signaled that TLE format will eventually be phased out.
- **OMM/CSV format**: The future standard. CSV files contain the same orbital parameters as TLE but use ISO 8601
  timestamps and support larger NORAD IDs. Celestrak and SatNOGS already provide OMM data in CSV format.

**Current implementation:**
- `DataParser.kt` handles both `parseTLEStream()` and `parseCSVStream()` seamlessly
- TLE data is downloaded from configured sources and stored in Room database
- When downloading satellite data, the app automatically detects format and parses accordingly
- Both formats produce identical `OrbitalData` objects, ensuring transparent format switching

**Migration path:**
As NORAD catalog space becomes constrained, OMM/CSV will become the primary format. Look4Sat is already positioned
to handle this transition without code changes — existing users can continue using TLE files while new sources
transition to OMM/CSV automatically.

## Code Style

- Prefer **short, focused functions** — single responsibility, easy to read.
- **Exceptions**: Composable functions and math-heavy algorithms (SGP4/SDP4) may be longer.
- Strict code style — no dead code, no unused imports, consistent formatting.

## Roadmap

- **KMP migration**: `core:domain` is to become a fully shareable KMM module. Keep it pure Kotlin/JVM.

## Gotchas

- Orbital math lives in `core:domain/predict/` — it's dense vector math (SGP4/SDP4). Tread carefully.
- TLE/OMM data must be refreshed weekly for accurate predictions (satellite orbits decay). TLE format is legacy and
  will eventually be deprecated in favor of OMM/CSV as NORAD catalog numbers approach the 5-digit limit.
- SSTV decoding in `feature:radar` is experimental; image quality depends on signal strength during satellite pass.
- `build-logic/convention/` contains all shared Gradle configuration — edit there, not in individual modules.
- ProGuard is enabled for release builds — don't add reflection-based libs or any other dependencies without asking.
