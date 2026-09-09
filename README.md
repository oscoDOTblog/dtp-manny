# Manny

A minimal, offline Mandarin flashcard app for Android (API 30+), built with Kotlin,
Jetpack Compose, Material 3, and Navigation Compose.

Choose a section, tap the placeholder illustration to reveal Hanzi, and use
**Show meaning** to toggle Pinyin and English. Tap the card again to return to its
front. **Previous** and **Next** move through the section; every new card starts
on its front. Audio is visibly disabled for V1.

## Bundled vocabulary

`app/src/main/assets/vocabulary.json` contains three sections:

- Chinese radicals: 40 entries transcribed from the two supplied radical screenshots.
- Numbers: 1–10 from the supplied number screenshot.
- Animals: 狗 / gǒu / dog, 猫 / māo / cat, 鸟 / niǎo / bird.

The supplied material was three screenshots, not a PDF file. Parenthesized radical
variants are retained in `hanzi`; source tone marks and English glosses are preserved.
The enclosure radical 囗 is distinct from mouth 口. Source footnote markers are
not vocabulary. All cards reuse `flashcard_placeholder.xml`; the illustration is
not a word-specific clue in V1.

The JSON is an array of `{id, title, cards}` sections. Each card has
`{id, sectionId, hanzi, pinyin, english}`. IDs must be unique, text nonblank, and
`sectionId` must match the enclosing section. Empty sections display an empty state.

## Architecture

`MainActivity` supplies an asset-only repository to `MannyApp`. The repository
reads UTF-8 JSON on `Dispatchers.IO`, and `parseSections` validates it. Compose
handles loading/error/retry state separately from the section and flashcard UI.
Navigation Compose handles section routes and system back navigation. Saveable
UI state retains the current card and reveal state during activity recreation.
There is no backend, networking, authentication, database, generated media, or TTS.

## Build and test

Use the Gradle wrapper with daemon JDK 21 and Android compile SDK 37 installed.
Target SDK is 36; Java source/target compatibility is 11.

```sh
./gradlew :app:checkDebugAarMetadata :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

The second command requires a connected device or emulator. Instrumentation tests
exercise JSON validation, vocabulary fidelity, and the section/flip/reveal/navigation
flow with activity recreation. The debug APK is at
`app/build/outputs/apk/debug/app-debug.apk`.
