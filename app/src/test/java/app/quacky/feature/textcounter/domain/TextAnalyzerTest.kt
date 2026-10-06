package app.quacky.feature.textcounter.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextAnalyzerTest {

    @Test
    fun `empty text produces zero statistics`() {
        val stats = TextAnalyzer.analyze("")
        assertEquals(0, stats.characterCountWithSpaces)
        assertEquals(0, stats.wordCount)
        assertEquals(0, stats.sentenceCount)
    }

    @Test
    fun `grapheme cluster counting handles emojis accurately`() {
        // "Quacky 🦆" has 7 characters: Q, u, a, c, k, y, space, 🦆 = 8
        val text = "Quacky 🦆"
        val count = TextAnalyzer.countGraphemes(text)
        assertEquals(8, count)

        val withoutSpaces = TextAnalyzer.countGraphemes(text, ignoreSpaces = true)
        assertEquals(7, withoutSpaces)
    }

    @Test
    fun `word counting handles punctuation correctly`() {
        val text = "Hello, world! This is Quacky."
        val stats = TextAnalyzer.analyze(text)
        assertEquals(5, stats.wordCount)
        assertEquals(2, stats.sentenceCount)
    }

    @Test
    fun `word frequency filters stopwords`() {
        val text = "the duck and the duck in the pond"
        val freqWithStopwords = TextAnalyzer.computeWordFrequency(text, ignoreStopwords = false)
        assertEquals("the", freqWithStopwords.first().word)

        val freqFiltered = TextAnalyzer.computeWordFrequency(text, ignoreStopwords = true)
        assertEquals("duck", freqFiltered.first().word)
        assertEquals(2, freqFiltered.first().count)
    }

    @Test
    fun `search occurrences finds word boundaries in word mode`() {
        val text = "The cat scattered the catalog."
        val matches = TextAnalyzer.findOccurrences(text, "cat", SearchMode.WORD)
        assertEquals(1, matches.size)
        assertEquals(4, matches[0].start)
        assertEquals(7, matches[0].end)
    }
}
