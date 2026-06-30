# QuickNotes — Agent Guide

## Testing

Follow the testing strategy in [docs/testing.md](docs/testing.md).

Before claiming Android/Kotlin work is complete:

```bash
./gradlew :app:lintDebug --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
```

Fix all lint **errors**. Unit tests must pass.

## Project structure

- `app/src/main/java/.../model/` — business logic (notes, tags, persistence, AI settings)
- `app/src/main/java/.../view/compose/` — Jetpack Compose UI (theme, components, screens, sheets, navigation)
- `app/src/main/java/.../controller/` — Activity, notifications

Jetpack Compose + Navigation Compose (Material 3). No View Binding. No DI framework yet.

## Deferred plans

After Compose migration, see [docs/plans/](docs/plans/) — e.g. native tag color swatch picker + tonal chips.

## Key conventions

- Conventional Commits for git messages
- Lint report: `app/build/reports/lint-results-debug.html`
- Min SDK 30, compile/target SDK 36, JDK 17
