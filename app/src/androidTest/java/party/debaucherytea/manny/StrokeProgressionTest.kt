package party.debaucherytea.manny

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import party.debaucherytea.manny.ui.StrokeProgressionControls
import party.debaucherytea.manny.ui.theme.MannyTheme

@RunWith(AndroidJUnit4::class)
class StrokeProgressionTest {
    @get:Rule val compose = createComposeRule()

    private fun show(total: Int = 8) {
        compose.setContent {
            MannyTheme {
                var current by remember { mutableIntStateOf(1) }
                StrokeProgressionControls(
                    current = current,
                    total = total,
                    onPrevious = { current-- },
                    onNext = { current++ },
                    onReset = { current = 1 }
                )
            }
        }
    }

    @Test fun startsAtFirstStrokeWithBoundedPrevious() {
        show()
        compose.onNodeWithText("1 / 8").assertExists()
        compose.onNodeWithText("Previous").assertIsNotEnabled()
        compose.onNodeWithText("Next").assertIsEnabled()
        compose.onNodeWithText("Reset").assertIsNotEnabled()
    }

    @Test fun nextAndPreviousStepThroughStrokes() {
        show()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("2 / 8").assertExists()
        compose.onNodeWithText("Previous").assertIsEnabled()
        compose.onNodeWithText("Previous").performClick()
        compose.onNodeWithText("1 / 8").assertExists()
    }

    @Test fun nextStopsAtLastStroke() {
        show(total = 2)
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("2 / 2").assertExists()
        compose.onNodeWithText("Next").assertIsNotEnabled()
    }

    @Test fun resetReturnsToFirstStroke() {
        show()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("3 / 8").assertExists()
        compose.onNodeWithText("Reset").assertIsEnabled().performClick()
        compose.onNodeWithText("1 / 8").assertExists()
        compose.onNodeWithText("Reset").assertIsNotEnabled()
    }
}
