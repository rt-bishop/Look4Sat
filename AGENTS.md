# Look4Sat AI Agent Instructions

This is the canonical project guide for all AI assistants working on Look4Sat.
All assistant-specific files (`CLAUDE.md`, `.github/copilot-instructions.md`) point here.

---

## Project Overview

Look4Sat is an open-source, fully offline Android satellite tracker and pass predictor. It tracks 9000+ active
satellites using Celestrak/SatNOGS orbital data, calculates positions via SGP4/SDP4, and predicts passes relative to
the user's location. Features include polar radar visualization, SSTV image decoding, and ground track mapping. No ads,
no tracking, no network required after initial data download.

## Architecture & Design

**MVI (Model-View-Intent)** with unidirectional data flow:
- `State` data class (named `<Feature>State`) exposed via `StateFlow` from ViewModel
- `Action` sealed interface (named `<Feature>Action`) dispatched to ViewModel's `onAction()`
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

**Feature isolation:**
- `feature:*` modules depend only on `core:domain` and `core:presentation`.
- No feature-to-feature dependencies; cross-feature communication goes through core layers.

## Build & Platform

```shell
# Debug build
./gradlew assembleDebug

# Release build (minified, shrunk resources)
./gradlew assembleRelease

# Run tests
./gradlew test
```

- **Min SDK**: 24 | **Target SDK**: 36 | **JDK**: 17
- **Gradle**: Version catalog in `gradle/libs.versions.toml` + convention plugins in `build-logic/`

## Tech Stack

- **Compose** (BOM 2026.05.01) + Material3 Adaptive
- **Navigation3**: Type-safe navigation with `@Serializable` nav keys
- **Room** (KSP code generation) for local satellite/orbital storage
- **OkHttp** 5.x for data downloads
- **OSMDroid** for map rendering
- **Kotlin Serialization** for navigation args and parsing
- **Coroutines** + `StateFlow` for async/reactive patterns
- **Localization**: 7 languages (en, es, ru, si, tr, uk, zh)

## Data Formats & Migration

Look4Sat supports both TLE and OMM (Orbit Mean-Elements Message) CSV formats:

- **TLE format**: Legacy 3-line element format limited by 5-digit NORAD IDs
- **OMM/CSV format**: Successor format with ISO 8601 timestamps and larger NORAD ID support
- New 5-digit NORAD IDs are exhausted; TLE is officially deprecated and OMM/CSV is the clear default
- `DataParser.kt` supports both via `parseTLEStream()` and `parseCSVStream()`
- Downloads auto-detect format; both produce identical `OrbitalData` objects
- Existing code already supports transparent source transition without feature changes
- Refresh orbital data weekly for accurate pass prediction (orbital decay)

## Engineering Heuristics (Lazy = Efficient)

- Treat "lazy" as efficient, not careless: the best code is the code never written.
- First understand the task and trace the real flow end-to-end, then climb this ladder:
  1. Does this need to be built now? (YAGNI)
  2. Does it already exist in this codebase? Reuse helpers/patterns before rewriting.
  3. Does Kotlin/Java stdlib already solve it?
  4. Does the Android/platform API already solve it?
  5. Does an already-installed dependency solve it?
  6. Can this be simpler (including one-liner simple)?
  7. Only then: write the minimum code that works.
- Prefer deletion to addition, boring over clever, and the fewest touched files.
- Avoid new abstractions, dependencies, and boilerplate unless explicitly requested.
- Manual DI only: ViewModels use companion `factory()` methods with `IMainContainer`.
- Release builds use ProGuard: avoid reflection-heavy libraries unless explicitly approved.
- When two options are similar in size, choose the edge-case-correct one.
- If you keep a deliberate simplification (for example O(n^2) scan or global lock), leave a short comment with the ceiling and upgrade path.
- For complex asks, challenge scope when appropriate: "Do you need X, or does Y already cover it?"

## Bug-Fix Policy

- Fix root cause, not just the reported symptom.
- If touching a shared function, inspect callers and prefer one shared fix over per-caller patches.
- The smallest correct diff wins only after behavior is understood.

## Roadmap

- **KMP migration**: `core:domain` is to become a fully shareable KMM module. Keep it pure Kotlin/JVM.

## Gotchas

- Orbital math lives in `core:domain/predict/` — dense vector math (SGP4/SDP4). Tread carefully.
- SSTV decoding in `feature:radar` is experimental; image quality depends on signal strength during satellite pass.
- `build-logic/convention/` contains shared Gradle configuration — edit there, not in individual modules.

## Copilot Working Mode: Code-Only

- Default to code changes only. Provide explanations in chat only.
- If documentation seems useful, ask first before creating files.
- Do NOT create any `.md` documentation files unless explicitly requested.
- Do NOT add README, guides, summaries, migration notes, or how-to files unless asked.
- Prefer minimal diffs focused on requested implementation.
- Default validation is static checks (`get_errors`). Do NOT run Gradle compile/test tasks unless explicitly requested.
