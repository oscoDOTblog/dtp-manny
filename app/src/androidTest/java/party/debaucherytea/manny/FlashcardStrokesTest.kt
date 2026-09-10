package party.debaucherytea.manny

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import party.debaucherytea.manny.audio.PronunciationPlayer
import party.debaucherytea.manny.data.Flashcard
import party.debaucherytea.manny.data.FlashcardSection
import party.debaucherytea.manny.strokes.HanziStrokeData
import party.debaucherytea.manny.strokes.StrokeDataRepository
import party.debaucherytea.manny.strokes.StrokePoint
import party.debaucherytea.manny.ui.FlashcardScreen
import party.debaucherytea.manny.ui.theme.MannyTheme

private class SilentPlayer : PronunciationPlayer {
    override suspend fun play(assetPath: String) = Unit
    override fun stop() = Unit
}

private class MapStrokes(val data: Map<String, HanziStrokeData>) : StrokeDataRepository {
    override suspend fun load(character: String): HanziStrokeData? = data[character]
}

private fun strokeData(character: String, strokes: Int) = HanziStrokeData(
    character,
    List(strokes) { "M 0 100 L 900 100" },
    List(strokes) { listOf(StrokePoint(0f, 100f), StrokePoint(900f, 100f)) }
)

@RunWith(AndroidJUnit4::class)
class FlashcardStrokesTest {
    @get:Rule val compose = createComposeRule()

    private fun show(card: Flashcard, strokes: StrokeDataRepository) {
        compose.setContent {
            MannyTheme {
                FlashcardScreen(
                    FlashcardSection("s", "S", listOf(card)),
                    SilentPlayer(),
                    strokes,
                    onBack = {}
                )
            }
        }
    }

    private fun flip() {
        compose.onNodeWithText("Tap to reveal").performClick()
    }

    @Test fun strokesButtonOpensSheetWhenSupported() {
        show(Flashcard("dog", "s", "狗", "gǒu", "dog"), MapStrokes(mapOf("狗" to strokeData("狗", 8))))
        flip()
        compose.waitUntil(5_000) {
            try {
                compose.onNodeWithContentDescription("Show stroke order").assertIsEnabled()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNodeWithContentDescription("Show stroke order").performClick()
        compose.onNodeWithText("Stroke order").assertExists()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Stroke diagram for 狗").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun strokesButtonDisabledWithoutData() {
        show(Flashcard("cat", "s", "猫", "māo", "cat"), MapStrokes(emptyMap()))
        flip()
        compose.onNodeWithContentDescription("Show stroke order").assertIsNotEnabled()
    }

    @Test fun partialSupportEnablesAndSelectsCharacters() {
        show(
            Flashcard("apple", "s", "苹果", "píngguǒ", "apple"),
            MapStrokes(mapOf("苹" to strokeData("苹", 2)))
        )
        flip()
        compose.waitUntil(5_000) {
            try {
                compose.onNodeWithContentDescription("Show stroke order").assertIsEnabled()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNodeWithContentDescription("Show stroke order").performClick()
        compose.onNodeWithText("苹").assertExists()
        compose.onNodeWithText("果").assertExists()
        compose.onNodeWithText("果").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("No stroke data for this character.").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
