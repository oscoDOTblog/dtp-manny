package party.debaucherytea.manny

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlashcardAppTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun sectionFlipInfoNavigationAndRestoration() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Animals").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Animals").performScrollTo().performClick()
        compose.onNodeWithText("1 / 3").assertExists()
        compose.onNodeWithText("Previous").assertIsNotEnabled()
        compose.onNodeWithText("gǒu").assertDoesNotExist()
        compose.onNodeWithText("Tap to reveal").performClick()
        compose.onNodeWithText("狗").assertExists()
        compose.onNodeWithText("gǒu").assertDoesNotExist()
        compose.onNodeWithContentDescription("Audio unavailable").assertIsNotEnabled()
        compose.onNodeWithText("Show meaning").performClick()
        compose.onNodeWithText("gǒu").assertExists()
        compose.onNodeWithText("dog").assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("gǒu").assertExists()
        compose.onNodeWithText("Hide meaning").performClick()
        compose.onNodeWithText("dog").assertDoesNotExist()
        compose.onNodeWithText("Next").performScrollTo().performClick()
        compose.onNodeWithText("2 / 3").assertExists()
        compose.onNodeWithText("Tap to reveal").assertExists()
        compose.onNodeWithText("Next").performScrollTo().performClick()
        compose.onNodeWithText("3 / 3").assertExists()
        compose.onNodeWithText("Next").assertIsNotEnabled()
        compose.onNodeWithText("Previous").performClick()
        compose.onNodeWithText("2 / 3").assertExists()
        compose.onNodeWithText("Back to sections").performScrollTo().performClick()
        compose.onNodeWithText("Chinese radicals").assertExists()
    }
}
