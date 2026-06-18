package com.singularitysoftware.encryptboard.keyboard

/**
 * Lightweight local word predictor using a frequency-weighted bigram model.
 * No network, no permissions. Pure offline prediction.
 */
object WordPredictor {

    // Common English words ranked by frequency (top ~300 most useful)
    private val COMMON_WORDS = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "it",
        "for", "not", "on", "with", "he", "as", "you", "do", "at", "this",
        "but", "his", "by", "from", "they", "we", "say", "her", "she", "or",
        "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know",
        "take", "people", "into", "year", "your", "good", "some", "could",
        "them", "see", "other", "than", "then", "now", "look", "only", "come",
        "its", "over", "think", "also", "back", "after", "use", "two", "how",
        "our", "work", "first", "well", "way", "even", "new", "want", "because",
        "any", "these", "give", "day", "most", "us", "great", "between", "need",
        "large", "often", "hand", "high", "place", "hold", "turn", "here",
        "why", "help", "talk", "where", "too", "little", "world", "very",
        "still", "own", "same", "another", "off", "tell", "boy", "follow",
        "came", "show", "form", "three", "small", "set", "put", "end", "does",
        "another", "well", "large", "big", "down", "never", "start", "city",
        "play", "small", "number", "off", "always", "move", "live", "machine",
        "note", "point", "today", "together", "during", "without", "again",
        "please", "thank", "thanks", "sorry", "hello", "hey", "okay", "ok",
        "yes", "yeah", "no", "nope", "sure", "maybe", "right", "wrong",
        "love", "hate", "feel", "think", "know", "believe", "hope", "wish",
        "happy", "sad", "good", "bad", "great", "nice", "cool", "awesome",
        "really", "very", "much", "many", "more", "less", "few", "lot",
        "going", "coming", "doing", "being", "having", "getting", "making",
        "looking", "taking", "giving", "saying", "seeing", "thinking",
        "morning", "evening", "night", "today", "tomorrow", "yesterday",
        "home", "work", "school", "office", "phone", "message", "call",
        "meet", "meeting", "send", "sending", "check", "checking", "wait",
        "waiting", "ready", "done", "finished", "started", "stop", "go",
        "come", "leave", "stay", "run", "walk", "eat", "drink", "sleep",
        "wake", "watch", "listen", "read", "write", "type", "click", "open",
        "close", "save", "delete", "share", "post", "reply", "forward",
        "friend", "family", "mom", "dad", "brother", "sister", "wife",
        "husband", "baby", "kids", "everyone", "someone", "anyone", "nobody",
        "something", "anything", "nothing", "everything", "somewhere", "here",
        "there", "everywhere", "nowhere", "somehow", "anyway", "however",
        "therefore", "although", "because", "since", "while", "before",
        "after", "during", "until", "unless", "whether", "though", "yet",
        "both", "either", "neither", "each", "every", "such", "rather",
        "quite", "almost", "already", "soon", "later", "early", "late",
        "quickly", "slowly", "easily", "hard", "fast", "long", "short",
        "old", "young", "new", "next", "last", "first", "second", "third",
        "free", "busy", "tired", "excited", "worried", "confused", "sure",
        "possible", "important", "different", "same", "similar", "special",
        "beautiful", "perfect", "amazing", "wonderful", "terrible", "awful"
    )

    // Bigram follow-up suggestions: after typing word X, suggest these
    private val BIGRAMS = mapOf(
        "i" to listOf("am", "will", "have", "think", "know", "want", "need", "can", "love", "feel"),
        "you" to listOf("are", "can", "will", "have", "know", "want", "need", "should", "might", "could"),
        "we" to listOf("are", "can", "will", "have", "need", "should", "could", "might", "want", "go"),
        "they" to listOf("are", "will", "have", "can", "should", "might", "could", "want", "need", "go"),
        "it" to listOf("is", "was", "will", "can", "should", "might", "could", "has", "had", "seems"),
        "the" to listOf("best", "same", "most", "first", "last", "next", "only", "other", "new", "right"),
        "is" to listOf("a", "the", "not", "very", "so", "really", "just", "also", "still", "already"),
        "are" to listOf("you", "we", "they", "not", "very", "so", "really", "just", "still", "already"),
        "have" to listOf("a", "the", "to", "been", "not", "already", "just", "never", "always", "some"),
        "do" to listOf("you", "not", "it", "that", "this", "we", "they", "so", "the", "a"),
        "can" to listOf("you", "we", "I", "they", "it", "be", "do", "get", "see", "help"),
        "will" to listOf("be", "have", "do", "get", "go", "come", "see", "make", "take", "help"),
        "not" to listOf("be", "have", "do", "get", "go", "know", "sure", "really", "just", "yet"),
        "please" to listOf("help", "let", "send", "check", "confirm", "reply", "call", "come", "wait", "note"),
        "thank" to listOf("you", "you so", "you very", "you for", "you again"),
        "thanks" to listOf("for", "a lot", "so much", "again", "everyone"),
        "good" to listOf("morning", "evening", "night", "luck", "job", "work", "idea", "time", "day", "one"),
        "how" to listOf("are", "is", "do", "can", "much", "many", "long", "far", "often", "about"),
        "what" to listOf("is", "are", "do", "did", "will", "can", "should", "about", "if", "time"),
        "when" to listOf("is", "are", "do", "did", "will", "can", "you", "we", "they", "it"),
        "where" to listOf("is", "are", "do", "did", "will", "can", "you", "we", "they", "it"),
        "why" to listOf("is", "are", "do", "did", "would", "can", "not", "you", "we", "they"),
        "let" to listOf("me", "us", "him", "her", "them", "it", "go", "know", "see", "try"),
        "going" to listOf("to", "on", "out", "back", "home", "there", "here", "now", "soon", "well"),
        "want" to listOf("to", "you", "it", "this", "that", "more", "some", "a", "the", "help"),
        "need" to listOf("to", "you", "it", "this", "that", "more", "some", "a", "the", "help"),
        "love" to listOf("you", "it", "this", "that", "the", "to", "how", "when", "what", "your"),
        "hey" to listOf("there", "you", "everyone", "guys", "what", "how", "can", "are", "is", "do"),
        "hello" to listOf("there", "everyone", "world", "how", "what", "can", "are", "is", "do", "guys"),
        "okay" to listOf("so", "then", "but", "and", "now", "let", "I", "we", "that", "this"),
        "ok" to listOf("so", "then", "but", "and", "now", "let", "I", "we", "that", "this"),
        "yes" to listOf("I", "we", "it", "that", "this", "please", "sure", "of", "definitely", "absolutely"),
        "no" to listOf("I", "we", "it", "that", "this", "problem", "way", "more", "longer", "worries"),
        "sorry" to listOf("for", "about", "I", "to", "but", "that", "if", "the", "my", "we"),
        "just" to listOf("a", "the", "to", "do", "be", "let", "want", "need", "got", "saw"),
        "really" to listOf("good", "great", "nice", "bad", "sorry", "want", "need", "like", "love", "hope"),
        "so" to listOf("much", "many", "good", "great", "sorry", "happy", "sad", "tired", "excited", "far"),
        "very" to listOf("much", "good", "great", "nice", "bad", "sorry", "happy", "sad", "tired", "well"),
        "my" to listOf("name", "phone", "number", "email", "address", "friend", "family", "work", "home", "life"),
        "your" to listOf("name", "phone", "number", "email", "address", "friend", "family", "work", "home", "life"),
        "this" to listOf("is", "was", "will", "can", "should", "might", "could", "has", "had", "seems"),
        "that" to listOf("is", "was", "will", "can", "should", "might", "could", "has", "had", "seems"),
        "check" to listOf("it", "this", "that", "out", "the", "your", "my", "if", "when", "how"),
        "send" to listOf("me", "you", "it", "this", "that", "the", "a", "an", "your", "my"),
        "call" to listOf("me", "you", "it", "this", "that", "the", "a", "an", "your", "my"),
        "meet" to listOf("you", "me", "us", "them", "at", "in", "on", "for", "the", "a"),
        "see" to listOf("you", "me", "it", "this", "that", "the", "a", "an", "your", "my"),
        "talk" to listOf("to", "about", "with", "later", "soon", "now", "more", "you", "me", "us")
    )

    /**
     * Returns up to 3 word predictions based on the current partial word being typed
     * and the last completed word (for bigram context).
     */
    fun predict(currentWord: String, lastWord: String = ""): List<String> {
        val prefix = currentWord.lowercase().trim()
        val context = lastWord.lowercase().trim()

        // If no prefix typed yet, use bigram context
        if (prefix.isEmpty()) {
            val bigramSuggestions = BIGRAMS[context]
            if (!bigramSuggestions.isNullOrEmpty()) {
                return bigramSuggestions.take(3)
            }
            // Default starters
            return listOf("I", "The", "It").take(3)
        }

        // Prefix match: collect candidates
        val candidates = mutableListOf<String>()

        // 1. Bigram-aware: if context word has suggestions, prefer those that match prefix
        val bigramPool = BIGRAMS[context]
        if (!bigramPool.isNullOrEmpty()) {
            bigramPool.filter { it.startsWith(prefix, ignoreCase = true) }
                .forEach { candidates.add(it) }
        }

        // 2. Common word prefix match
        COMMON_WORDS.filter { it.startsWith(prefix, ignoreCase = true) && !candidates.contains(it) }
            .forEach { candidates.add(it) }

        // 3. Exact match as first suggestion if it's a real word
        val exactIdx = candidates.indexOfFirst { it.equals(prefix, ignoreCase = true) }
        if (exactIdx > 0) {
            val exact = candidates.removeAt(exactIdx)
            candidates.add(0, exact)
        }

        // Capitalize if the prefix starts with uppercase
        return candidates.take(3).map { word ->
            if (prefix[0].isUpperCase()) word.replaceFirstChar { it.uppercase() } else word
        }
    }
}
