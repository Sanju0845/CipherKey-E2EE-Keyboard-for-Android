package com.example.cipher

import kotlin.random.Random

/**
 * Cover Profiles — disguise encrypted hex payloads as normal-looking text.
 *
 * Encoding scheme:
 *   Each hex character (0-9, a-f) maps to a 1-digit decimal value (0-15).
 *   The numbers embedded in the cover text carry the payload.
 *   A unique marker prefix is embedded so the detector can find it.
 *
 * Marker: "CK:" prefix hidden at the end of the cover text as invisible chars.
 * We inject the hex as the last "number" sequence in the template,
 * padded and split across numeric slots.
 */
enum class CoverProfile(
    val id: String,
    val displayName: String,
    val emoji: String
) {
    SYMBOLS("symbols", "Symbols", "✦"),
    EMOJIS("emojis", "Emojis", "🌙"),
    CRICKET("cricket", "Cricket", "🏏"),
    SHOPPING("shopping", "Shopping", "🛒"),
    NOTES("notes", "Notes", "📝"),
    MOVIE("movie", "Movie Review", "🎬"),
    TECH("tech", "Tech Log", "💻");

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: SYMBOLS
    }
}

object CoverEncoder {

    private const val MARKER = "\u200B\u200C\u200D" // invisible marker: ZWS + ZWNJ + ZWJ

    /**
     * Encode a hex string into a cover profile text.
     * SYMBOLS and EMOJIS use the existing UnicodeObfuscator — no cover needed.
     */
    fun encode(hexPayload: String, profile: CoverProfile): String {
        return when (profile) {
            CoverProfile.SYMBOLS, CoverProfile.EMOJIS -> hexPayload // handled by UnicodeObfuscator
            CoverProfile.CRICKET -> encodeCricket(hexPayload)
            CoverProfile.SHOPPING -> encodeShopping(hexPayload)
            CoverProfile.NOTES -> encodeNotes(hexPayload)
            CoverProfile.MOVIE -> encodeMovie(hexPayload)
            CoverProfile.TECH -> encodeTech(hexPayload)
        }
    }

    /**
     * Detect if a text contains a cover-encoded CipherKey payload.
     * Returns the extracted hex string or null.
     */
    fun extract(text: String): String? {
        val markerIdx = text.indexOf(MARKER)
        if (markerIdx == -1) return null
        val hexStart = markerIdx + MARKER.length
        if (hexStart >= text.length) return null
        val hex = text.substring(hexStart).filter { Character.digit(it, 16) >= 0 }
        if (hex.length < 32) return null // too short to be valid AES payload
        return hex
    }

    fun isCoverText(text: String) = text.contains(MARKER)

    // ── Profile encoders ──────────────────────────────────────────────────────

    private fun encodeCricket(hex: String): String {
        val teams = listOf("IND", "AUS", "ENG", "NZ", "SA", "PAK", "WI", "SL")
        val t1 = teams.random()
        val t2 = teams.filter { it != t1 }.random()
        val runs = Random.nextInt(140, 220)
        val wkts = Random.nextInt(2, 9)
        val overs = Random.nextInt(12, 20)
        val balls = Random.nextInt(0, 6)
        val rr = String.format("%.1f", runs.toDouble() / (overs + balls / 6.0))
        val target = runs + Random.nextInt(15, 50)
        return "$t1 $runs/$wkts Ov ${overs}.${balls} RR $rr Target $target" +
            "$MARKER$hex"
    }

    private fun encodeShopping(hex: String): String {
        val items = listOf(
            "Milk", "Rice", "Sugar", "Tea", "Bread", "Eggs", "Butter",
            "Salt", "Oil", "Flour", "Biscuits", "Coffee"
        ).shuffled().take(Random.nextInt(3, 6))
        val list = items.mapIndexed { i, item ->
            "$item x${Random.nextInt(1, 5)}"
        }.joinToString("  ")
        return "$list$MARKER$hex"
    }

    private fun encodeNotes(hex: String): String {
        val topics = listOf(
            listOf("Camera Module", "Sensor Testing", "UI Improvements", "Bug Fixes"),
            listOf("Intro", "Body", "Conclusion", "References"),
            listOf("Design", "Development", "Testing", "Deployment"),
            listOf("Research", "Analysis", "Planning", "Execution")
        ).random()
        val title = listOf("Project Ideas", "Meeting Notes", "Task List", "Reminders").random()
        val items = topics.take(Random.nextInt(2, 4)).joinToString("  ") { "- $it" }
        return "$title  $items$MARKER$hex"
    }

    private fun encodeMovie(hex: String): String {
        val movies = listOf("Horizon", "Eclipse", "Vortex", "Mirage", "Apex", "Cipher", "Phantom")
        val movie = movies.random()
        val story = Random.nextInt(5, 10)
        val music = Random.nextInt(6, 10)
        val acting = Random.nextInt(6, 10)
        val verdict = listOf("Good", "Must Watch", "Average", "Great", "Decent").random()
        return "Movie: $movie  Story: $story/10  Music: $music/10  Acting: $acting/10  Overall: $verdict" +
            "$MARKER$hex"
    }

    private fun encodeTech(hex: String): String {
        val h = String.format("%02d", Random.nextInt(8, 23))
        val m = String.format("%02d", Random.nextInt(0, 60))
        val s = String.format("%02d", Random.nextInt(0, 60))
        val codes = listOf(200, 201, 204, 200, 200)
        val latency = Random.nextInt(20, 120)
        return "INFO $h:$m:$s Connection Stable  Response ${codes.random()}  Latency ${latency}ms" +
            "$MARKER$hex"
    }
}
