package party.debaucherytea.manny

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import party.debaucherytea.manny.strokes.HanziStrokeData
import party.debaucherytea.manny.strokes.StrokePoint
import party.debaucherytea.manny.strokes.parseStrokeData
import party.debaucherytea.manny.ui.HanziStrokeView
import party.debaucherytea.manny.ui.theme.MannyTheme

@RunWith(AndroidJUnit4::class)
class HanziStrokeViewTest {
    @get:Rule val compose = createComposeRule()

    private val horizontal = HanziStrokeData(
        "一",
        listOf("M 0 450 L 900 450"),
        listOf(listOf(StrokePoint(0f, 450f), StrokePoint(900f, 450f)))
    )

    private fun bundledDog() =
        InstrumentationRegistry.getInstrumentation().targetContext.assets
            .open("strokes/U+72D7.json").bufferedReader(Charsets.UTF_8)
            .use { parseStrokeData(it.readText()) }

    @Test fun rendersSyntheticCharacter() {
        compose.setContent { MannyTheme { HanziStrokeView(horizontal, Modifier.size(300.dp)) } }
        compose.onNodeWithContentDescription("Stroke diagram for 一").assertIsDisplayed()
    }

    @Test fun rendersBundledCharacter() {
        val dog = bundledDog()
        compose.setContent { MannyTheme { HanziStrokeView(dog, Modifier.size(300.dp)) } }
        compose.onNodeWithContentDescription("Stroke diagram for 狗").assertIsDisplayed()
    }
}
