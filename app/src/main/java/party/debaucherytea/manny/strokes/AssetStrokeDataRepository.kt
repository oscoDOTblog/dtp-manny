package party.debaucherytea.manny.strokes

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * A [StrokeDataRepository] reading `assets/strokes/U+XXXX.json` files.
 * Only the application context is retained, so the repository may outlive
 * any single Activity.
 *
 * A character without an asset is a normal unsupported state reported as
 * null. Unparseable assets surface as exceptions, distinguishable from
 * missing data.
 */
class AssetStrokeDataRepository(context: Context) : StrokeDataRepository {
    private val assets = context.applicationContext.assets

    private val mutex = Mutex()
    private val cache = mutableMapOf<String, HanziStrokeData?>()

    override suspend fun load(character: String): HanziStrokeData? {
        // Only a single code point can name an asset; anything else simply
        // has no data rather than being an error.
        if (character.codePointCount(0, character.length) != 1) return null
        mutex.withLock {
            if (cache.containsKey(character)) return cache.getValue(character)
        }
        val loaded = withContext(Dispatchers.IO) {
            try {
                assets.open("strokes/${strokeAssetName(character)}")
                    .bufferedReader(Charsets.UTF_8)
                    .use { parseStrokeData(it.readText()) }
            } catch (_: FileNotFoundException) {
                null
            }
        }
        mutex.withLock {
            // A newer load() may have populated the entry while this one was
            // reading; keep the first completed result either way.
            if (!cache.containsKey(character)) cache[character] = loaded
            return cache.getValue(character)
        }
    }
}

/** Deterministic asset name for a character, e.g. U+72D7.json for 狗. */
internal fun strokeAssetName(character: String): String =
    "U+%04X.json".format(java.util.Locale.US, character.codePointAt(0))
