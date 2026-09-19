package party.debaucherytea.manny.audio

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A [PronunciationPlayer] backed by a single [MediaPlayer] reading MP3
 * clips from app assets. Only the application context is retained, so the
 * player may outlive any single Activity.
 */
class AssetPronunciationPlayer(context: Context) : PronunciationPlayer {
    private val assets = context.applicationContext.assets

    private val lock = Any()
    private var active: MediaPlayer? = null

    override suspend fun play(assetPath: String) {
        require(assetPath.isNotBlank()) { "Missing assetPath" }
        // A new clip always replaces the previous one; clips never overlap.
        stop()
        val fresh = MediaPlayer()
        try {
            withContext(Dispatchers.IO) {
                assets.openFd(assetPath).use { descriptor ->
                    fresh.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                    fresh.prepare()
                }
            }
        } catch (error: CancellationException) {
            fresh.release()
            throw error
        } catch (error: Exception) {
            fresh.release()
            throw error
        }
        val superseded = synchronized(lock) {
            val previous = active
            fresh.setOnCompletionListener { finished ->
                finished.release()
                synchronized(lock) {
                    if (active === finished) active = null
                }
            }
            active = fresh
            previous
        }
        superseded?.let(::releaseQuietly)
        if (synchronized(lock) { active !== fresh }) {
            // A newer play() has taken over and already released this clip.
            return
        }
        try {
            fresh.start()
        } catch (_: IllegalStateException) {
            // Released by a newer play() racing start(); that clip owns playback now.
        }
    }

    override fun stop() {
        val current = synchronized(lock) {
            active.also { active = null }
        } ?: return
        releaseQuietly(current)
    }

    private fun releaseQuietly(player: MediaPlayer) {
        try {
            player.stop()
        } catch (_: IllegalStateException) {
            // Already idle or released; still released below.
        }
        player.release()
    }

    /** Releases any held playback resources. */
    fun close() = stop()
}
