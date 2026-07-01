# QuickNotes Testing Analysis

Analysis date: 2026-06-30

## Summary

QuickNotes is a **Jetpack Compose** Android app with **no dependency injection framework**. Testing uses JVM unit tests (Robolectric where Android APIs are needed) and Compose UI instrumented tests on device/emulator.

## Stack Inventory

| Area | Current setup |
|------|----------------|
| **DI** | None (manual `Context` construction) |
| **Unit test framework** | JUnit 5 (Jupiter) via `useJUnitPlatform()` |
| **Mocking** | None |
| **Robolectric** | Added in this setup (4.16.1) |
| **UI paradigm** | Jetpack Compose (Material 3) |
| **Behavior UI tests** | Compose UI Test in `androidTest` (device/emulator) |
| **Screenshot tests** | Dropshots added for instrumented screenshots |
| **E2E / UI Automator** | Available; Compose UI Test preferred for in-app flows |
| **Coverage** | JaCoCo added (`jacocoTestReport` task) |
| **CI** | Runs lint + unit tests; does **not** run instrumented tests |

## Existing Tests (before setup)

| Location | Type | Count | Notes |
|----------|------|-------|-------|
| `app/src/test/` | Unit | 0 | Empty |
| `app/src/androidTest/` | Instrumented | 4 | Espresso fragment tests + `TagSettingsManagerTest` |

`TagSettingsManagerTest` lived in `androidTest` but exercised pure preference logic — a good candidate for Robolectric unit tests.

## Business Logic Candidates for Unit Tests

| Class | Priority | Notes |
|-------|----------|-------|
| `NoteLibrary` | High | Search, add/delete, pin — needs Robolectric for `Context` |
| `Persistence` | High | JSON round-trip |
| `TagSettingsManager` | High | Provider settings, legacy migration |
| `AiModelCatalog` | Medium | Curated/merged model lists |
| `Tag` | Medium | Case-insensitive equality |
| `AutoTaggingService` | Medium | Keyword tagging — needs refactor or Robolectric |
| `TagRepository` | Medium | Tag color/name operations — needs Robolectric |
| `TagManager` | Low | Orchestrates tagging + network |
| Activities / Compose screens | Skip | Covered by Compose UI instrumented tests, not unit tests |

## Gaps and Recommendations

### Gson + JVM records

`Tag` is a `@JvmRecord` data class. Gson 2.13 cannot serialize `Note` objects containing tags on the desktop JVM (Robolectric). Note JSON persistence tests belong in `androidTest`; Robolectric unit tests for `NoteLibrary` seed notes via reflection to avoid persistence.

### Dependency injection (not installed)

The app constructs dependencies manually (`NoteLibrary(ctx)`, `TagManager(this)`). **Hilt is recommended** before adding runtime fakes for network, AI, and storage. A full Hilt migration is a separate effort; fakes for instrumented tests should wait until DI is in place.

### Robolectric vs instrumented split

| Test type | Sourceset | Runner |
|-----------|-----------|--------|
| Unit + Robolectric UI | `test/` | JVM, fast |
| Device Espresso / Dropshots | `androidTest/` | Emulator/device |

Existing Espresso tests remain in `androidTest`. New model-layer tests run locally with Robolectric in `test/`.

### Screenshot testing

- **Dropshots** (instrumented): Added for the search empty-state screen. Baselines live in `app/src/androidTest/screenshots/`.
- **Paparazzi** (local View screenshots): Not added — consider if you want JVM screenshot tests without a device.

### CI gap

Instrumented tests (Espresso, Dropshots) are not in `.github/workflows/android.yml`. Add a `connectedDebugAndroidTest` job with an emulator when you want screenshot and UI tests in CI.

### Coverage

Run `./gradlew :app:jacocoTestReport` after unit tests. Report: `app/build/reports/jacoco/jacocoTestReport/html/index.html`.

## Changes Applied in This Setup

1. Robolectric 4.16.1 + JaCoCo + Dropshots 0.6.0
2. Unit tests for `TagSettingsManager`, `AiModelCatalog`, `NoteLibrary` search, `Persistence`, `Tag`
3. Moved `TagSettingsManager` tests: preferences/defaults in `test/`, encrypted API keys in `androidTest`
4. Added `SearchNotesScreenshotTest` (Dropshots)
5. Documentation: `docs/testing.md`, `AGENTS.md`
