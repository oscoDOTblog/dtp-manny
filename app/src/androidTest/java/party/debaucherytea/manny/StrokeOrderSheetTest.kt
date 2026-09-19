package party.debaucherytea.manny

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import party.debaucherytea.manny.strokes.HanziStrokeData
import party.debaucherytea.manny.strokes.StrokeDataRepository
import party.debaucherytea.manny.strokes.StrokePoint
import party.debaucherytea.manny.ui.StrokeOrderSheet
import party.debaucherytea.manny.ui.theme.MannyTheme

private class FakeStrokeRepository(val data: Map<String, HanziStrokeData>) : StrokeDataRepository {
    override suspend fun load(character: String): HanziStrokeData? = data[character]
}

private fun syntheticData(character: String, strokes: Int) = HanziStrokeData(
    character,
    List(strokes) { "M 0 100 L 900 100" },
    List(strokes) { listOf(StrokePoint(0f, 100f), StrokePoint(900f, 100f)) }
)

@RunWith(AndroidJUnit4::class)
class StrokeOrderSheetTest {
    @get:Rule val compose = createComposeRule()

    private fun show(repository: FakeStrokeRepository, vararg characters: String = arrayOf("测")) {
        compose.setContent {
            MannyTheme { StrokeOrderSheet(characters.toList(), repository, onDismiss = {}) }
        }
    }

    @Test fun showsTitleProgressAndDiagram() {
        show(FakeStrokeRepository(mapOf("测" to syntheticData("测", 3))))
        compose.onNodeWithText("Stroke order").assertIsDisplayed()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("1 / 3").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Stroke diagram for 测").assertIsDisplayed()
    }

    @Test fun advancesAutomatically() {
        show(FakeStrokeRepository(mapOf("测" to syntheticData("测", 3))))
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("2 / 3").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun pauseAndResume() {
        show(FakeStrokeRepository(mapOf("测" to syntheticData("测", 3))))
        compose.onNodeWithText("Pause").performClick()
        compose.onNodeWithText("Play").assertIsDisplayed()
        compose.onNodeWithText("Play").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("3 / 3").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun restartReturnsToFirstStroke() {
        show(FakeStrokeRepository(mapOf("测" to syntheticData("测", 3))))
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("2 / 3").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Reset").performClick()
        compose.onNodeWithText("1 / 3").assertIsDisplayed()
    }

    @Test fun multiCharacterSelectorSwitchesAnimations() {
        show(
            FakeStrokeRepository(mapOf(
                "苹" to syntheticData("苹", 2),
                "果" to syntheticData("果", 3)
            )),
            "苹", "果"
        )
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("1 / 2").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("果").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("1 / 3").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Stroke diagram for 果").assertIsDisplayed()
    }

    @Test fun selectorCharacterWithoutDataShowsMissing() {
        show(FakeStrokeRepository(mapOf("苹" to syntheticData("苹", 2))), "苹", "火")
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("1 / 2").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("火").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("No stroke data for this character.").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun missingDataShowsGracefulState() {
        show(FakeStrokeRepository(emptyMap()))
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("No stroke data for this character.").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
