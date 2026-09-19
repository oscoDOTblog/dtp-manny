package party.debaucherytea.manny

import org.junit.Assert.*
import org.junit.Test
import party.debaucherytea.manny.strokes.parseStrokeData

class StrokeDataTest {
    private val valid = """
        {
          "character": "狗",
          "strokes": ["M 10 20 L 30 40", "M 50 60 L 70 80"],
          "medians": [
            [[10, 20], [30, 40]],
            [[50, 60], [70, 80]]
          ]
        }
    """.trimIndent()

    @Test fun validDataParses() {
        val data = parseStrokeData(valid)
        assertEquals("狗", data.character)
        assertEquals(listOf("M 10 20 L 30 40", "M 50 60 L 70 80"), data.strokes)
        assertEquals(2, data.medians.size)
        assertEquals(10f, data.medians[0][0].x)
        assertEquals(40f, data.medians[0][1].y)
    }

    @Test fun rejectsMissingAndMultiCharacter() {
        assertThrows(IllegalArgumentException::class.java) {
            parseStrokeData("""{"strokes": ["M 0 0 L 1 1"], "medians": [[[0, 0], [1, 1]]]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseStrokeData(valid.replace(""""character": "狗"""", """"character": "狗狗""""))
        }
    }

    @Test fun rejectsEmptyStrokes() {
        assertThrows(IllegalArgumentException::class.java) {
            parseStrokeData(valid.replace(
                """"strokes": ["M 10 20 L 30 40", "M 50 60 L 70 80"]""",
                """"strokes": []"""
            ))
        }
    }

    @Test fun rejectsMedianCountMismatch() {
        assertThrows(IllegalArgumentException::class.java) {
            parseStrokeData(valid.replace(",\n    [[50, 60], [70, 80]]", ""))
        }
    }

    @Test fun rejectsMalformedPoints() {
        assertThrows(IllegalArgumentException::class.java) {
            parseStrokeData(valid.replace("[10, 20]", "[10]"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseStrokeData(valid.replace("[10, 20]", """["x", 20]"""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseStrokeData("""{"character": "狗", "strokes": ["M 0 0 L 1 1"], "medians": [[]]}""")
        }
    }

    @Test fun rejectsInvalidJson() {
        assertThrows(org.json.JSONException::class.java) { parseStrokeData("not json") }
    }
}
