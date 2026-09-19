package party.debaucherytea.manny.strokes

import org.json.JSONArray
import org.json.JSONObject

data class StrokePoint(val x: Float, val y: Float)

data class HanziStrokeData(
    val character: String,
    val strokes: List<String>,
    val medians: List<List<StrokePoint>>
)

/**
 * Parses harness-normalized stroke JSON (see tools/strokes/README.md) into
 * [HanziStrokeData].
 *
 * @throws IllegalArgumentException if the schema is violated: the character
 *   must be a single code point, strokes must be non-empty SVG path strings,
 *   and medians must provide one non-empty point list per stroke.
 */
fun parseStrokeData(json: String): HanziStrokeData {
    val root = JSONObject(json)
    val character = root.optString("character", "")
    require(character.codePointCount(0, character.length) == 1) {
        "Stroke data must describe exactly one character"
    }
    val strokes = root.reqStringList("strokes")
    require(strokes.isNotEmpty()) { "Stroke data for $character has no strokes" }
    val medians = root.reqMedians("medians")
    require(medians.size == strokes.size) {
        "Stroke data for $character has ${strokes.size} strokes but ${medians.size} medians"
    }
    return HanziStrokeData(character, strokes, medians)
}

private fun JSONObject.reqStringList(key: String): List<String> {
    val array = optJSONArray(key) ?: throw IllegalArgumentException("Missing $key")
    return List(array.length()) { index ->
        val value = array.optString(index, "")
        require(value.isNotBlank()) { "Missing or invalid $key[$index]" }
        value
    }
}

private fun JSONObject.reqMedians(key: String): List<List<StrokePoint>> {
    val array = optJSONArray(key) ?: throw IllegalArgumentException("Missing $key")
    return List(array.length()) { index ->
        val points = array.optJSONArray(index) ?: throw IllegalArgumentException("Missing $key[$index]")
        require(points.length() > 0) { "Empty $key[$index]" }
        List(points.length()) { pointIndex ->
            (points.optJSONArray(pointIndex) ?: throw IllegalArgumentException("Invalid $key[$index][$pointIndex]"))
                .toStrokePoint("$key[$index][$pointIndex]")
        }
    }
}

private fun JSONArray.toStrokePoint(label: String): StrokePoint {
    require(length() == 2) { "Invalid $label" }
    val x = optDouble(0, Double.NaN)
    val y = optDouble(1, Double.NaN)
    require(!x.isBadCoordinate() && !y.isBadCoordinate()) { "Invalid $label" }
    return StrokePoint(x.toFloat(), y.toFloat())
}

private fun Double.isBadCoordinate(): Boolean =
    java.lang.Double.isNaN(this) || java.lang.Double.isInfinite(this)
