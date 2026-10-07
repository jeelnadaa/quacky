package app.quacky.feature.arruler.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidenceScorerTest {

    @Test
    fun `scorePoint is monotonic - degraded conditions never increase score`() {
        val scorer = ConfidenceScorer()

        val optimal = scorer.scorePoint(
            isTracking = true,
            depthQuality = 0.9f,
            fitQuality = 0.95f,
            distanceMeters = 1.0f,
            jitterMeters = 0.001,
            hasSourceAgreement = true,
            hasDisagreement = false,
            parallaxRatio = 1.0f
        )
        assertEquals(ConfidenceLevel.HIGH, optimal.level)

        // Degrade tracking
        val degradedTracking = scorer.scorePoint(
            isTracking = false,
            depthQuality = 0.9f,
            fitQuality = 0.95f,
            distanceMeters = 1.0f,
            jitterMeters = 0.001,
            hasSourceAgreement = true,
            hasDisagreement = false,
            parallaxRatio = 1.0f
        )
        assertTrue(degradedTracking.score < optimal.score)

        // Degrade range to 7m
        val farRange = scorer.scorePoint(
            isTracking = true,
            depthQuality = 0.9f,
            fitQuality = 0.95f,
            distanceMeters = 7.0f,
            jitterMeters = 0.001,
            hasSourceAgreement = true,
            hasDisagreement = false,
            parallaxRatio = 1.0f
        )
        assertTrue(farRange.score < optimal.score)

        // Degrade jitter
        val shaky = scorer.scorePoint(
            isTracking = true,
            depthQuality = 0.9f,
            fitQuality = 0.95f,
            distanceMeters = 1.0f,
            jitterMeters = 0.012, // 12 mm jitter
            hasSourceAgreement = true,
            hasDisagreement = false,
            parallaxRatio = 1.0f
        )
        assertTrue(shaky.score < optimal.score)
    }

    @Test
    fun `scoreMeasurement applies path drift decay and duration penalty`() {
        val scorer = ConfidenceScorer()

        // Short duration (<20s), short path (1m)
        val shortMeas = scorer.scoreMeasurement(
            endpointConfidences = listOf(0.85f, 0.90f),
            pathLengthMeters = 1.0,
            durationMs = 5000L,
            pointReasons = emptyList()
        )
        assertEquals(ConfidenceLevel.HIGH, shortMeas.level)

        // Long duration (>20s) and long path (10m)
        val longMeas = scorer.scoreMeasurement(
            endpointConfidences = listOf(0.85f, 0.90f),
            pathLengthMeters = 10.0,
            durationMs = 35000L,
            pointReasons = emptyList()
        )
        assertTrue(longMeas.score < shortMeas.score)
    }
}
