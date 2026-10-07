package app.quacky.feature.arruler.engine

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class ConfidenceLevel(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low")
}

data class PointConfidence(
    val score: Float,
    val level: ConfidenceLevel,
    val factors: Map<String, Float>,
    val weakestReasons: List<ConfidenceReason>
)

data class MeasurementConfidence(
    val score: Float,
    val level: ConfidenceLevel,
    val weakestReasons: List<ConfidenceReason>
)

class ConfidenceScorer {

    fun scorePoint(
        isTracking: Boolean,
        depthQuality: Float, // 0..1
        fitQuality: Float,   // 0..1
        distanceMeters: Float,
        jitterMeters: Double,
        hasSourceAgreement: Boolean,
        hasDisagreement: Boolean,
        parallaxRatio: Float,
        isShinyOrGlass: Boolean = false
    ): PointConfidence {
        val fTracking = if (isTracking) 1.0 else 0.0

        val fDepth = when {
            depthQuality >= 0.8f -> 1.0
            depthQuality > 0.0f -> 0.3 + 0.7 * depthQuality
            else -> 0.3
        }

        val fFit = fitQuality.toDouble().coerceIn(0.4, 1.0)

        val fRange = when {
            distanceMeters in 0.4f..3.0f -> 1.0
            distanceMeters < 0.4f -> (0.6 + 0.4 * (distanceMeters / 0.4f)).coerceIn(0.6, 1.0)
            distanceMeters <= 5.0f -> (1.0 - 0.6 * ((distanceMeters - 3.0f) / 2.0f)).coerceIn(0.4, 1.0)
            distanceMeters <= 8.0f -> (0.4 - 0.2 * ((distanceMeters - 5.0f) / 3.0f)).coerceIn(0.2, 0.4)
            else -> 0.1
        }

        val fStability = when {
            jitterMeters <= 0.002 -> 1.0
            jitterMeters >= 0.010 -> 0.3
            else -> 1.0 - 0.7 * ((jitterMeters - 0.002) / 0.008)
        }

        val fAgreement = when {
            hasSourceAgreement -> 1.0
            hasDisagreement -> 0.5
            else -> 0.8
        }

        val fParallax = if (parallaxRatio >= 1.0f) 1.0 else (0.6 + 0.4 * parallaxRatio.toDouble()).coerceIn(0.6, 1.0)

        val fSurface = if (isShinyOrGlass) 0.5 else 1.0

        val factors = mapOf(
            "tracking" to (fTracking to MeasureTuning.CONF_W_TRACKING),
            "depth" to (fDepth to MeasureTuning.CONF_W_DEPTH_QUALITY),
            "fit" to (fFit to MeasureTuning.CONF_W_FIT_QUALITY),
            "range" to (fRange to MeasureTuning.CONF_W_RANGE),
            "stability" to (fStability to MeasureTuning.CONF_W_STABILITY),
            "agreement" to (fAgreement to MeasureTuning.CONF_W_AGREEMENT),
            "parallax" to (fParallax to MeasureTuning.CONF_W_PARALLAX),
            "surface" to (fSurface to MeasureTuning.CONF_W_SURFACE_TYPE)
        )

        // Weighted geometric mean = exp( sum( w_i * ln(f_i) ) )
        var logSum = 0.0
        for ((_, pair) in factors) {
            val (v, w) = pair
            val safeV = max(1e-4, v)
            logSum += w * ln(safeV)
        }
        val score = exp(logSum).toFloat().coerceIn(0.0f, 1.0f)

        val reasons = mutableListOf<ConfidenceReason>()
        if (distanceMeters > MeasureTuning.WARN_TARGET_DISTANCE_METERS) reasons.add(ConfidenceReason.FAR_AWAY)
        if (distanceMeters < MeasureTuning.MIN_TARGET_DISTANCE_METERS) reasons.add(ConfidenceReason.TOO_CLOSE)
        if (depthQuality < 0.4f) reasons.add(ConfidenceReason.LOW_DEPTH_DATA)
        if (isShinyOrGlass) reasons.add(ConfidenceReason.SHINY_OR_TRANSPARENT)
        if (hasDisagreement) reasons.add(ConfidenceReason.SURFACES_DISAGREED)
        if (jitterMeters > MeasureTuning.LOCKED_MAX_JITTER_METERS) reasons.add(ConfidenceReason.UNSTABLE)
        if (parallaxRatio < 1.0f) reasons.add(ConfidenceReason.INSUFFICIENT_PARALLAX)

        val level = when {
            score >= MeasureTuning.CONFIDENCE_HIGH_THRESHOLD -> ConfidenceLevel.HIGH
            score >= MeasureTuning.CONFIDENCE_MEDIUM_THRESHOLD -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        return PointConfidence(
            score = score,
            level = level,
            factors = factors.mapValues { it.value.first.toFloat() },
            weakestReasons = reasons.take(2)
        )
    }

    fun scoreMeasurement(
        endpointConfidences: List<Float>,
        pathLengthMeters: Double,
        durationMs: Long,
        pointReasons: List<Set<ConfidenceReason>>
    ): MeasurementConfidence {
        if (endpointConfidences.isEmpty()) {
            return MeasurementConfidence(0.0f, ConfidenceLevel.LOW, emptyList())
        }

        val minEndpoint = endpointConfidences.minOrNull() ?: 0.5f

        // Decay 0.99 ^ (path length / 1 m)
        val pathDecay = MeasureTuning.DRIFT_DECAY_RATE_PER_METER.pow(pathLengthMeters)

        // Time drift penalty: 0.9 if > 20s
        val timePenalty = if (durationMs > MeasureTuning.DRIFT_LONG_TIME_THRESHOLD_MS) {
            MeasureTuning.DRIFT_LONG_TIME_PENALTY
        } else 1.0

        val finalScore = (minEndpoint.toDouble() * pathDecay * timePenalty).toFloat().coerceIn(0.0f, 1.0f)

        val level = when {
            finalScore >= MeasureTuning.CONFIDENCE_HIGH_THRESHOLD -> ConfidenceLevel.HIGH
            finalScore >= MeasureTuning.CONFIDENCE_MEDIUM_THRESHOLD -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        val allReasons = pointReasons.flatten().toSet().toList()

        return MeasurementConfidence(
            score = finalScore,
            level = level,
            weakestReasons = allReasons.take(2)
        )
    }
}
