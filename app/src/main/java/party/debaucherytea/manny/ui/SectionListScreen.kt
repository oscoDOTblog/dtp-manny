package party.debaucherytea.manny.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import party.debaucherytea.manny.R
import party.debaucherytea.manny.data.FlashcardSection

@Composable
fun SectionListScreen(sections: List<FlashcardSection>, onSectionClick: (FlashcardSection) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(40.dp))
            Text(stringResource(R.string.library_heading), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.library_subtitle), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.library_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (sections.isEmpty()) item { Text(stringResource(R.string.empty_sections)) }
        items(sections, key = { it.id }) { section ->
            Card(onClick = { onSectionClick(section) }, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp)) {
                    Text(section.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.card_count, section.cards.size), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.offline_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
