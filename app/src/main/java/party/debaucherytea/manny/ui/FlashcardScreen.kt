package party.debaucherytea.manny.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import party.debaucherytea.manny.R
import party.debaucherytea.manny.data.FlashcardSection

@Composable
fun FlashcardScreen(section: FlashcardSection?, onBack: () -> Unit) {
    var index by rememberSaveable(section?.id) { mutableIntStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text(stringResource(R.string.back)) }
        Text(section?.title.orEmpty(), style = MaterialTheme.typography.headlineMedium)
        if (section == null || section.cards.isEmpty()) {
            Text(stringResource(R.string.empty_cards))
        } else {
            val safeIndex = index.coerceIn(section.cards.indices)
            Text(stringResource(R.string.position, safeIndex + 1, section.cards.size), style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = { (safeIndex + 1).toFloat() / section.cards.size }, modifier = Modifier.fillMaxWidth())
            // A new card always begins on its image side with the meaning hidden.
            key(section.id, safeIndex) { FlashcardFace(section.cards[safeIndex]) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { index = safeIndex - 1 }, enabled = safeIndex > 0, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.previous))
                }
                Button(onClick = { index = safeIndex + 1 }, enabled = safeIndex < section.cards.lastIndex, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.next))
                }
            }
        }
    }
}
