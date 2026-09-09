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
    val cardCount = section?.cards?.size ?: 0
    var index by rememberSaveable(section?.id) { mutableIntStateOf(0) }
    var order by rememberSaveable(section?.id) { mutableStateOf((0 until cardCount).toList()) }
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
            // Fall back to the natural order if the saved order no longer matches the section.
            val effectiveOrder = if (order.size == section.cards.size) order else section.cards.indices.toList()
            val safeIndex = index.coerceIn(section.cards.indices)
            val card = section.cards[effectiveOrder[safeIndex]]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.position, safeIndex + 1, section.cards.size), style = MaterialTheme.typography.labelLarge)
                TextButton(
                    onClick = {
                        order = section.cards.indices.toList().shuffled()
                        index = 0
                    },
                    enabled = section.cards.size > 1
                ) {
                    Text(stringResource(R.string.shuffle))
                }
            }
            LinearProgressIndicator(progress = { (safeIndex + 1).toFloat() / section.cards.size }, modifier = Modifier.fillMaxWidth())
            // A new card always begins on its image side with the meaning hidden.
            key(card.id) { FlashcardFace(card) }
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
