package party.debaucherytea.manny

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import party.debaucherytea.manny.data.*

class VocabularyTest {
    @Test fun bundledVocabularyPreservesSourceAndLoadsAllSections() = runBlocking {
        val sections = FlashcardRepository(InstrumentationRegistry.getInstrumentation().targetContext.assets).loadSections()
        assertEquals(listOf(40, 10, 3), sections.map { it.cards.size })
        assertEquals(53, sections.flatMap { it.cards }.map { it.id }.toSet().size)
        assertEquals("口", sections[0].cards[4].hanzi)
        assertEquals("囗", sections[0].cards[5].hanzi)
        assertEquals("糸（纟）", sections[0].cards[27].hanzi)
        assertEquals("mì", sections[0].cards[27].pinyin)
        assertEquals("十", sections[1].cards.last().hanzi)
        assertEquals(listOf("gǒu", "māo", "niǎo"), sections[2].cards.map { it.pinyin })
        assertTrue(sections.flatMap { it.cards }.all { it.audioPath == null })
    }

    @Test fun audioPathIsOptional() {
        val withAudio = """{"id":"dog","sectionId":"animals","hanzi":"狗","pinyin":"gǒu","english":"dog","audioPath":"audio/dog.mp3"}"""
        val withoutAudio = """{"id":"cat","sectionId":"animals","hanzi":"猫","pinyin":"māo","english":"cat"}"""
        val json = """[{"id":"animals","title":"Animals","cards":[$withAudio,$withoutAudio]}]"""
        val cards = parseSections(json).single().cards
        assertEquals("audio/dog.mp3", cards[0].audioPath)
        assertNull(cards[1].audioPath)
    }

    @Test fun rejectsBlankAudioPath() {
        val card = """{"id":"dog","sectionId":"animals","hanzi":"狗","pinyin":"gǒu","english":"dog","audioPath":" "}"""
        val json = """[{"id":"animals","title":"Animals","cards":[$card]}]"""
        assertThrows(IllegalArgumentException::class.java) { parseSections(json) }
    }

    @Test fun emptyCollectionsAreSupported() {
        assertTrue(parseSections("[]").isEmpty())
        assertTrue(parseSections("""[{"id":"empty","title":"Empty","cards":[]}]""").single().cards.isEmpty())
    }

    @Test fun rejectsDuplicateIdsAndMismatchedSections() {
        val card = """{"id":"a","sectionId":"s","hanzi":"狗","pinyin":"gǒu","english":"dog"}"""
        val json = """[{"id":"s","title":"Test","cards":[$card]}]"""
        assertThrows(IllegalArgumentException::class.java) { parseSections(json.replace("[$card]", "[$card,$card]")) }
        assertThrows(IllegalArgumentException::class.java) { parseSections(json.replace("\"sectionId\":\"s\"", "\"sectionId\":\"other\"")) }
        assertThrows(IllegalArgumentException::class.java) { parseSections(json.replace("gǒu", " ")) }
        assertThrows(org.json.JSONException::class.java) { parseSections("not json") }
    }
}
