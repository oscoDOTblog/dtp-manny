package party.debaucherytea.manny.data

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class Flashcard(
    val id: String,
    val sectionId: String,
    val hanzi: String,
    val pinyin: String,
    val english: String,
    val audioPath: String? = null
)
data class FlashcardSection(val id: String, val title: String, val cards: List<Flashcard>)

class FlashcardRepository(private val assets: AssetManager) {
    suspend fun loadSections(): List<FlashcardSection> = withContext(Dispatchers.IO) {
        assets.open("vocabulary.json").bufferedReader(Charsets.UTF_8).use { parseSections(it.readText()) }
    }
}

fun parseSections(json: String): List<FlashcardSection> {
    val sections = JSONArray(json)
    val sectionIds = mutableSetOf<String>()
    val cardIds = mutableSetOf<String>()
    return List(sections.length()) { index ->
        val section = sections.getJSONObject(index)
        val id = section.requiredText("id")
        require(sectionIds.add(id)) { "Duplicate section: $id" }
        val cards = section.getJSONArray("cards")
        FlashcardSection(id, section.requiredText("title"), List(cards.length()) { cardIndex ->
            val card = cards.getJSONObject(cardIndex)
            val cardId = card.requiredText("id")
            require(cardIds.add(cardId)) { "Duplicate card: $cardId" }
            require(card.requiredText("sectionId") == id) { "Card section mismatch: $cardId" }
            Flashcard(
                cardId,
                id,
                card.requiredText("hanzi"),
                card.requiredText("pinyin"),
                card.requiredText("english"),
                card.optionalText("audioPath")
            )
        })
    }
}

private fun JSONObject.requiredText(key: String): String {
    val value = get(key)
    require(value is String && value.isNotBlank()) { "Missing or invalid $key" }
    return value
}

private fun JSONObject.optionalText(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val value = get(key)
    require(value is String && value.isNotBlank()) { "Missing or invalid $key" }
    return value
}
