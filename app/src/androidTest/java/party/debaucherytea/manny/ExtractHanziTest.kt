package party.debaucherytea.manny

import org.junit.Assert.*
import org.junit.Test
import party.debaucherytea.manny.strokes.extractHanzi

class ExtractHanziTest {
    @Test fun splitsWordsAndDeduplicates() {
        assertEquals(listOf("苹", "果", "银", "行"), extractHanzi("苹果银行苹果"))
    }

    @Test fun keepsParenthesizedVariants() {
        assertEquals(listOf("人", "亻"), extractHanzi("人（亻）"))
    }

    @Test fun ignoresNonHanzi() {
        assertEquals(listOf("恤", "号", "线"), extractHanzi("T恤3号线！"))
    }

    @Test fun emptyAndHanziFree() {
        assertTrue(extractHanzi("").isEmpty())
        assertTrue(extractHanzi("abc 123").isEmpty())
    }
}
