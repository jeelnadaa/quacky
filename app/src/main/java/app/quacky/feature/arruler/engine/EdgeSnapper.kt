package app.quacky.feature.arruler.engine

import kotlin.math.abs
import kotlin.math.max

data class EdgeSnapResult(
    val snappedPoint: Vec3,
    val isSnapped: Boolean,
    val snapScreenOffsetX: Float = 0f,
    val snapScreenOffsetY: Float = 0f
)

class EdgeSnapper {

    private var previousSnappedPoint: Vec3? = null
    private var isCurrentlySnapped: Boolean = false

    fun reset() {
        previousSnappedPoint = null
        isCurrentlySnapped = false
    }

    /**
     * Checks if a nearby edge with depth discontinuity exists and snaps to the foreground (nearer) side.
     */
    fun checkEdgeSnap(
        aimHit: SurfaceHit,
        samples: List<WeightedSample>,
        cameraPosition: Vec3
    ): EdgeSnapResult {
        val currentPt = aimHit.worldPoint
        val distToCam = aimHit.distanceFromCamera.toDouble()

        // If previously snapped, check release distance
        val prev = previousSnappedPoint
        if (isCurrentlySnapped && prev != null) {
            val distFromSnap = (currentPt - prev).length()
            val releaseDist = MeasureTuning.SNAP_EDGE_DEPTH_DISCONTINUITY_BASE_METERS *
                    MeasureTuning.SNAP_RELEASE_DISTANCE_MULTIPLIER
            if (distFromSnap > releaseDist) {
                isCurrentlySnapped = false
                previousSnappedPoint = null
            } else {
                return EdgeSnapResult(
                    snappedPoint = prev,
                    isSnapped = true
                )
            }
        }

        if (samples.size < 6) {
            return EdgeSnapResult(snappedPoint = currentPt, isSnapped = false)
        }

        val discontinuityThreshold = max(
            MeasureTuning.SNAP_EDGE_DEPTH_DISCONTINUITY_BASE_METERS,
            MeasureTuning.SNAP_EDGE_DEPTH_DISCONTINUITY_SCALE * distToCam
        )

        // Find depth step across samples
        val distances = samples.map { (it.point - cameraPosition).length() }
        val minD = distances.minOrNull() ?: distToCam
        val maxD = distances.maxOrNull() ?: distToCam

        if (maxD - minD >= discontinuityThreshold) {
            // There is a depth step (edge). Pick the nearer side cluster
            val nearSamples = samples.filter { (it.point - cameraPosition).length() <= minD + discontinuityThreshold * 0.5 }
            if (nearSamples.isNotEmpty()) {
                val nearCentroid = nearSamples.fold(Vec3.ZERO) { acc, s -> acc + s.point } / nearSamples.size.toDouble()
                isCurrentlySnapped = true
                previousSnappedPoint = nearCentroid
                return EdgeSnapResult(
                    snappedPoint = nearCentroid,
                    isSnapped = true
                )
            }
        }

        return EdgeSnapResult(snappedPoint = currentPt, isSnapped = false)
    }
}
