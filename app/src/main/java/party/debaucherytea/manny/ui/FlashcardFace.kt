package party.debaucherytea.manny.ui

import androidx.annotation.DrawableRes
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
fun FlashcardFace(card: Flashcard, onPlayAudio: (() -> Unit)?) {
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
                Image(
                    painterResource(flashcardImageResource(card.id)),
                    stringResource(R.string.flashcard_illustration_description, card.english),
                    Modifier.size(200.dp)
                )
                Text(stringResource(R.string.recall_prompt), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(stringResource(R.string.front_hint), style = MaterialTheme.typography.bodySmall)
            } else {
                Text(card.hanzi, fontSize = if (card.hanzi.length > 2) 48.sp else 88.sp, lineHeight = 100.sp, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showInfo = !showInfo }) {
                        Text(stringResource(if (showInfo) R.string.hide_info else R.string.show_info))
                    }
                    val audioDescription = stringResource(
                        if (onPlayAudio != null) R.string.play_pronunciation else R.string.audio_unavailable
                    )
                    OutlinedButton(
                        onClick = { onPlayAudio?.invoke() },
                        enabled = onPlayAudio != null,
                        modifier = Modifier.semantics { contentDescription = audioDescription }
                    ) {
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

@DrawableRes
private fun flashcardImageResource(cardId: String): Int = when (cardId) {
    "radicals-01" -> R.drawable.flashcard_person_retro
    "radicals-02" -> R.drawable.flashcard_radicals_02_retro
    "radicals-03" -> R.drawable.flashcard_radicals_03_retro
    "radicals-04" -> R.drawable.flashcard_radicals_04_retro
    "radicals-05" -> R.drawable.flashcard_radicals_05_retro
    "radicals-06" -> R.drawable.flashcard_radicals_06_retro
    "radicals-07" -> R.drawable.flashcard_radicals_07_retro
    "radicals-08" -> R.drawable.flashcard_radicals_08_retro
    "radicals-09" -> R.drawable.flashcard_radicals_09_retro
    "radicals-10" -> R.drawable.flashcard_radicals_10_retro
    "radicals-11" -> R.drawable.flashcard_radicals_11_retro
    "radicals-12" -> R.drawable.flashcard_radicals_12_retro
    "radicals-13" -> R.drawable.flashcard_radicals_13_retro
    "radicals-14" -> R.drawable.flashcard_radicals_14_retro
    "radicals-15" -> R.drawable.flashcard_radicals_15_retro
    "radicals-16" -> R.drawable.flashcard_radicals_16_retro
    "radicals-17" -> R.drawable.flashcard_radicals_17_retro
    "radicals-18" -> R.drawable.flashcard_radicals_18_retro
    "radicals-19" -> R.drawable.flashcard_radicals_19_retro
    "radicals-20" -> R.drawable.flashcard_radicals_20_retro
    "radicals-21" -> R.drawable.flashcard_radicals_21_retro
    "radicals-22" -> R.drawable.flashcard_radicals_22_retro
    "radicals-23" -> R.drawable.flashcard_radicals_23_retro
    "radicals-24" -> R.drawable.flashcard_radicals_24_retro
    "radicals-25" -> R.drawable.flashcard_radicals_25_retro
    "radicals-26" -> R.drawable.flashcard_radicals_26_retro
    "radicals-27" -> R.drawable.flashcard_radicals_27_retro
    "radicals-28" -> R.drawable.flashcard_radicals_28_retro
    "radicals-29" -> R.drawable.flashcard_radicals_29_retro
    "radicals-30" -> R.drawable.flashcard_radicals_30_retro
    "radicals-31" -> R.drawable.flashcard_radicals_31_retro
    "radicals-32" -> R.drawable.flashcard_radicals_32_retro
    "radicals-33" -> R.drawable.flashcard_radicals_33_retro
    "radicals-34" -> R.drawable.flashcard_radicals_34_retro
    "radicals-35" -> R.drawable.flashcard_radicals_35_retro
    "radicals-36" -> R.drawable.flashcard_radicals_36_retro
    "radicals-37" -> R.drawable.flashcard_radicals_37_retro
    "radicals-38" -> R.drawable.flashcard_radicals_38_retro
    "radicals-39" -> R.drawable.flashcard_radicals_39_retro
    "radicals-40" -> R.drawable.flashcard_radicals_40_retro
    "numbers-01" -> R.drawable.flashcard_numbers_01_retro
    "numbers-02" -> R.drawable.flashcard_numbers_02_retro
    "numbers-03" -> R.drawable.flashcard_numbers_03_retro
    "numbers-04" -> R.drawable.flashcard_numbers_04_retro
    "numbers-05" -> R.drawable.flashcard_numbers_05_retro
    "numbers-06" -> R.drawable.flashcard_numbers_06_retro
    "numbers-07" -> R.drawable.flashcard_numbers_07_retro
    "numbers-08" -> R.drawable.flashcard_numbers_08_retro
    "numbers-09" -> R.drawable.flashcard_numbers_09_retro
    "numbers-10" -> R.drawable.flashcard_numbers_10_retro
    "animals-01" -> R.drawable.flashcard_animals_01_retro
    "animals-02" -> R.drawable.flashcard_animals_02_retro
    "animals-03" -> R.drawable.flashcard_animals_03_retro
    else -> R.drawable.flashcard_placeholder
}
