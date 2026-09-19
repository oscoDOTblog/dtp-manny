# Refresh AGENTS.md for dtp-manny

### Summary

Update only the root `AGENTS.md`, preserving Mandarin flashcard product intent while clearly separating planned V1 features from the current Compose starter app.

### Changes

- Document the verified configuration: single `:app` module, package `party.debaucherytea.manny`, compile SDK 37, target SDK 36, minimum SDK 30, AGP 9.2.1, Gradle 9.4.1, Kotlin Compose plugin 2.2.10, Gradle daemon JDK 21, and Java compatibility 11.
- Record Compose BOM `2026.02.01` and AndroidX Core `1.19.0`; identify Gradle configuration and the version catalog as the authoritative sources.
- Describe the existing activity, Compose theme, resources, and starter tests. Explicitly identify flashcards, bundled vocabulary JSON, ViewModels, and repositories as unimplemented.
- Preserve planned learning interactions, vocabulary fields, Mandarin conventions, accessibility guidance, and exclusions for backend, authentication, generated images, and TTS.
- Replace unconditional references to missing `docs/PRODUCT.md`, `docs/ARCHITECTURE.md`, and `docs/ROADMAP.md` with guidance to consult them when present. Do not create these documents.
- Consolidate repeated engineering rules. Document `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:checkDebugAarMetadata` for relevant changes, plus device-dependent `:app:connectedDebugAndroidTest` for instrumentation checks.

### Validation

- Cross-check documented versions, paths, and implementation claims against the repository.
- Review the diff to confirm only `AGENTS.md` changes and product intent remains intact.
- No build required for this documentation-only update; do not imply starter tests validate future flashcard functionality.

### Assumptions

- Existing Mandarin/V1 requirements remain the intended product specification.
- No application code, dependencies, SDK settings, or public interfaces change.
