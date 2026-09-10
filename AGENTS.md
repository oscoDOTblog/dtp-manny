# AGENTS.md

## Project and current implementation

Manny (`dtp-manny`) is a local Mandarin vocabulary flashcard app built with
Kotlin, Jetpack Compose, and Material 3.

- One Android application module: `:app`; Gradle root project name: `Manny`.
- Namespace and application ID: `party.debaucherytea.manny`.
- `MainActivity.kt` creates the asset repository and hosts `MannyApp`.
- `data/FlashcardRepository.kt` loads and validates UTF-8 JSON on the IO dispatcher.
- `ui/` contains Navigation Compose routing, the section list, card navigation,
  and a reusable animated flashcard. Loading failures offer a retry.
- `audio/` defines the PronunciationPlayer abstraction with an asset-backed
  MediaPlayer implementation. The flashcard 🔊 button plays `audioPath` when
  present and stays disabled otherwise; playback stops when leaving a section
  and resources are released with the Activity.
- `strokes/` holds the HanziStrokeData/StrokePoint models, strict schema
  parsing, and the StrokeDataRepository interface. `assets/strokes/` has
  imported U+XXXX.json data for all 63 vocabulary Hanzi (Arphic-licensed, see
  THIRD_PARTY_NOTICES.md). AssetStrokeDataRepository loads with an in-memory
  cache (missing is null, corrupt is an error); StrokePaths converts SVG
  stroke strings via AndroidX PathParser with an isolated 900-space
  centering transform. HanziStrokeView renders the first `shownStrokes`
  strokes on Canvas (theme-aware fill, paths parsed once, optional
  partially-drawn current stroke via PathMeasure segments);
  StrokeProgressionControls provides Previous/Next/Reset with an n/total
  readout. StrokeOrderSheet hosts autoplaying draw animation with
  pause/resume/restart in a bottom sheet (loading/missing/ready states) plus
  a FilterChip selector for multi-character cards. The flashcard back has a
  full-width Strokes button, enabled when at least one card character has
  data (checked per card via extractHanzi + repository).
- `app/src/main/assets/vocabulary.json` contains 40 radicals, 10 numbers, and 3
  animals. Parenthesized radical variants are retained in the Hanzi field.
- Cards begin with a shared vector illustration (except 人, which uses a bundled
  retro person PNG), flip to Hanzi, and reveal
  Pinyin/English on request. Audio is visible but disabled. Previous/next controls
  have boundaries; a shuffle control randomizes the card order and restarts at the
first card; position, order, and reveal state survive activity recreation.
- `ui/theme/` provides light/dark themes and dynamic colors on supported devices.
- Instrumentation tests cover bundled data, invalid JSON/data, and the learning
  flow including state restoration. No ViewModels, database, or network layer
  are currently needed or implemented.

Inspect existing code first. Do not implement planned features unless requested.
Do not introduce architecture for hypothetical future requirements.

## Build configuration

| Setting | Current value |
| --- | --- |
| Compile SDK | 37, configured with `release(37)` |
| Target SDK | 36 |
| Minimum SDK | 30 |
| Android Gradle Plugin | 9.2.1 |
| Gradle wrapper | 9.4.1 |
| Kotlin Compose plugin | 2.2.10 |
| Gradle daemon JDK | 21 |
| Java source/target compatibility | 11 |
| Compose BOM | 2026.02.01 |
| AndroidX Core / Core KTX | 1.19.0 |
| Navigation Compose | 2.9.7 |

Treat `app/build.gradle.kts`, `gradle/libs.versions.toml`,
`gradle/wrapper/gradle-wrapper.properties`, and
`gradle/gradle-daemon-jvm.properties` as authoritative for these settings.
Manage dependency versions through the version catalog and Compose BOM.
The daemon JDK and Java source/target compatibility are separate settings.

AndroidX Core 1.19.0 requires compile SDK 37 or later. Keep compile SDK, target SDK,
and minimum SDK changes intentional and independent; raising compile SDK does not
require changing the other two. Use the repository Gradle wrapper for checks.
Keep machine-specific SDK paths in local configuration, not shared instructions.

## V1 product behavior

The learning interaction is:

Image → recall concept → reveal Hanzi → optionally reveal Pinyin/English

V1 targets Android phones and local vocabulary loaded from bundled JSON assets.
Implemented features are vocabulary sections, a placeholder image, flashcard
front/back, Hanzi reveal, optional Pinyin/English reveal, card navigation, and a
progress indicator. Hanzi should be visually prominent on the card back;
Pinyin and English stay hidden until explicitly requested by the learner.

Do not add AI image generation, TTS, a backend, accounts, authentication, cloud
synchronization, or networking unless explicitly requested. Avoid premature
abstractions for these future capabilities.

### Vocabulary requirements

A flashcard represents a Mandarin word or expression, not necessarily one Chinese
character. Its core fields are `id`, `sectionId`, `hanzi`, `pinyin`, and
`english`. Future `imageUrl` and `audioUrl` fields must remain optional so cards
work when generated media is unavailable.

- Keep vocabulary separate from UI code; do not hardcode lists in Composables.
- Use Simplified Chinese unless otherwise specified.
- Store tone-marked Hanyu Pinyin, such as `gǒu`, `māo`, and `píngguǒ`, rather than
  numbered tones such as `gou3`, `mao1`, or `ping2guo3`.
- Pronunciation belongs to the word/expression and context; do not assume every
  Hanzi has exactly one pronunciation.
- Do not silently change existing Hanzi, Pinyin, or English definitions.

## Engineering conventions

- Use Kotlin, Jetpack Compose, and Material 3. Do not introduce XML layouts unless
  explicitly requested; existing XML resources and manifest configuration are expected.
- Prefer clarity, safety, maintainability, and simple solutions. Follow Kotlin
  conventions, use explicit names, prefer `val`, and keep functions/files focused.
- Prefer AndroidX dependencies and avoid deprecated APIs, unnecessary libraries,
  inheritance, and abstractions. Do not add a DI framework unless requested.
- Add ViewModels and repositories/services when they improve clarity and testability.
  Keep business logic outside Composables and avoid oversized ViewModels.
- Keep Composables small, declarative, and stateless where practical. Hoist state
  to the lowest reasonable owner and expose immutable, read-only UI state.
  UI events go up; state flows down. Keep mutable state and collections private.
- Use `remember` for local UI state and `rememberSaveable` for appropriate state
  that should survive recreation. Use Compose side effects intentionally.
- Prefer coroutines and Flow for asynchronous work and lifecycle-aware collection
  for UI flows; add supporting dependencies only when needed.
- Keep parsing and expensive work out of Composables. Avoid unnecessary
  recomposition, use lazy layouts with stable keys for large lists, and avoid
  retaining Activity/Context references in long-lived objects.
- Handle recoverable failures through appropriate UI state and user-friendly
  errors. Do not silently fail or use broad catch blocks. Log useful diagnostics
  without sensitive information.
- Write production-ready, Play Store compliant code. Never commit secrets or API
  keys or embed future service credentials in the app; use secure backend/environment
  configuration when external services are explicitly added.

## UI and accessibility

- Follow Material 3 and respect system font scaling.
- Give important images and icons meaningful content descriptions and interactive
  controls appropriate touch targets.
- Keep new user-facing text in Android string resources and design for localization.

## Testing and verification

Prioritize meaningful unit tests for business logic, parsing, transformations,
and future ViewModels/repositories. Keep pure logic independent of Android where
practical. Use `kotlinx-coroutines-test` when coroutine tests are introduced;
it is not currently a declared dependency.

Run checks appropriate to the change from the repository root:

- App code or behavior changes:
  `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- SDK or dependency changes:
  `./gradlew :app:checkDebugAarMetadata :app:assembleDebug`
- Instrumentation or Compose UI checks, with a compatible connected device or
  running emulator: `./gradlew :app:connectedDebugAndroidTest`

Fix failures introduced by the change and report checks actually run, including
any device/environment limitations. Do not confuse starter tests with feature coverage.
Documentation-only changes require a content/diff review, not a build.

## Documentation

`docs/PRODUCT.md`, `docs/ARCHITECTURE.md`, and `docs/ROADMAP.md` do not currently
exist. When present, consult them for intended behavior, current architecture,
and planned features respectively. Update applicable existing documents when
behavior or architecture meaningfully changes; do not assume missing documents
exist or create them solely to satisfy these references.

Keep this file aligned with verified project configuration and clearly distinguish
implemented behavior from plans. Do not mark roadmap items complete until they
are implemented. Add brief comments explaining non-obvious decisions rather than
obvious code, use KDoc where public APIs benefit, and explain meaningful tradeoffs.
