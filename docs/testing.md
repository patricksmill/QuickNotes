# Testing Strategy

QuickNotes uses a layered testing approach: fast JVM unit tests (with Robolectric where Android APIs are needed), instrumented Espresso behavior tests on device/emulator, and Dropshots for screenshot regression.

See [testing-analysis.md](testing-analysis.md) for the full stack audit and gaps.

## Commands

| Task | Command |
|------|---------|
| Unit tests | `./gradlew :app:testDebugUnitTest` |
| Unit test report | `app/build/reports/tests/testDebugUnitTest/index.html` |
| Code coverage | `./gradlew :app:jacocoTestReport` |
| Coverage report | `app/build/reports/jacoco/jacocoTestReport/html/index.html` |
| Lint | `./gradlew :app:lintDebug` |
| Instrumented tests | `./gradlew :app:connectedDebugAndroidTest` |
| Record screenshots | `./gradlew :app:recordDebugAndroidTestScreenshots` |
| Lint report | `app/build/reports/lint-results-debug.html` |

## Test Layout

```
app/src/
├── test/                          # JVM unit tests (Robolectric when needed)
│   └── java/.../
│       ├── model/
│       │   ├── PersistenceTest.kt
│       │   ├── note/NoteLibrarySearchTest.kt
│       │   └── tag/
│       │       ├── AiModelCatalogTest.kt
│       │       ├── TagSettingsManagerTest.kt
│       │       └── TagTest.kt
│   └── (future Robolectric Espresso tests)
└── androidTest/                   # Device/emulator tests
    └── java/.../
        ├── view/
        │   ├── ManageNoteFragmentTest.kt
        │   ├── SearchNotesFragmentTest.kt
        │   ├── SearchNotesScreenshotTest.kt
        │   └── SettingsFragmentTest.kt
    └── screenshots/               # Dropshots reference images (after record)
```

## Frameworks

- **JUnit 5** — pure unit tests (`TagTest`)
- **JUnit 4 + Vintage engine** — Robolectric tests (`@RunWith(RobolectricTestRunner::class)`)
- **Robolectric 4.16** — Android framework fakes on JVM (`@Config(sdk = [33])`)
- **Espresso** — UI behavior (`androidTest/`; can also run in `test/` with Robolectric)
- **UI Automator** — system dialogs in `SearchNotesFragmentTest`
- **Dropshots** — instrumented screenshot comparison
- **JaCoCo** — unit test coverage

## Writing New Tests

### Unit tests (`test/`)

Use for ViewModels, repositories, model classes, and persistence. Prefer **fakes** over mocks. Use Robolectric when `Context`, `SharedPreferences`, or file I/O is required.

Do **not** unit test Activities, Fragments, or DI modules.

### Instrumented tests (`androidTest/`)

Use for full integration flows that need a real device: notifications, system UI, exact alarms, and Dropshots screenshots.

### Screenshot tests

1. Write test with `Dropshots` rule and `dropshots.assertSnapshot(...)`.
2. Record baselines: `./gradlew :app:recordDebugAndroidTestScreenshots`
3. Commit images under `app/src/androidTest/screenshots/`.
4. Verify: `./gradlew :app:connectedDebugAndroidTest`

## CI

GitHub Actions (`.github/workflows/android.yml`) runs lint and unit tests on every push/PR. Instrumented tests are local/optional until an emulator job is added.

## Future Work

- [ ] Introduce **Hilt** for test doubles (network, AI, storage)
- [ ] Add Robolectric Espresso tests in `test/` for faster UI feedback
- [ ] Expand unit coverage: `AutoTaggingService`, `TagRepository`, `TagManager`
- [ ] Add emulator job to CI for `connectedDebugAndroidTest`
- [ ] Navigation tests (back stack, settings flow)
