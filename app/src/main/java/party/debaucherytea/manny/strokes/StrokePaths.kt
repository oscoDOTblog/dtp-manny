package party.debaucherytea.manny.strokes

import android.graphics.Matrix
import android.graphics.Path
import androidx.core.graphics.PathParser

/** Hanzi Writer stroke data lives in a 900x900 SVG-like coordinate space. */
const val STROKE_SPACE = 900f

/** Width of the animated centerline trace, in stroke-space units. */
const val STROKE_TRACE_WIDTH = 32f

/**
 * Traces the first [progress] fraction of a stroke's median centerline into
 * [out] (rewound first, so callers can reuse one Path across frames).
 *
 * Unlike the filled outline paths, the median runs strictly from the
 * stroke's start point to its end point, so the trace grows in one
 * direction and never doubles back.
 */
fun traceMedian(points: List<StrokePoint>, progress: Float, out: Path) {
    out.rewind()
    if (points.isEmpty()) return
    out.moveTo(points[0].x, points[0].y)
    if (points.size == 1) {
        // Degenerate median: a round-capped zero-length line renders as a dot.
        if (progress > 0f) out.lineTo(points[0].x, points[0].y)
        return
    }
    var remaining = progress.coerceIn(0f, 1f) * points.totalLength()
    var index = 1
    while (index < points.size && remaining > 0f) {
        val previous = points[index - 1]
        val current = points[index]
        val segment = current.distanceTo(previous)
        if (remaining >= segment) {
            out.lineTo(current.x, current.y)
            remaining -= segment
        } else {
            val fraction = remaining / segment
            out.lineTo(
                previous.x + (current.x - previous.x) * fraction,
                previous.y + (current.y - previous.y) * fraction
            )
            remaining = 0f
        }
        index++
    }
}

private fun List<StrokePoint>.totalLength(): Float {
    var total = 0f
    for (index in 1 until size) total += this[index].distanceTo(this[index - 1])
    return total
}

private fun StrokePoint.distanceTo(other: StrokePoint): Float {
    val dx = x - other.x
    val dy = y - other.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

/**
 * Parses one Hanzi Writer SVG stroke path into an Android [Path].
 *
 * @throws IllegalArgumentException if the path data is malformed or empty.
 */
fun parseStrokePath(pathData: String): Path {
    val path = try {
        PathParser.createPathFromPathData(pathData)
    } catch (error: Exception) {
        throw IllegalArgumentException("Unparseable stroke path: $pathData", error)
    } ?: throw IllegalArgumentException("Unparseable stroke path: $pathData")
    require(!path.isEmpty) { "Empty stroke path: $pathData" }
    return path
}

/** Parses every stroke of [data] in source order. */
fun parseStrokePaths(data: HanziStrokeData): List<Path> = data.strokes.map(::parseStrokePath)

/**
 * Scales the 900x900 stroke space into a [width] by [height] area,
 * preserving aspect ratio and centering the character.
 *
 * Upstream coordinates are y-up (origin bottom-left, as in font data) while
 * the canvas is y-down, so the transform also flips vertically -- the same
 * scale(1, -1) flip Hanzi Writer itself applies when rendering.
 */
fun strokeTransform(width: Float, height: Float): Matrix {
    val scale = minOf(width, height) / STROKE_SPACE
    return Matrix().apply {
        setScale(scale, -scale)
        postTranslate(
            (width - STROKE_SPACE * scale) / 2f,
            (height - STROKE_SPACE * scale) / 2f + STROKE_SPACE * scale
        )
    }
}
