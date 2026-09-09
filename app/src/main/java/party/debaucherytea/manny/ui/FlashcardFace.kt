package party.debaucherytea.manny.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import party.debaucherytea.manny.R
import party.debaucherytea.manny.data.Flashcard

@Composable
fun FlashcardFace(card: Flashcard) {
    var flipped by rememberSaveable(card.id) { mutableStateOf(false) }
    var showInfo by rememberSaveable(card.id) { mutableStateOf(false) }
    val angle by animateFloatAsState(if (flipped) 180f else 0f, tween(360), label = "cardFlip")
    val backVisible = angle > 90f
    val flipLabel = stringResource(R.string.flip)
    Card(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).graphicsLayer {
            rotationY = angle
            cameraDistance = 12 * density
        }.clickable(role = Role.Button, onClickLabel = flipLabel) {
            flipped = !flipped
            showInfo = false
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 340.dp).graphicsLayer {
                // Counter-rotate the back so Hanzi never appears mirrored.
                rotationY = if (backVisible) 180f else 0f
            }.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            if (!backVisible) {
                Image(painterResource(R.drawable.flashcard_placeholder), stringResource(R.string.placeholder_description), Modifier.size(200.dp))
                Text(stringResource(R.string.recall_prompt), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(stringResource(R.string.front_hint), style = MaterialTheme.typography.bodySmall)
            } else {
                Text(card.hanzi, fontSize = if (card.hanzi.length > 2) 48.sp else 88.sp, lineHeight = 100.sp, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showInfo = !showInfo }) {
                        Text(stringResource(if (showInfo) R.string.hide_info else R.string.show_info))
                    }
                    val audioDescription = stringResource(R.string.audio_unavailable)
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.semantics { contentDescription = audioDescription }) {
                        Text(stringResource(R.string.audio))
                    }
                }
                if (showInfo) {
                    Text(card.pinyin, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Text(card.english, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
                Text(stringResource(R.string.back_hint), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        }
    }
}
