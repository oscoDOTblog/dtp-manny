package party.debaucherytea.manny

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import party.debaucherytea.manny.audio.PronunciationPlayer
import party.debaucherytea.manny.data.Flashcard
import party.debaucherytea.manny.data.FlashcardSection
import party.debaucherytea.manny.strokes.HanziStrokeData
import party.debaucherytea.manny.strokes.StrokeDataRepository
import party.debaucherytea.manny.ui.FlashcardScreen
import party.debaucherytea.manny.ui.theme.MannyTheme
import java.io.IOException

private object NoStrokes : StrokeDataRepository {
    override suspend fun load(character: String): HanziStrokeData? = null
}

private class FakePlayer(var fail: Boolean = false) : PronunciationPlayer {
    val played = mutableListOf<String>()
    var stops = 0

    override suspend fun play(assetPath: String) {
        played += assetPath
        if (fail) throw IOException("no audio")
    }

    override fun stop() {
        stops++
    }
}

private fun sectionWith(vararg cards: Flashcard) = FlashcardSection("animals", "Animals", cards.toList())

private val silentCard = Flashcard("cat", "animals", "猫", "māo", "cat")
private val soundingCard = Flashcard("dog", "animals", "狗", "gǒu", "dog", "audio/dog.mp3")

@RunWith(AndroidJUnit4::class)
class FlashcardAudioTest {
    @get:Rule val compose = createComposeRule()

    private fun show(player: FakePlayer, section: FlashcardSection) {
        compose.setContent { MannyTheme { FlashcardScreen(section, player, NoStrokes, onBack = {}) } }
    }

    @Test fun audioButtonIsDisabledWithoutAudioPath() {
        val player = FakePlayer()
        show(player, sectionWith(silentCard))
        compose.onNodeWithText("Tap to reveal").performClick()
        compose.onNodeWithContentDescription("Audio unavailable").assertIsNotEnabled()
        assert(player.played.isEmpty())
    }

    @Test fun audioButtonPlaysCardAudioPath() {
        val player = FakePlayer()
        show(player, sectionWith(soundingCard))
        compose.onNodeWithText("Tap to reveal").performClick()
        compose.onNodeWithContentDescription("Play pronunciation").assertIsEnabled().performClick()
        compose.waitUntil(5_000) { player.played.isNotEmpty() }
        assert(player.played == listOf("audio/dog.mp3"))
    }

    @Test fun playbackFailureShowsError() {
        val player = FakePlayer(fail = true)
        show(player, sectionWith(soundingCard))
        compose.onNodeWithText("Tap to reveal").performClick()
        compose.onNodeWithContentDescription("Play pronunciation").performClick()
        compose.onNodeWithText("Pronunciation audio is unavailable.").assertExists()
    }

    @Test fun leavingTheScreenStopsPlayback() {
        val player = FakePlayer()
        show(player, sectionWith(soundingCard))
        val stopsAfterEnter = player.stops
        compose.setContent { }
        compose.waitForIdle()
        assert(player.stops > stopsAfterEnter)
    }
}
