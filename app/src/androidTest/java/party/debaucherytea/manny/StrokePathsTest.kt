package party.debaucherytea.manny

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import party.debaucherytea.manny.strokes.StrokePoint
import party.debaucherytea.manny.strokes.parseStrokeData
import party.debaucherytea.manny.strokes.parseStrokePath
import party.debaucherytea.manny.strokes.parseStrokePaths
import party.debaucherytea.manny.strokes.strokeTransform
import party.debaucherytea.manny.strokes.traceMedian

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
        assertMaps(matrix, 0f, 0f, 0f, 300f)
        assertMaps(matrix, 900f, 900f, 300f, 0f)
        assertMaps(matrix, 450f, 450f, 150f, 150f)
    }

    @Test fun transformFlipsUpstreamYUpToCanvasYDown() {
        // A top-starting upstream stroke (high y, e.g. 狗's first median at
        // y ~767) must land near the top of the canvas (small y).
        val matrix = strokeTransform(300f, 300f)
        assertMaps(matrix, 362f, 767f, 362f / 3f, (900f - 767f) / 3f)
    }

    @Test fun transformPreservesAspectOnWideCanvas() {
        val matrix = strokeTransform(600f, 300f)
        assertMaps(matrix, 0f, 0f, 150f, 300f)
        assertMaps(matrix, 900f, 900f, 450f, 0f)
    }

    private fun assertMaps(matrix: android.graphics.Matrix, x: Float, y: Float, ex: Float, ey: Float) {
        val point = floatArrayOf(x, y)
        matrix.mapPoints(point)
        assertEquals(ex, point[0], 0.01f)
        assertEquals(ey, point[1], 0.01f)
    }

    @Test fun traceMedianGrowsInOneDirection() {
        val points = listOf(StrokePoint(0f, 0f), StrokePoint(900f, 0f))
        val out = android.graphics.Path()
        val measure = android.graphics.PathMeasure()

        traceMedian(points, 0f, out)
        measure.setPath(out, false)
        assertEquals(0f, measure.length, 0.01f)

        traceMedian(points, 0.5f, out)
        measure.setPath(out, false)
        assertEquals(450f, measure.length, 0.5f)
        val end = FloatArray(2)
        measure.getPosTan(measure.length, end, null)
        assertEquals(450f, end[0], 0.5f)
        assertEquals(0f, end[1], 0.5f)

        traceMedian(points, 1f, out)
        measure.setPath(out, false)
        assertEquals(900f, measure.length, 0.5f)
    }

    @Test fun traceMedianHandlesDegenerateInput() {
        val out = android.graphics.Path()
        val measure = android.graphics.PathMeasure()
        traceMedian(emptyList(), 1f, out)
        measure.setPath(out, false)
        assertEquals(0f, measure.length, 0.01f)
    }
}
