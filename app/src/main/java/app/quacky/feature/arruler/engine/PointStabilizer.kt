package app.quacky.feature.arruler.engine

import kotlin.math.abs
import kotlin.math.sqrt

enum class ReticleState {
    SEARCHING,
    TRACKING,
    LOCKED
}

data class StabilizedPointResult(
    val point: Vec3,
    val jitterMeters: Double,
    val isLocked: Boolean,
    val sampleCount: Int,
    val penalty: Float,
    val confidenceReason: ConfidenceReason?
)

class PointStabilizer {

    private data class TimestampedPoint(
        val point: Vec3,
        val timestampMs: Long
    )

    private val ringBuffer = ArrayDeque<TimestampedPoint>(MeasureTuning.STABILIZER_BUFFER_SIZE)

    fun reset() {
        ringBuffer.clear()
    }

    fun update(
        hit: SurfaceHit?,
        timestampMs: Long
    ): ReticleState {
        if (hit == null || hit.confidence < 0.4f) {
            ringBuffer.clear()
            return ReticleState.SEARCHING
        }

        val pt = hit.worldPoint

        // Check if new hit deviates > 1.5 cm from buffer median
        if (ringBuffer.isNotEmpty()) {
            val medPt = computeMedianPoint(ringBuffer.map { it.point })
            if ((pt - medPt).length() > MeasureTuning.TARGET_CHANGE_DISTANCE_METERS) {
                ringBuffer.clear()
            }
        }

        if (ringBuffer.size >= MeasureTuning.STABILIZER_BUFFER_SIZE) {
            ringBuffer.removeFirst()
        }
        ringBuffer.addLast(TimestampedPoint(pt, timestampMs))

        val result = getStabilizedPoint()
        return if (result.isLocked) ReticleState.LOCKED else ReticleState.TRACKING
    }

    fun getStabilizedPoint(): StabilizedPointResult {
        if (ringBuffer.isEmpty()) {
            return StabilizedPointResult(
                point = Vec3.ZERO,
                jitterMeters = 0.0,
                isLocked = false,
                sampleCount = 0,
                penalty = 0.4f,
                confidenceReason = ConfidenceReason.UNSTABLE
            )
        }

        val points = ringBuffer.map { it.point }
        val med = computeMedianPoint(points)

        // Compute MAD per axis
        val madX = computeMad(points.map { it.x }, med.x)
        val madY = computeMad(points.map { it.y }, med.y)
        val madZ = computeMad(points.map { it.z }, med.z)

        val threshX = MeasureTuning.MAD_OUTLIER_MULTIPLIER * madX + MeasureTuning.MAD_BASE_OFFSET_METERS
        val threshY = MeasureTuning.MAD_OUTLIER_MULTIPLIER * madY + MeasureTuning.MAD_BASE_OFFSET_METERS
        val threshZ = MeasureTuning.MAD_OUTLIER_MULTIPLIER * madZ + MeasureTuning.MAD_BASE_OFFSET_METERS

        // Filter outliers
        val inliers = points.filter { p ->
            abs(p.x - med.x) <= threshX &&
                    abs(p.y - med.y) <= threshY &&
                    abs(p.z - med.z) <= threshZ
        }

        val activeList = if (inliers.isNotEmpty()) inliers else points
        val meanPoint = activeList.fold(Vec3.ZERO) { acc, p -> acc + p } / activeList.size.toDouble()

        // Jitter = standard deviation of distance to mean
        val variance = activeList.map { (it - meanPoint).lengthSquared() }.average()
        val jitter = sqrt(variance)

        val durationMs = if (ringBuffer.size >= 2) {
            ringBuffer.last().timestampMs - ringBuffer.first().timestampMs
        } else 0L

        val isLocked = activeList.size >= MeasureTuning.LOCKED_MIN_SAMPLES &&
                jitter <= MeasureTuning.LOCKED_MAX_JITTER_METERS &&
                durationMs >= MeasureTuning.LOCKED_MIN_DURATION_MS

        val penalty = when {
            isLocked -> 0.0f
            activeList.size >= 3 -> 0.15f
            else -> 0.35f
        }

        val reason = if (!isLocked && jitter > MeasureTuning.LOCKED_MAX_JITTER_METERS) {
            ConfidenceReason.UNSTABLE
        } else null

        return StabilizedPointResult(
            point = meanPoint,
            jitterMeters = jitter,
            isLocked = isLocked,
            sampleCount = activeList.size,
            penalty = penalty,
            confidenceReason = reason
        )
    }

    private fun computeMedianPoint(pts: List<Vec3>): Vec3 {
        val xs = pts.map { it.x }.sorted()
        val ys = pts.map { it.y }.sorted()
        val zs = pts.map { it.z }.sorted()
        val mid = pts.size / 2
        return Vec3(xs[mid], ys[mid], zs[mid])
    }

    private fun computeMad(values: List<Double>, median: Double): Double {
        val diffs = values.map { abs(it - median) }.sorted()
        return if (diffs.isNotEmpty()) diffs[diffs.size / 2] else 0.0
    }
}
