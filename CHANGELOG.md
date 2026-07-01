# Changelog

## [1.3.0](https://github.com/patricksmill/QuickNotes/compare/QuickNotes-v1.2.0...QuickNotes-v1.3.0) (2026-07-01)


### Features

* add Java home configuration and remove obsolete test file ([0f45e72](https://github.com/patricksmill/QuickNotes/commit/0f45e720403040b4952ba3444e28bff974559d84))
* complete migration to Jetpack Compose UI ([5e5da73](https://github.com/patricksmill/QuickNotes/commit/5e5da73a67f4e751be1e756adb859ed6e5365640))
* enhance demo note functionality and settings integration ([33cd178](https://github.com/patricksmill/QuickNotes/commit/33cd178d8bf6102e90a3908df58ffbb7660a5488))
* enhance note management and UI with Compose features ([ad9f5c3](https://github.com/patricksmill/QuickNotes/commit/ad9f5c383bbfbd8741a2e8122023e522db871663))
* enhance tag management and search functionality ([ca7f337](https://github.com/patricksmill/QuickNotes/commit/ca7f33716e0e0a7250de65547026c60bc6bd0e01))
* fix tag color management and UI enhancements ([e39525e](https://github.com/patricksmill/QuickNotes/commit/e39525e73a1ca46eb76d5e78d23511872eb8b1ac))
* implement navigation and back handling in QuickNotes app ([e502531](https://github.com/patricksmill/QuickNotes/commit/e50253118faa863778a95b9475ab8a2e372e10b5))
* introduce new UI components for enhanced note management ([cf46b64](https://github.com/patricksmill/QuickNotes/commit/cf46b64dc79d3c8e775ce0aac5c965e66b87869c))


### Bug Fixes

* fixed a regression with pinning notes ([48bb4b2](https://github.com/patricksmill/QuickNotes/commit/48bb4b2fee84333d125e1741444354126c2f0770))

## [Unreleased]

### Headline: Full Jetpack Compose UI

QuickNotes is now entirely Compose-driven. XML layouts, View Binding, and Fragment-based screens are gone. Navigation, sheets, settings, tag management, and the onboarding tutorial all use Material 3 Compose components with proper back handling, edge-to-edge layout, and predictive back.

### Features

#### Compose UI and navigation
- Complete migration from XML Views to Jetpack Compose with Navigation Compose (`SearchNotesScreen`, `SettingsScreen`, `QuickNotesNavHost`)
- Note editor and tag manager as Material 3 bottom sheets (`ManageNoteBottomSheet`, `ManageTagsBottomSheet`)
- Compose tutorial overlay with spotlight targeting (`TutorialOverlay`)
- Slide-and-fade transitions between search and settings
- System back and predictive back: layered `BackHandler` for settings, dialogs, sheets, and tutorial
- Edge-to-edge display with inset-aware scaffolds and keyboard padding in the note editor
- Unsaved-changes confirmation when dismissing the note editor with edits

#### Notes list and search
- Pull-to-refresh on the note list
- Animated item placement when sorting, filtering, or pinning notes
- Swipe-to-delete with visible delete affordance and haptic feedback on delete/pin
- Rich empty states: “No notes yet” with create CTA, and “No matching notes” with clear-filters action
- Tag filter chips with selected-state styling (`TagFilterChip`)

#### Tags and colors
- Native tag color swatch picker (`TagColorPickerSheet`) with a curated 12-color palette
- Tonal tag chips (color dot + label) across search filters, note editor, and manage-tags
- Tag suggestions as responsive inline buttons in the note editor
- Tag color updates propagate to all notes using that tag

#### Settings and pickers
- Settings screen rebuilt in Compose (`SettingsScreen`)
- List-style dialogs replaced with bottom sheets: sort order, AI provider, model library, tag actions, AI tag suggestions
- Model library sheet includes “Refresh from API” with loading indicator

#### Notifications
- Notification permission prompt migrated to Compose (`NotificationPermissionDialog`)
- Uses Activity Result API; granted permission applies the pending reminder without re-toggling

### Fixed

- Pin toggle regression that prevented notes from staying pinned correctly
- Tag color management: case-insensitive tag matching and consistent color lookup via `TagRepository`
- Tag replacement on notes now case-insensitive

### Changed

- Duplicate note titles are allowed (case-insensitive uniqueness check removed)
- `compileSdk` raised to 37
- Search query held in reactive Compose state for smoother list updates
- Note and tag UI state consolidated in `ControllerActivity` for simpler recomposition

### Removed

- Legacy View fragments: `SearchNotesFragment`, `ManageNoteFragment`, `ManageTagsFragment`, `SettingsFragment`, `TagColorSettingsFragment`, `TutorialOverlayFragment`
- `MainUI` fragment container helper and `TagSuggestion` View adapters
- All fragment XML layouts, slide transition animations, and `root_preferences.xml`
- View Binding and unused `recyclerview` / `constraintlayout` dependencies

### Developer / quality

- Instrumented tests renamed and updated for Compose: `SearchNotesScreenTest`, `ManageNoteSheetTest`, `SettingsScreenTest`
- New Compose component tests: `TagFilterChipTest`, `QuickNotesThemeTest`
- Unit tests added for note pinning, duplicate titles, and tag set operations
- Architecture and testing docs updated to reflect Compose screens and removed fragments
- Cursor rule for `JAVA_HOME` on Windows/Android Studio setups

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
* Refactor AI tagging system to support multiple providers and enhance settings ([25c8bbd](https://github.com/patricksmill/quicknotes/commit/25c8bbdf3b38f7b1a8825759e1f7ca18695c9271))
* Refactor AI tagging system to support multiple providers and enhance settings ([f869889](https://github.com/patricksmill/QuickNotes/commit/f869889bca62343981b1bf78252a761c1e005abe))
