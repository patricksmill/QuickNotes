# Changelog

## [1.2.0](https://github.com/patricksmill/QuickNotes/compare/QuickNotes-v1.1.0...QuickNotes-v1.2.0) (2026-06-30)


### Headline: ~94% smaller release APK
v1.1.0 Release APK: ~44 MB
v1.2.0 Release APK: ~2.71 MB

The release build now uses R8 code shrinking plus resource shrinking, with updated ProGuard keep rules for persisted Note and Tag models. Unused code and resources are stripped from release artifacts — same app, much smaller download.

#### Performance & size
- Enabled resource shrinking (isShrinkResources = true) alongside existing R8 minification
- Tuned ProGuard rules so Gson-backed note/tag persistence still works after obfuscation
- Release APK drops from ~44 MB → ~2.71 MB (~94% reduction)

#### Build & platform
- Updated Gradle toolchain and Android Gradle Plugin 9 configuration
- Target compile/target SDK 36
- Resource packaging cleanup (duplicate META-INF handling)

#### Reliability
- Declared ACCESS_NETWORK_STATE so connectivity checks pass lint and behave correctly on device
- CI release pipeline fixed to build and attach AAB/APK for QuickNotes-v* tags (Release Please tags)

#### Developer / quality (no user-facing UI changes)
- Robolectric unit tests for model-layer logic (search, persistence, AI settings)
- JaCoCo coverage reporting (./gradlew :app:jacocoTestReport)
- Dropshots scaffolding for instrumented screenshot tests
- Testing docs: docs/testing.md, docs/testing-analysis.md

## [1.1.0](https://github.com/patricksmill/QuickNotes/compare/QuickNotes-v1.0.7...QuickNotes-v1.1.0) (2026-05-05)


### Features

* Add Release Please configuration and workflow for automated rel… ([e0eee75](https://github.com/patricksmill/QuickNotes/commit/e0eee75b9c945d9c28da63e00ab5628fc0b15067))
* Add Release Please configuration and workflow for automated releases ([8db872a](https://github.com/patricksmill/QuickNotes/commit/8db872aa1424daab994b58909012f68a453b1742))
* Refactor AI tagging system to support multiple providers and enhance settings ([25c8bbd](https://github.com/patricksmill/QuickNotes/commit/25c8bbdf3b38f7b1a8825759e1f7ca18695c9271))
* Refactor AI tagging system to support multiple providers and enhance settings ([f869889](https://github.com/patricksmill/QuickNotes/commit/f869889bca62343981b1bf78252a761c1e005abe))
