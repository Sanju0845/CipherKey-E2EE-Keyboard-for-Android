package com.singularitysoftware.encryptboard.cipher

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
    TECH("tech", "Tech Log", "💻"),
    TANGLISH("tanglish", "Telugu-Eng", "🗣️");

    companion object {
        fun fromId(id: String) = entries.find { it.id == id } ?: SYMBOLS
    }
}

object CoverEncoder {

    // Use zero-width chars to encode each hex nibble (0-15) invisibly
    // 0=ZWS, 1=ZWNJ, 2=ZWJ, 3=WJ, then combine pairs for more range
    private val NIBBLE_CHARS = listOf(
        '\u200B', // 0  Zero Width Space
        '\u200C', // 1  Zero Width Non-Joiner
        '\u200D', // 2  Zero Width Joiner
        '\u2060', // 3  Word Joiner
        '\u180E', // 4  Mongolian Vowel Separator
        '\uFEFF', // 5  Zero Width No-Break Space
        '\u200E', // 6  Left-to-Right Mark
        '\u200F', // 7  Right-to-Left Mark
        '\u202A', // 8  Left-to-Right Embedding
        '\u202B', // 9  Right-to-Left Embedding
        '\u202C', // a  Pop Directional Formatting
        '\u202D', // b  Left-to-Right Override
        '\u202E', // c  Right-to-Left Override
        '\u2061', // d  Function Application
        '\u2062', // e  Invisible Times
        '\u2063'  // f  Invisible Separator
    )
    // Start/end markers using chars not in the nibble set
    private val MARKER_START = '\u2064' // Invisible Plus
    private val MARKER_END   = '\u2065' // reserved

    /** Encode hex string as a fully invisible sequence of unicode chars */
    private fun encodeHexInvisible(hex: String): String {
        val sb = StringBuilder()
        sb.append(MARKER_START)
        for (ch in hex.lowercase()) {
            val nibble = Character.digit(ch, 16)
            if (nibble >= 0) sb.append(NIBBLE_CHARS[nibble])
        }
        sb.append(MARKER_END)
        return sb.toString()
    }

    /** Decode invisible sequence back to hex string */
    fun decodeInvisible(text: String): String? {
        val start = text.indexOf(MARKER_START)
        val end = text.indexOf(MARKER_END)
        if (start == -1 || end == -1 || end <= start) return null
        val payload = text.substring(start + 1, end)
        val sb = StringBuilder()
        for (ch in payload) {
            val idx = NIBBLE_CHARS.indexOf(ch)
            if (idx >= 0) sb.append(idx.toString(16))
        }
        val hex = sb.toString()
        return if (hex.length >= 32) hex else null
    }

    fun isCoverText(text: String) = text.contains(MARKER_START) && text.contains(MARKER_END)

    fun encode(hexPayload: String, profile: CoverProfile): String {
        return when (profile) {
            CoverProfile.SYMBOLS, CoverProfile.EMOJIS -> hexPayload
            CoverProfile.CRICKET  -> encodeCricket(hexPayload)
            CoverProfile.SHOPPING -> encodeShopping(hexPayload)
            CoverProfile.NOTES    -> encodeNotes(hexPayload)
            CoverProfile.MOVIE    -> encodeMovie(hexPayload)
            CoverProfile.TECH     -> encodeTech(hexPayload)
            CoverProfile.TANGLISH -> encodeTanglish(hexPayload)
        }
    }

    /** Extract hex payload from any cover text (returns null if not a cover text) */
    fun extract(text: String): String? = decodeInvisible(text)

    // ── Profile encoders — append INVISIBLE hex at end ────────────────────────

    private fun coverWrap(visibleText: String, hex: String): String =
        visibleText + encodeHexInvisible(hex)

    private fun encodeCricket(hex: String): String {
        val teams = listOf("IND", "AUS", "ENG", "NZ", "SA", "PAK", "WI", "SL")
        val t1 = teams.random()
        val runs = Random.nextInt(140, 220)
        val wkts = Random.nextInt(2, 9)
        val overs = Random.nextInt(12, 20)
        val balls = Random.nextInt(0, 6)
        val rr = String.format("%.1f", runs.toDouble() / (overs + balls / 6.0))
        val target = runs + Random.nextInt(15, 50)
        return coverWrap("$t1 $runs/$wkts Ov ${overs}.${balls} RR $rr Target $target", hex)
    }

    private fun encodeShopping(hex: String): String {
        val items = listOf("Milk", "Rice", "Sugar", "Tea", "Bread", "Eggs", "Butter", "Salt", "Oil", "Flour")
            .shuffled().take(Random.nextInt(3, 5))
            .joinToString("  ") { "$it x${Random.nextInt(1, 5)}" }
        return coverWrap(items, hex)
    }

    private fun encodeNotes(hex: String): String {
        val topics = listOf(
            listOf("Camera Module", "Sensor Testing", "UI Improvements"),
            listOf("Design", "Development", "Testing"),
            listOf("Research", "Analysis", "Planning")
        ).random()
        val title = listOf("Project Ideas", "Meeting Notes", "Task List").random()
        val items = topics.take(Random.nextInt(2, 4)).joinToString("  ") { "- $it" }
        return coverWrap("$title  $items", hex)
    }

    private fun encodeMovie(hex: String): String {
        val movies = listOf("Horizon", "Eclipse", "Vortex", "Mirage", "Apex", "Cipher", "Phantom")
        val story = Random.nextInt(5, 10)
        val music = Random.nextInt(6, 10)
        val acting = Random.nextInt(6, 10)
        val verdict = listOf("Good", "Must Watch", "Average", "Great", "Decent").random()
        return coverWrap("Movie: ${movies.random()}  Story: $story/10  Music: $music/10  Acting: $acting/10  Overall: $verdict", hex)
    }

    private fun encodeTech(hex: String): String {
        val h = String.format("%02d", Random.nextInt(8, 23))
        val m = String.format("%02d", Random.nextInt(0, 60))
        val s = String.format("%02d", Random.nextInt(0, 60))
        val latency = Random.nextInt(20, 120)
        return coverWrap("INFO $h:$m:$s Connection Stable  Response 200  Latency ${latency}ms", hex)
    }

    private fun encodeTanglish(hex: String): String {
        val openers = listOf("bro", "yaar", "da", "maccha", "ra", "anna", "boss")
        val middles = listOf(
            "nenu cheppindi correct ga undhi",
            "ikkade chuste baguntundi",
            "okka second wait chey",
            "nenu ready ga unna",
            "konchem busy ga unna",
            "chill agu tension paddaku",
            "nenu just check chestha",
            "inkevvariki cheppakku",
            "malli cheptha okay na",
            "tharvata matladu",
            "ippudu kadu tarvata",
            "nee choice bro",
            "okay okay got it",
            "are yaar no stress",
            "same pinch me too",
            "pakka confirm chestanu",
            "nenu plan chestha okay",
            "just chill mawa",
            "kalisi matladu later",
            "super idea ra nenu try chesta"
        )
        val closers = listOf("😂", "😅", "👍", "🔥", "💯", "lol", "haha", "okay ra", "no issues", "done da", "sure boss", "👀", "😎")
        return coverWrap("${openers.random()} ${middles.random()} ${closers.random()}", hex)
    }
}
