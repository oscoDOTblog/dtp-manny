package party.debaucherytea.manny.ui

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import party.debaucherytea.manny.R
import party.debaucherytea.manny.strokes.HanziStrokeData
import party.debaucherytea.manny.strokes.parseStrokePaths
import party.debaucherytea.manny.strokes.strokeTransform

/**
 * Renders the first [shownStrokes] of [data] simultaneously (all of them by
 * default). When [currentProgress] is provided, the next stroke is revealed
 * progressively, as if being drawn. Strokes are filled outlines drawn in the
 * theme's on-surface color; paths are parsed once per [data], never per frame.
 *
 * The caller supplies sizing (e.g. a square aspect); the character is
 * centered with its aspect ratio preserved.
 */
@Composable
fun HanziStrokeView(
    data: HanziStrokeData,
    modifier: Modifier = Modifier,
    shownStrokes: Int = data.strokes.size,
    currentProgress: Float? = null
) {
    val paths = remember(data) { parseStrokePaths(data) }
    val fullCount = shownStrokes.coerceIn(0, paths.size)
    val visible = paths.take(fullCount)
    val paint = remember { Paint().apply { style = Paint.Style.FILL; isAntiAlias = true } }
    // Reused every frame so animation never allocates paths.
    val measure = remember { PathMeasure() }
    val segment = remember { Path() }
    val glyphColor = MaterialTheme.colorScheme.onSurface
    val description = stringResource(R.string.stroke_diagram_description, data.character)
    Canvas(modifier.semantics { contentDescription = description }) {
        paint.color = glyphColor.toArgb()
        with(drawContext.canvas.nativeCanvas) {
            save()
            concat(strokeTransform(size.width, size.height))
            visible.forEach { drawPath(it, paint) }
            if (currentProgress != null && fullCount < paths.size) {
                segment.rewind()
                measure.setPath(paths[fullCount], false)
                measure.getSegment(0f, measure.length * currentProgress.coerceIn(0f, 1f), segment, true)
                drawPath(segment, paint)
            }
            restore()
        }
    }
}
