package party.debaucherytea.manny

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import party.debaucherytea.manny.strokes.parseStrokeData
import party.debaucherytea.manny.strokes.parseStrokePath
import party.debaucherytea.manny.strokes.parseStrokePaths
import party.debaucherytea.manny.strokes.strokeTransform

class StrokePathsTest {
    private fun bundledDog() =
        InstrumentationRegistry.getInstrumentation().targetContext.assets
            .open("strokes/U+72D7.json").bufferedReader(Charsets.UTF_8)
            .use { parseStrokeData(it.readText()) }

    @Test fun realStrokePathsParseInOrder() {
        val paths = parseStrokePaths(bundledDog())
        assertEquals(8, paths.size)
        assertTrue(paths.all { !it.isEmpty })
    }

    @Test fun simplePathParses() {
        assertFalse(parseStrokePath("M 0 0 L 900 900").isEmpty)
    }

    @Test fun malformedPathsFail() {
        assertThrows(IllegalArgumentException::class.java) { parseStrokePath("M banana") }
        assertThrows(IllegalArgumentException::class.java) { parseStrokePath("") }
        assertThrows(IllegalArgumentException::class.java) { parseStrokePath("   ") }
    }

    @Test fun transformCentersSquareCanvas() {
        val matrix = strokeTransform(300f, 300f)
        assertMaps(matrix, 0f, 0f, 0f, 0f)
        assertMaps(matrix, 900f, 900f, 300f, 300f)
        assertMaps(matrix, 450f, 450f, 150f, 150f)
    }

    @Test fun transformPreservesAspectOnWideCanvas() {
        val matrix = strokeTransform(600f, 300f)
        assertMaps(matrix, 0f, 0f, 150f, 0f)
        assertMaps(matrix, 900f, 900f, 450f, 300f)
    }

    private fun assertMaps(matrix: android.graphics.Matrix, x: Float, y: Float, ex: Float, ey: Float) {
        val point = floatArrayOf(x, y)
        matrix.mapPoints(point)
        assertEquals(ex, point[0], 0.01f)
        assertEquals(ey, point[1], 0.01f)
    }
}
