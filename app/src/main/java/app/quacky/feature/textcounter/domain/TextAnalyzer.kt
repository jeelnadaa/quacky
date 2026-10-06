package app.quacky.feature.textcounter.domain

import java.text.BreakIterator
import java.util.Locale

data class TextStatistics(
    val characterCountWithSpaces: Int = 0,
    val characterCountWithoutSpaces: Int = 0,
    val wordCount: Int = 0,
    val sentenceCount: Int = 0,
    val paragraphCount: Int = 0,
    val lineCount: Int = 0,
    val readingTimeSeconds: Int = 0, // 238 wpm
    val speakingTimeSeconds: Int = 0 // 150 wpm
)

data class WordFrequency(
    val word: String,
    val count: Int,
    val percentage: Float
)

data class LetterFrequency(
    val letter: Char,
    val count: Int,
    val percentage: Float
)

data class SearchMatch(
    val start: Int,
    val end: Int
)

enum class SearchMode {
    WORD,
    LETTER,
    PHRASE,
    REGEX
}

object TextAnalyzer {

    val STOPWORDS = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't",
        "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can't",
        "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
        "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't", "have", "haven't", "having",
        "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself", "him", "himself", "his", "how",
        "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its",
        "itself", "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off", "on",
        "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "shan't",
        "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such", "than", "that", "that's",
        "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these", "they", "they'd",
        "they'll", "they're", "they've", "this", "those", "through", "to", "too", "under", "until", "up", "very",
        "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when",
        "when's", "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't",
        "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves"
    )

    /**
     * Accurate grapheme-cluster character count using BreakIterator (handles emojis and unicode graphemes).
     */
    fun countGraphemes(text: String, ignoreSpaces: Boolean = false): Int {
        if (text.isEmpty()) return 0
        val it = BreakIterator.getCharacterInstance(Locale.ROOT)
        it.setText(text)
        var count = 0
        var start = it.first()
        var end = it.next()
        while (end != BreakIterator.DONE) {
            val cluster = text.substring(start, end)
            if (!ignoreSpaces || !cluster.all { it.isWhitespace() }) {
                count++
            }
            start = end
            end = it.next()
        }
        return count
    }

    /**
     * Compute comprehensive live text stats.
     */
    fun analyze(text: String): TextStatistics {
        if (text.isEmpty()) return TextStatistics()

        val charsWithSpaces = countGraphemes(text, ignoreSpaces = false)
        val charsWithoutSpaces = countGraphemes(text, ignoreSpaces = true)

        // Word count via BreakIterator
        val wordIt = BreakIterator.getWordInstance(Locale.ROOT)
        wordIt.setText(text)
        var words = 0
        var start = wordIt.first()
        var end = wordIt.next()
        while (end != BreakIterator.DONE) {
            val token = text.substring(start, end)
            if (token.any { it.isLetterOrDigit() }) {
                words++
            }
            start = end
            end = wordIt.next()
        }

        // Sentence count via BreakIterator
        val sentenceIt = BreakIterator.getSentenceInstance(Locale.ROOT)
        sentenceIt.setText(text)
        var sentences = 0
        var sStart = sentenceIt.first()
        var sEnd = sentenceIt.next()
        while (sEnd != BreakIterator.DONE) {
            val sent = text.substring(sStart, sEnd)
            if (sent.any { it.isLetterOrDigit() }) {
                sentences++
            }
            sStart = sEnd
            sEnd = sentenceIt.next()
        }

        val lines = text.split("\n", "\r\n").size
        val paragraphs = text.split(Regex("(\r?\n){2,}")).filter { it.isNotBlank() }.size

        // Reading speed: ~238 wpm -> 238 / 60 words per second
        val readingSeconds = ((words / 238.0) * 60).toInt()
        // Speaking speed: ~150 wpm
        val speakingSeconds = ((words / 150.0) * 60).toInt()

        return TextStatistics(
            characterCountWithSpaces = charsWithSpaces,
            characterCountWithoutSpaces = charsWithoutSpaces,
            wordCount = words,
            sentenceCount = maxOf(sentences, if (words > 0) 1 else 0),
            paragraphCount = maxOf(paragraphs, if (words > 0) 1 else 0),
            lineCount = lines,
            readingTimeSeconds = readingSeconds,
            speakingTimeSeconds = speakingSeconds
        )
    }

    /**
     * Top words frequency analysis with optional stopword filtering and minimum length.
     */
    fun computeWordFrequency(
        text: String,
        ignoreStopwords: Boolean = true,
        minWordLength: Int = 2,
        limit: Int = 10
    ): List<WordFrequency> {
        if (text.isEmpty()) return emptyList()

        val tokens = text.lowercase()
            .split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.length >= minWordLength }
            .filter { !ignoreStopwords || !STOPWORDS.contains(it) }

        if (tokens.isEmpty()) return emptyList()

        val totalWords = tokens.size.toFloat()
        val counts = tokens.groupingBy { it }.eachCount()

        return counts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (word, count) ->
                WordFrequency(
                    word = word,
                    count = count,
                    percentage = (count / totalWords) * 100f
                )
            }
    }

    /**
     * Letter frequency analysis for a-z.
     */
    fun computeLetterFrequency(text: String): List<LetterFrequency> {
        val letters = text.lowercase().filter { it in 'a'..'z' }
        if (letters.isEmpty()) return emptyList()

        val total = letters.length.toFloat()
        val counts = letters.groupingBy { it }.eachCount()

        return ('a'..'z').map { char ->
            val count = counts[char] ?: 0
            LetterFrequency(
                letter = char,
                count = count,
                percentage = if (total > 0) (count / total) * 100f else 0f
            )
        }
    }

    /**
     * Find occurrences for search and highlight.
     */
    fun findOccurrences(
        text: String,
        query: String,
        mode: SearchMode,
        caseSensitive: Boolean = false
    ): List<SearchMatch> {
        if (text.isEmpty() || query.isEmpty()) return emptyList()

        val matches = mutableListOf<SearchMatch>()
        try {
            when (mode) {
                SearchMode.WORD -> {
                    val regex = Regex(
                        "\\b${Regex.escape(query)}\\b",
                        if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                    )
                    regex.findAll(text).forEach { m ->
                        matches.add(SearchMatch(m.range.first, m.range.last + 1))
                    }
                }
                SearchMode.LETTER, SearchMode.PHRASE -> {
                    val targetText = if (caseSensitive) text else text.lowercase()
                    val targetQuery = if (caseSensitive) query else query.lowercase()
                    var index = targetText.indexOf(targetQuery)
                    while (index >= 0) {
                        matches.add(SearchMatch(index, index + targetQuery.length))
                        index = targetText.indexOf(targetQuery, index + targetQuery.length)
                    }
                }
                SearchMode.REGEX -> {
                    val regex = Regex(
                        query,
                        if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                    )
                    regex.findAll(text).take(200).forEach { m ->
                        matches.add(SearchMatch(m.range.first, m.range.last + 1))
                    }
                }
            }
        } catch (_: Exception) {
            // Safe fallback on regex syntax errors
        }
        return matches
    }
}
