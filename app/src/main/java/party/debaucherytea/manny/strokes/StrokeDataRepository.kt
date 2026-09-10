package party.debaucherytea.manny.strokes

/**
 * Loads per-character stroke data.
 *
 * Lookup is deterministic: one character maps to exactly one asset, and a
 * character without data is a normal unsupported state reported as null --
 * never an exception. Parsing failures are distinct from missing data and
 * surface as exceptions.
 */
interface StrokeDataRepository {
    suspend fun load(character: String): HanziStrokeData?
}
