package party.debaucherytea.manny.audio

/**
 * Plays short pronunciation clips for flashcards.
 *
 * Implementations must stop any current playback before starting another so
 * clips never overlap, and must surface playback failures to the caller
 * rather than failing silently. Calls are expected to be sequential, as
 * issued from UI event handlers.
 */
interface PronunciationPlayer {
    /**
     * Stops any current playback, then plays the MP3 at [assetPath]
     * (relative to the app assets directory, e.g. `"audio/dog.mp3"`).
     *
     * @throws IllegalArgumentException if [assetPath] is blank.
     * @throws java.io.IOException if the asset is missing or cannot be played.
     */
    suspend fun play(assetPath: String)

    /** Stops playback if a clip is playing. Safe to call when idle. */
    fun stop()
}
