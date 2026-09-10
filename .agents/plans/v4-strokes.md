For V4, I’d keep the same philosophy as V3: **prepare structured assets ahead of time, keep the Android runtime simple, and make the feature work fully offline**.

The best source is still Hanzi Writer / Make Me a Hanzi data. It provides ordered stroke vector data for 9,000+ simplified and traditional characters, and Hanzi Writer’s data package is explicitly usable offline for mobile apps. ([GitHub][1])

## V4 goal

Add a stroke-order animation button on the back of each flashcard.

```text
Back

┌────────────────────────────┐
│                            │
│             狗             │
│                            │
│      ⓘ     🔊     ✍️       │
│                            │
└────────────────────────────┘
```

Tap `✍️`:

```text
┌────────────────────────────┐
│          Stroke order      │
│                            │
│            狗              │
│       animated drawing     │
│                            │
│         3 / 8 strokes      │
│                            │
│        ↻    ▶/⏸    ❯       │
└────────────────────────────┘
```

For multi-character vocabulary like:

```text
苹果
银行
出租车
```

the user should choose or cycle through the individual characters rather than trying to animate the entire word as one continuous glyph.

---

# Recommended V4 architecture

I would not call an external API at runtime.

Instead:

```text
Make Me a Hanzi / Hanzi Writer data
                 ↓
          coding harness
                 ↓
     extract characters used
                 ↓
     normalize stroke JSON
                 ↓
app/src/main/assets/strokes/
                 ↓
       Android repository
                 ↓
    Compose StrokeOrderView
```

That means your harness does for V4 what Polly did for V3.

## Data source

Use **Hanzi Writer Data** as the canonical input.

Its underlying data contains ordered SVG stroke paths and median paths, derived from Make Me a Hanzi. Hanzi Writer itself uses this data for both animation and writing quizzes. ([GitHub][2])

One important licensing note: Hanzi Writer code is MIT, but the character data has separate licensing under the Arphic Public License, so include the correct attribution/license notices in your app or OSS acknowledgements. ([GitHub][2])

---

# V4 data model

I would **not** add a `strokePath` field to every flashcard.

Stroke data belongs to characters, not vocabulary cards.

Instead create a separate character-data layer:

```kotlin
data class HanziStrokeData(
    val character: String,
    val strokes: List<String>,
    val medians: List<List<StrokePoint>>
)

data class StrokePoint(
    val x: Float,
    val y: Float
)
```

Then:

```text
Flashcard
  hanzi = "苹果"

        ↓

extract characters

        ↓

"苹"
"果"

        ↓

StrokeDataRepository
```

This avoids duplicating the same stroke data across vocabulary.

For example:

```text
水果
苹果
果汁
水果店
```

may all reuse:

```text
果.json
```

---

# Suggested repository structure

```text
mandarin-flashcards/
│
├── docs/
│   ├── ARCHITECTURE.md
│   ├── PRODUCT.md
│   └── ROADMAP.md
│
├── tools/
│   └── strokes/
│       ├── import_strokes.py
│       ├── validate_strokes.py
│       └── README.md
│
├── content/
│   └── vocabulary.json
│
└── app/
    └── src/main/
        └── assets/
            └── strokes/
                ├── 狗.json
                ├── 猫.json
                ├── 苹.json
                └── 果.json
```

You could also use Unicode filenames if you want, but I slightly prefer code points:

```text
U+72D7.json
U+732B.json
U+82F9.json
U+679C.json
```

Then your lookup is deterministic and filesystem-safe.

---

# Task V4.1 — Define stroke models

Create the Android models first.

```kotlin
data class HanziStrokeData(
    val character: String,
    val strokes: List<String>,
    val medians: List<List<StrokePoint>>
)
```

Add a repository interface:

```kotlin
interface StrokeDataRepository {
    suspend fun load(character: String): HanziStrokeData?
}
```

Acceptance:

```text
[ ] Stroke data is separate from Flashcard.
[ ] Character lookup is deterministic.
[ ] Missing characters return null cleanly.
[ ] Unit tests cover valid and missing data.
```

---

# Task V4.2 — Normalize source data with the harness

I would not make Android understand the raw upstream package directly if you can avoid it.

Have the harness convert upstream Hanzi Writer data into a schema you own.

For example:

```json
{
  "character": "狗",
  "strokes": [
    "M ...",
    "M ..."
  ],
  "medians": [
    [[123, 456], [130, 462]],
    [[201, 502], [210, 511]]
  ]
}
```

That gives you insulation if upstream changes later.

Harness flow:

```text
vocabulary.json
      ↓
extract unique Hanzi
      ↓
lookup Hanzi Writer data
      ↓
normalize
      ↓
save one JSON per character
```

Acceptance:

```text
[ ] Only characters used by vocabulary are imported.
[ ] Duplicate characters are imported once.
[ ] Missing source data is reported.
[ ] Existing files are not rewritten unnecessarily.
[ ] Generated JSON matches a stable schema.
```

---

# Task V4.3 — Character extraction

The harness should scan all vocabulary entries.

Example:

```text
狗
苹果
银行
```

becomes:

```text
狗
苹
果
银
行
```

Do not treat punctuation, spaces, digits, or Latin characters as Hanzi.

This matters later for entries like:

```text
T恤
3号线
你好！
```

Acceptance:

```text
[ ] Unique Hanzi extraction works.
[ ] Multi-character words are split into characters.
[ ] Non-Hanzi symbols are ignored.
[ ] Duplicate Hanzi are deduplicated.
```

---

# Task V4.4 — Build `StrokeDataRepository`

Android loads:

```text
assets/strokes/U+72D7.json
```

and parses it into:

```kotlin
HanziStrokeData
```

Cache results in memory so repeated viewing doesn't reparse the same JSON.

Conceptually:

```kotlin
class AssetStrokeDataRepository(
    private val context: Context
) : StrokeDataRepository {

    private val cache = mutableMapOf<String, HanziStrokeData>()

    override suspend fun load(character: String): HanziStrokeData? {
        // Load/cache asset.
    }
}
```

Acceptance:

```text
[ ] Assets load off the main thread.
[ ] Loaded data is cached.
[ ] Missing asset is a normal unsupported state.
[ ] Parsing failures are distinguishable from missing data.
```

---

# Task V4.5 — SVG path parsing

This is probably the most technically important Android task.

Hanzi Writer supplies SVG-style path strings. Android Compose Canvas wants Android/Compose paths.

Create a conversion layer:

```text
SVG path string
      ↓
PathParser
      ↓
Android Path
      ↓
Compose rendering
```

AndroidX provides path parsing utilities, so I would use platform/AndroidX support before introducing a third-party SVG engine.

Do **not** build your own SVG parser unless necessary.

Acceptance:

```text
[ ] Common Hanzi Writer paths parse successfully.
[ ] Paths render at the correct proportions.
[ ] Coordinate transforms are isolated in one component.
[ ] Malformed paths fail gracefully.
```

---

# Task V4.6 — Static character renderer

Before animation, render all strokes simultaneously.

Build:

```kotlin
@Composable
fun HanziStrokeView(
    strokeData: HanziStrokeData,
    modifier: Modifier = Modifier
)
```

First milestone:

```text
stroke data
   ↓
Compose Canvas
   ↓
recognizable 狗
```

Don't proceed to animation until this is correct.

Acceptance:

```text
[ ] Character is visually correct.
[ ] Character is centered.
[ ] Aspect ratio is preserved.
[ ] Works across multiple screen sizes.
[ ] No clipping.
```

---

# Task V4.7 — Stroke-by-stroke display

Before smooth animation, implement discrete progression:

```text
stroke 1
   ↓
stroke 1 + 2
   ↓
stroke 1 + 2 + 3
```

Expose:

```kotlin
currentStrokeIndex
```

and render:

```text
completed strokes
+
current stroke
```

Controls:

```text
Previous
Next
Reset
```

Acceptance:

```text
[ ] Stroke order matches source order.
[ ] Previous/Next works.
[ ] Reset returns to stroke 1.
[ ] Progress shows `n / total`.
```

---

# Task V4.8 — Animate the current stroke

Then add animation.

The important visual distinction is:

```text
completed strokes
      ↓
drawn fully

current stroke
      ↓
revealed gradually
```

Use Compose animation APIs and a progress value:

```kotlin
0f → 1f
```

You may find that the `medians` are useful for guiding the animation direction, while stroke outlines provide the final glyph appearance.

Do not just fade a whole stroke in. It should visually read as **being drawn**.

Acceptance:

```text
[ ] Stroke grows in writing direction.
[ ] Completed strokes remain visible.
[ ] Animation can pause/resume.
[ ] Animation can restart.
[ ] Animation progresses automatically to the next stroke.
```

---

# Task V4.9 — Stroke-order screen/modal

I would make this a modal/bottom-sheet/full-screen overlay rather than squeezing animation directly into the flashcard.

For example:

```text
Flashcard
   ↓ ✍️
StrokeOrderSheet
```

For a single-character card:

```text
狗
```

open it immediately.

For:

```text
苹果
```

show:

```text
[ 苹 ] [ 果 ]
```

with the first character selected.

Acceptance:

```text
[ ] Single-character cards open immediately.
[ ] Multi-character cards expose character selector.
[ ] Selecting character loads its animation.
[ ] Missing stroke data displays gracefully.
```

---

# Task V4.10 — Wire the button to availability

Ideally don't show an active button if none of the characters have stroke data.

Possible states:

```text
all characters supported
→ enabled

some supported
→ enabled

none supported
→ disabled
```

The user doesn't need to know why an unsupported obscure character has no file unless they tap somewhere explanatory.

---

# Task V4.11 — Build validation script

Create:

```bash
python tools/strokes/validate_strokes.py
```

Report:

```text
Vocabulary entries:         520
Unique Hanzi:               743
Stroke assets available:    739
Missing:                      4
Invalid files:                0
Unused stroke assets:        12
```

Validate:

```text
JSON parses
character field matches filename
at least one stroke
medians/strokes counts make sense
no zero-byte assets
no duplicate filenames
```

---

# Task V4.12 — Make import idempotent

Exactly like V3.

Running:

```bash
python tools/strokes/import_strokes.py
```

repeatedly should produce:

```text
✓ existing / unchanged
✓ existing / unchanged
+ imported
! missing
```

not rewrite every file.

You can store upstream/source metadata if useful:

```json
{
  "source": "hanzi-writer-data",
  "character": "狗",
  "sourceHash": "..."
}
```

---

# Task V4.13 — Add licensing/attribution

Because the stroke dataset's license differs from Hanzi Writer's MIT code, explicitly add attribution.

Something like:

```text
THIRD_PARTY_NOTICES.md
```

and potentially an About/Open Source Licenses screen later.

The dataset licensing distinction is documented by both Hanzi Writer and the Hanzi Writer Data repository. ([GitHub][2])

---

# Task V4.14 — Performance polish

Once it works:

```text
[ ] Cache parsed stroke data.
[ ] Cache parsed Paths if beneficial.
[ ] Don't parse JSON in Composables.
[ ] Don't reconstruct paths every animation frame.
[ ] Preserve animation state only where useful.
[ ] Stop animation when sheet closes.
```

A few hundred tiny JSON assets shouldn't be an issue, especially if you're only bundling characters actually used.

---

# Task V4.15 — Harness skill

I'd make this another reusable Codex skill:

```text
generate-hanzi-strokes/
└── SKILL.md
```

Workflow:

```text
1. Scan vocabulary.
2. Extract unique Hanzi.
3. Compare against existing stroke assets.
4. Fetch/read configured Hanzi Writer source data.
5. Normalize missing characters.
6. Preserve existing valid assets.
7. Validate the dataset.
8. Run Android tests.
9. Build debug APK.
10. Report missing/unsupported Hanzi.
```

Then after adding vocabulary you can eventually say:

```text
Add the Transportation section,
generate its images,
generate Polly pronunciation,
and sync stroke-order assets.
```

and the harness executes all three content pipelines.

---

## Recommended Codex sequence

I would give Codex these as separate tasks:

```text
V4.1  Add stroke data models and repository interface.

V4.2  Build the harness importer for Hanzi Writer data.

V4.3  Add unique-Hanzi extraction from vocabulary.

V4.4  Generate stroke assets for existing vocabulary.

V4.5  Implement AssetStrokeDataRepository.

V4.6  Parse and render static SVG stroke paths in Compose.

V4.7  Add discrete stroke progression.

V4.8  Implement animated stroke drawing.

V4.9  Build StrokeOrderSheet with playback controls.

V4.10 Add multi-character selector.

V4.11 Wire flashcard ✍️ button and availability state.

V4.12 Create stroke-data validation tooling.

V4.13 Add third-party licensing/attribution.

V4.14 Optimize parsing/rendering and add tests.

V4.15 Update PRODUCT.md, ARCHITECTURE.md, and ROADMAP.md.

V4.16 Create the generate-hanzi-strokes skill.
```

## Definition of done

```text
[ ] Flashcard backs have a stroke-order control.

[ ] A character can be animated stroke by stroke.

[ ] Animation follows correct stroke order.

[ ] Multi-character words let the learner switch characters.

[ ] Stroke data works offline.

[ ] No runtime API is required.

[ ] Stroke assets are generated/imported by the coding harness.

[ ] Only required Hanzi assets are bundled.

[ ] Missing stroke data does not break flashcards.

[ ] Import is idempotent.

[ ] Stroke assets can be validated automatically.

[ ] Licensing attribution is included.

[ ] Unit tests pass.

[ ] Debug APK builds.

[ ] Architecture docs describe the V4 pipeline.
```

The larger architecture is becoming quite clean:

```text
                 CONTENT HARNESS

Vocabulary
    │
    ├── V2 → image generator → WebP
    │
    ├── V3 → Amazon Polly ───→ MP3
    │
    └── V4 → Hanzi dataset ──→ stroke JSON
                              
                 ↓

            ANDROID APP

        completely local assets
              │
     ┌────────┼─────────┐
     ↓        ↓         ↓
   Image     Audio    Stroke
   recall     🔊        ✍️
```

That separation is probably worth preserving as you add future learning modes.

[1]: https://github.com/skishore/makemeahanzi?utm_source=chatgpt.com "GitHub - skishore/makemeahanzi: Free, open-source Chinese character data · GitHub"
[2]: https://github.com/chanind/hanzi-writer/blob/master/README.md?utm_source=chatgpt.com "hanzi-writer/README.md at master · chanind/hanzi-writer · GitHub"
