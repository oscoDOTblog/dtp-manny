package party.debaucherytea.manny.ui

import android.graphics.PathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import party.debaucherytea.manny.R
import party.debaucherytea.manny.strokes.HanziStrokeData
import party.debaucherytea.manny.strokes.StrokeDataRepository
import party.debaucherytea.manny.strokes.parseStrokePaths

private sealed interface SheetContent {
    data object Loading : SheetContent
    data object Missing : SheetContent
    data class Ready(val data: HanziStrokeData) : SheetContent
}

/**
 * Bottom sheet animating stroke order for [characters]. A single character
 * opens straight into its animation; multiple characters get a selector and
 * each selection loads independently. Loading shows a spinner and a
 * character without data shows a graceful missing state instead of breaking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrokeOrderSheet(characters: List<String>, repository: StrokeDataRepository, onDismiss: () -> Unit) {
    require(characters.isNotEmpty()) { "StrokeOrderSheet needs at least one character" }
    var selected by remember(characters) { mutableIntStateOf(0) }
    val character = characters[selected.coerceIn(characters.indices)]
    val content by produceState<SheetContent>(SheetContent.Loading, character, repository) {
        value = repository.load(character)?.let(SheetContent::Ready) ?: SheetContent.Missing
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.stroke_order_title), style = MaterialTheme.typography.headlineSmall)
            if (characters.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    characters.forEachIndexed { index, choice ->
                        FilterChip(
                            selected = index == selected,
                            onClick = { selected = index },
                            label = { Text(choice) }
                        )
                    }
                }
            }
            when (val current = content) {
                SheetContent.Loading -> CircularProgressIndicator()
                SheetContent.Missing -> Text(
                    stringResource(R.string.stroke_missing),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                is SheetContent.Ready -> StrokeAnimation(current.data)
            }
        }
    }
}

/**
 * Draws [data] stroke by stroke: completed strokes appear fully while the
 * current one is revealed along its path, as if being written. Playback
 * starts automatically, advances through every stroke, and can be
 * paused/resumed or restarted.
 */
@Composable
private fun StrokeAnimation(data: HanziStrokeData) {
    val paths = remember(data) { parseStrokePaths(data) }
    val lengths = remember(data) {
        paths.map { path -> PathMeasure().apply { setPath(path, false) }.length }
    }
    var current by remember(data) { mutableIntStateOf(0) }
    var playing by remember(data) { mutableStateOf(true) }
    var runId by remember(data) { mutableIntStateOf(0) }
    val progress = remember(data) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(data, current, playing, runId) {
        if (!playing) return@LaunchedEffect
        if (progress.value >= 1f) progress.snapTo(0f)
        progress.animateTo(1f, tween(strokeDurationMs(lengths[current]), easing = LinearEasing))
        if (current < paths.lastIndex) current++ else playing = false
    }
    fun restart() {
        scope.launch {
            progress.stop()
            progress.snapTo(0f)
        }
        current = 0
        playing = true
        runId++
    }

    HanziStrokeView(
        data = data,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        shownStrokes = current,
        currentProgress = progress.value
    )
    Text(
        stringResource(R.string.position, current + 1, paths.size),
        style = MaterialTheme.typography.labelLarge
    )
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = ::restart) {
            Text(stringResource(R.string.stroke_reset))
        }
        Button(onClick = {
            if (playing) {
                playing = false
            } else {
                if (current == paths.lastIndex && progress.value >= 1f) restart() else playing = true
            }
        }) {
            Text(stringResource(if (playing) R.string.stroke_pause else R.string.stroke_play))
        }
    }
}

private fun strokeDurationMs(length: Float): Int =
    (400 + length / 3).toInt().coerceIn(400, 1200)
