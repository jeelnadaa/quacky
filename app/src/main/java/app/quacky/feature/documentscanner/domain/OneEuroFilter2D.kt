package app.quacky.feature.documentscanner.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot

/**
 * One Euro filter for 2D points to eliminate jitter at low speed while avoiding lag at high speed.
 */
class OneEuroFilter2D(
    private val minCutoff: Float = ScanTuning.ONE_EURO_MIN_CUTOFF_HZ,
    private val beta: Float = ScanTuning.ONE_EURO_BETA,
    private val dCutoff: Float = ScanTuning.ONE_EURO_DERIVATIVE_CUTOFF_HZ
) {
    private var xFilter: LowPassFilter? = null
    private var yFilter: LowPassFilter? = null
    private var dxFilter: LowPassFilter? = null
    private var dyFilter: LowPassFilter? = null
    private var lastTimeMs: Long = -1L

    fun filter(x: Float, y: Float, timestampMs: Long = System.currentTimeMillis()): CornerPoint {
        if (lastTimeMs < 0L) {
            lastTimeMs = timestampMs
            xFilter = LowPassFilter(alpha(minCutoff, 1f / 30f)).also { it.setLast(x) }
            yFilter = LowPassFilter(alpha(minCutoff, 1f / 30f)).also { it.setLast(y) }
            dxFilter = LowPassFilter(alpha(dCutoff, 1f / 30f)).also { it.setLast(0f) }
            dyFilter = LowPassFilter(alpha(dCutoff, 1f / 30f)).also { it.setLast(0f) }
            return CornerPoint(x, y)
        }

        val dt = ((timestampMs - lastTimeMs).toFloat() / 1000f).coerceIn(0.001f, 0.5f)
        lastTimeMs = timestampMs

        val xF = xFilter ?: return CornerPoint(x, y)
        val yF = yFilter ?: return CornerPoint(x, y)
        val dxF = dxFilter ?: return CornerPoint(x, y)
        val dyF = dyFilter ?: return CornerPoint(x, y)

        // Reset if large jump detected (e.g. document moved or switched)
        val jumpDist = hypot(x - xF.last(), y - yF.last())
        if (jumpDist > ScanTuning.ONE_EURO_RESET_JUMP_DIAGONAL_RATIO) {
            xF.setLast(x)
            yF.setLast(y)
            dxF.setLast(0f)
            dyF.setLast(0f)
            return CornerPoint(x, y)
        }

        val dx = (x - xF.last()) / dt
        val dy = (y - yF.last()) / dt

        val edx = dxF.filter(dx, alpha(dCutoff, dt))
        val edy = dyF.filter(dy, alpha(dCutoff, dt))

        val cutoffX = minCutoff + beta * abs(edx)
        val cutoffY = minCutoff + beta * abs(edy)

        val filteredX = xF.filter(x, alpha(cutoffX, dt))
        val filteredY = yF.filter(y, alpha(cutoffY, dt))

        return CornerPoint(filteredX, filteredY)
    }

    fun reset() {
        lastTimeMs = -1L
        xFilter = null
        yFilter = null
        dxFilter = null
        dyFilter = null
    }

    private fun alpha(cutoff: Float, dt: Float): Float {
        val tau = 1.0f / (2.0f * PI.toFloat() * cutoff)
        return 1.0f / (1.0f + tau / dt)
    }

    private class LowPassFilter(private var alpha: Float) {
        private var lastVal = 0f
        private var initialized = false

        fun filter(value: Float, newAlpha: Float = alpha): Float {
            alpha = newAlpha
            lastVal = if (initialized) {
                alpha * value + (1.0f - alpha) * lastVal
            } else {
                initialized = true
                value
            }
            return lastVal
        }

        fun last(): Float = lastVal
        fun setLast(v: Float) {
            lastVal = v
            initialized = true
        }
    }
}
