package party.debaucherytea.manny.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import party.debaucherytea.manny.R

/**
 * Discrete stroke-order controls: reveals strokes 1..[current] of [total].
 * Previous steps back to (but never before) stroke 1; Reset returns to
 * stroke 1 from anywhere. Stateless: the owner holds [current] and pairs
 * this with [HanziStrokeView] via its `shownStrokes` parameter.
 */
@Composable
fun StrokeProgressionControls(
    current: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onPrevious, enabled = current > 1, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.previous))
            }
            Text(
                stringResource(R.string.position, current.coerceIn(1, total), total),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onNext, enabled = current < total, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.next))
            }
        }
        TextButton(onClick = onReset, enabled = current != 1) {
            Text(stringResource(R.string.stroke_reset))
        }
    }
}
