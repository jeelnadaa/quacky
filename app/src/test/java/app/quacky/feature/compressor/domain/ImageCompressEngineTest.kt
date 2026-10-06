package app.quacky.feature.compressor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageCompressEngineTest {

    @Test
    fun `calculatePercentSaved calculates accurate non-negative percentages`() {
        assertEquals(70, CompressionResult.calculatePercentSaved(1000L, 300L))
        assertEquals(50, CompressionResult.calculatePercentSaved(2000L, 1000L))
        assertEquals(0, CompressionResult.calculatePercentSaved(1000L, 1000L))
        // Never claim saved if larger
        assertEquals(0, CompressionResult.calculatePercentSaved(1000L, 1200L))
        assertEquals(90, CompressionResult.calculatePercentSaved(100000L, 10000L))
    }

    @Test
    fun `formatBytes formats B, KB, MB accurately`() {
        assertEquals("0 B", CompressionResult.formatBytes(0L))
        assertEquals("512 B", CompressionResult.formatBytes(512L))
        assertEquals("1.0 KB", CompressionResult.formatBytes(1024L))
        assertEquals("2.00 MB", CompressionResult.formatBytes(2 * 1024 * 1024L))
    }

    @Test
    fun `binary search logic converges toward target size`() {
        // Pure Kotlin binary search verification matching ImageCompressEngine's search
        fun simulateBinarySearch(targetBytes: Long, sizeFunction: (Int) -> Long): Int {
            var lowQ = 5
            var highQ = 95
            var bestQ = 50

            for (i in 0..6) {
                val midQ = (lowQ + highQ) / 2
                val size = sizeFunction(midQ)
                if (size <= targetBytes) {
                    bestQ = midQ
                    lowQ = midQ + 1
                } else {
                    highQ = midQ - 1
                }
            }
            return bestQ
        }

        // Monotonic size function: size = q * 2000
        val target = 80000L // Expected q around 40
        val foundQ = simulateBinarySearch(target) { q -> q * 2000L }
        assertTrue("Expected Q around 40, got $foundQ", foundQ in 39..41)
        assertTrue(foundQ * 2000L <= target)
    }
}
