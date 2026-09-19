package party.debaucherytea.manny.strokes

/**
 * Unique Hanzi code points in [text], in first-appearance order. Mirrors the
 * harness extraction rule: CJK Unified Ideographs plus Extension A;
 * punctuation, Latin, digits, and other symbols are ignored, so entries like
 * "人（亻）" yield 人 and 亻 while "T恤" yields only 恤.
 */
fun extractHanzi(text: String): List<String> =
    text.filter { it in '一'..'鿿' || it in '㐀'..'䶿' }
        .toList()
        .distinct()
        .map(Char::toString)
