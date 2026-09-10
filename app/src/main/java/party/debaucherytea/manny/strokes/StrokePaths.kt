package party.debaucherytea.manny.strokes

import android.graphics.Matrix
import android.graphics.Path
import androidx.core.graphics.PathParser

/** Hanzi Writer stroke data lives in a 900x900 SVG-like coordinate space. */
const val STROKE_SPACE = 900f

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
