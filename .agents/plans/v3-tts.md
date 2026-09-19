Yes. For V3, I’d make TTS a **build-time content-generation pipeline**, not a runtime Android dependency. The coding harness can call Amazon Polly, save the generated files into the repo, update the vocabulary metadata, and then the Android app simply plays local audio.

That keeps the app offline, cheap, deterministic, and removes AWS credentials from the client.

## V3 goal

Add Mandarin pronunciation playback to flashcards.

The user flow becomes:

```text
Front
┌──────────────────────────┐
│                          │
│          IMAGE           │
│                          │
└──────────────────────────┘

             ↓ flip

Back
┌──────────────────────────┐
│                          │
│            狗            │
│                          │
│       ⓘ       🔊         │
│                          │
└──────────────────────────┘

ⓘ → gǒu · dog
🔊 → plays pronunciation
```

The important architecture is:

```text
Amazon Polly
     ↑
coding harness
     ↑
vocabulary.json
     │
     ├── 狗
     ├── 猫
     └── 苹果
     ↓
generated audio
     ↓
assets/audio/
     ↓
Android Audio Player
```

The Android application should **never call Polly directly**.

---

# V3 data model

Extend the existing flashcard model with an optional local audio reference:

```kotlin
data class Flashcard(
    val id: String,
    val sectionId: String,
    val hanzi: String,
    val pinyin: String,
    val english: String,
    val imagePath: String? = null,
    val audioPath: String? = null
)
```

Example JSON:

```json
{
  "id": "dog",
  "sectionId": "animals",
  "hanzi": "狗",
  "pinyin": "gǒu",
  "english": "dog",
  "imagePath": "images/dog.webp",
  "audioPath": "audio/dog.mp3"
}
```

Keep `audioPath` optional.

That gives you:

```text
audio exists
    ↓
🔊 enabled

audio missing
    ↓
🔊 disabled
```

rather than breaking the card.

---

# Recommended repository structure

```text
mandarin-flashcards/
│
├── AGENTS.md
│
├── docs/
│   ├── PRODUCT.md
│   ├── ARCHITECTURE.md
│   └── ROADMAP.md
│
├── tools/
│   └── tts/
│       ├── generate_tts.py
│       ├── validate_tts.py
│       └── README.md
│
├── content/
│   ├── vocabulary.json
│   └── tts-manifest.json
│
└── app/
    └── src/main/
        └── assets/
            ├── images/
            └── audio/
                ├── dog.mp3
                ├── cat.mp3
                └── apple.mp3
```

I like separating:

```text
content/
```

from Android code because your harness can manipulate content without touching application logic.

---

# TTS generation pipeline

The harness should be able to run something like:

```bash
python tools/tts/generate_tts.py
```

and get:

```text
Reading vocabulary...

Animals
✓ 狗 → dog.mp3
✓ 猫 → cat.mp3
○ 苹果 → already exists, skipped

Food
✓ 米饭 → rice.mp3
✓ 牛奶 → milk.mp3

Generated: 4
Skipped:   1
Failed:    0
```

The pipeline should work like this:

```text
Load vocabulary.json
        ↓
Find cards with no audioPath
        ↓
Validate Hanzi
        ↓
Generate deterministic filename
        ↓
Call Polly
        ↓
Save MP3
        ↓
Verify file
        ↓
Set audioPath
        ↓
Update manifest
        ↓
Continue
```

Critically, update metadata **only after a successful audio generation**.

---

# Task 1 — Extend flashcard schema

Have Codex first make V3 possible without actually adding audio.

### Implementation

Add:

```kotlin
val audioPath: String? = null
```

to `Flashcard`.

Update JSON parsing accordingly.

Add test cases covering:

```text
card with audioPath
card without audioPath
```

Existing V1/V2 vocabulary must continue parsing.

### Acceptance criteria

```text
[ ] Existing vocabulary still loads.
[ ] audioPath is optional.
[ ] Missing audio does not cause parsing errors.
[ ] Tests pass.
[ ] ./gradlew assembleDebug passes.
```

---

# Task 2 — Add audio playback abstraction

Do **not** immediately put audio APIs inside the Composable.

Introduce something like:

```kotlin
interface PronunciationPlayer {
    suspend fun play(assetPath: String)
    fun stop()
}
```

Then:

```kotlin
class AssetPronunciationPlayer(
    private val context: Context
) : PronunciationPlayer
```

This gives you separation between:

```text
UI
 ↓
ViewModel
 ↓
PronunciationPlayer
 ↓
Android audio API
```

rather than:

```text
Composable → MediaPlayer
```

For tiny MP3 vocabulary clips, you don't need an elaborate media architecture.

Use the simplest reliable Android mechanism that handles assets cleanly.

### Acceptance criteria

```text
[ ] Audio logic does not live inside Composables.
[ ] Player can play an MP3 from app assets.
[ ] Previous playback stops before another starts.
[ ] Resources are released correctly.
[ ] Playback failures are surfaced.
```

---

# Task 3 — Add pronunciation UI

Now activate the 🔊 button.

Behavior:

```text
audioPath != null
    ↓
enabled

audioPath == null
    ↓
disabled
```

On press:

```text
🔊
 ↓
ViewModel.playPronunciation(card)
 ↓
PronunciationPlayer.play(...)
```

Optionally show a short playing state:

```text
🔊 normal

🔊 playing
```

but don't overbuild this yet.

### Acceptance criteria

```text
[ ] Audio button appears on flashcard back.
[ ] Audio button is disabled when audioPath is absent.
[ ] Audio button plays local pronunciation.
[ ] Rapid repeated presses do not create overlapping audio.
[ ] Navigating away stops playback if appropriate.
```

---

# Task 4 — Create the Polly generator

This is the main harness-side implementation.

I'd make the script accept:

```bash
python tools/tts/generate_tts.py
```

All missing cards.

And:

```bash
python tools/tts/generate_tts.py --section animals
```

Only one section.

And:

```bash
python tools/tts/generate_tts.py --card dog
```

One card.

Potential future:

```bash
python tools/tts/generate_tts.py --force dog
```

Explicit regeneration.

## Generation rules

Input should be the actual Mandarin:

```python
text = card["hanzi"]
```

not the English:

```python
# NO
text = card["english"]
```

and not normally the Pinyin:

```python
# NO
text = card["pinyin"]
```

Polly gets:

```text
苹果
```

and synthesizes the Mandarin pronunciation.

---

# Task 5 — Deterministic filenames

Do not derive filenames from Chinese text.

Use stable card IDs:

```text
dog.mp3
cat.mp3
apple.mp3
```

rather than:

```text
狗.mp3
猫.mp3
苹果.mp3
```

This avoids potential filesystem, tooling, URL, and normalization headaches.

So:

```python
filename = f"{card['id']}.mp3"
```

produces:

```text
app/src/main/assets/audio/dog.mp3
```

and:

```json
"audioPath": "audio/dog.mp3"
```

---

# Task 6 — Make generation idempotent

This is one of the most important tasks.

Normal run:

```text
audioPath exists
+
file exists
       ↓
SKIP
```

Missing:

```text
no audioPath
       ↓
GENERATE
```

Broken metadata:

```text
audioPath exists
but file doesn't
       ↓
WARN / regenerate
```

Never silently overwrite existing audio.

Require:

```bash
--force
```

for explicit regeneration.

That means you can repeatedly tell your coding harness:

> Generate TTS for the vocabulary.

without paying to regenerate all 2,000 words.

---

# Task 7 — Add a generation manifest

I'd strongly recommend this.

Example:

```json
{
  "dog": {
    "hanzi": "狗",
    "file": "audio/dog.mp3",
    "provider": "amazon-polly",
    "voice": "Zhiyu",
    "language": "cmn-CN",
    "format": "mp3"
  }
}
```

Eventually you can add:

```json
{
  "dog": {
    "hanzi": "狗",
    "file": "audio/dog.mp3",
    "provider": "amazon-polly",
    "voice": "Zhiyu",
    "engine": "neural",
    "language": "cmn-CN",
    "format": "mp3",
    "contentHash": "..."
  }
}
```

The manifest answers:

> How was this audio generated?

without bloating your learner-facing vocabulary JSON.

---

# Task 8 — Content hashing

This is a particularly useful improvement for a harness.

Generate a hash based on the synthesis inputs:

```text
狗
+
voice
+
engine
+
language
```

For example:

```python
hash_input = f"{hanzi}|{voice}|{engine}|{language}"
```

Then store:

```json
"contentHash": "9f21..."
```

Suppose you change:

```text
狗
```

to:

```text
小狗
```

but retain the same card ID.

The harness detects:

```text
stored hash != current hash
```

and knows the audio is stale.

That's much better than relying only on whether `dog.mp3` exists.

---

# Task 9 — Polly configuration

Keep Polly configuration outside source code.

For example:

```text
POLLY_REGION
POLLY_VOICE
POLLY_ENGINE
```

or command-line options:

```bash
python tools/tts/generate_tts.py \
    --voice Zhiyu \
    --engine neural
```

AWS credentials should come from the normal environment/credential chain used by the coding harness.

Never put:

```python
aws_access_key = "..."
aws_secret = "..."
```

inside the repository.

---

# Task 10 — Handle polyphonic characters

This is the one Mandarin-specific issue worth designing for from the start.

The harness should synthesize:

```text
银行
```

not:

```text
行
```

because the pronunciation can depend on context.

Therefore the default synthesis source should always be:

```text
complete card.hanzi
```

This is another reason your card should represent a **word or expression**, rather than necessarily one character.

Later, if Polly gets a particular pronunciation wrong, support an override:

```json
{
  "id": "bank",
  "hanzi": "银行",
  "pinyin": "yínháng",
  "english": "bank",
  "tts": {
    "textOverride": "银行"
  }
}
```

Eventually you could support SSML overrides too:

```json
"tts": {
  "ssml": "..."
}
```

but I would **not add this in the first V3 implementation**.

Only add override capability after you encounter an actual problem.

---

# Task 11 — Add validation tooling

Create:

```bash
python tools/tts/validate_tts.py
```

It should check:

```text
Every audioPath points to a real file.

Every audio file belongs to a known card.

No duplicate audio paths.

MP3 files are non-empty.

Manifest entries correspond to vocabulary entries.

Cards without audio are reported.

Stale hashes are reported.
```

Example:

```text
TTS Validation

Vocabulary cards:       250
Cards with audio:       247
Missing audio:            3
Broken references:        0
Orphaned MP3s:            1
Stale generations:        0

Missing:
- transportation/train
- food/noodles
- verbs/wait
```

That will be extremely useful once the vocabulary library grows.

---

# Task 12 — Harness skill

This is where I'd turn the whole process into a Codex skill.

Something like:

```text
.skills/
└── generate-mandarin-tts/
    └── SKILL.md
```

Its workflow should effectively be:

```text
1. Read vocabulary.
2. Identify entries missing valid TTS.
3. Do not overwrite valid existing audio.
4. Generate Mandarin using Amazon Polly.
5. Save to the expected asset directory.
6. Update audioPath.
7. Update the generation manifest.
8. Run TTS validation.
9. Run Android tests/build.
10. Report generated/skipped/failed entries.
```

Then eventually you can tell Codex:

> Add this vocabulary section and generate its pronunciation.

and your harness knows the entire pipeline.

---

# Recommended Codex task sequence

I would feed these to Codex **one at a time**, roughly in this order:

```text
V3.1
Extend Flashcard with optional audioPath and update parsing/tests.

V3.2
Create PronunciationPlayer abstraction and local-asset implementation.

V3.3
Wire the flashcard audio button to PronunciationPlayer.

V3.4
Create tools/tts/generate_tts.py using Amazon Polly.

V3.5
Add section/card filtering and --force behavior.

V3.6
Add deterministic asset naming and vocabulary JSON updates.

V3.7
Add tts-manifest.json and content hashing.

V3.8
Create validate_tts.py.

V3.9
Generate pronunciation for the existing vocabulary library.

V3.10
Perform end-to-end Android playback validation.

V3.11
Document V3 architecture.

V3.12
Create the generate-mandarin-tts Codex skill.
```

## Definition of done for V3

I'd consider V3 complete when:

```text
[ ] Flashcards can optionally reference local pronunciation audio.

[ ] 🔊 plays the Mandarin pronunciation.

[ ] Cards without audio continue working normally.

[ ] Android contains no AWS credentials.

[ ] Android does not depend on Polly or network connectivity.

[ ] Coding harness can generate missing TTS automatically.

[ ] Existing audio is not regenerated unnecessarily.

[ ] Changing synthesis input marks audio as stale.

[ ] Generator can target all vocabulary, a section, or one card.

[ ] TTS assets are validated automatically.

[ ] Android tests pass.

[ ] Debug APK builds.

[ ] docs/ARCHITECTURE.md describes the TTS system.
```

The result is a nice separation of responsibilities:

```text
CONTENT BUILD TIME

Codex / harness
      ↓
Amazon Polly
      ↓
MP3 assets
      ↓
vocabulary.json


APP RUNTIME

vocabulary.json
      ↓
Flashcard
      ↓
local MP3
      ↓
🔊
```

That architecture will also match V2 nicely: **your harness generates expensive/AI-derived content ahead of time, while the Android application remains a lightweight consumer of finished assets.**
