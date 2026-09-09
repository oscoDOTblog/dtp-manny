package party.debaucherytea.manny

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import party.debaucherytea.manny.audio.AssetPronunciationPlayer
import java.io.IOException

class PronunciationPlayerTest {
    private fun player() = AssetPronunciationPlayer(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test fun missingAssetSurfacesIOException() {
        val player = player()
        try {
            assertThrows(IOException::class.java) { runBlocking { player.play("audio/does-not-exist.mp3") } }
        } finally {
            player.close()
        }
    }

    @Test fun blankAssetPathIsRejected() {
        val player = player()
        try {
            assertThrows(IllegalArgumentException::class.java) { runBlocking { player.play("  ") } }
        } finally {
            player.close()
        }
    }

    @Test fun stopWithoutPlaybackDoesNotThrow() {
        val player = player()
        try {
            player.stop()
        } finally {
            player.close()
        }
    }
}
