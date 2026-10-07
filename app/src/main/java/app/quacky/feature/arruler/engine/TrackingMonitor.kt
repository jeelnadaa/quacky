package app.quacky.feature.arruler.engine

import kotlin.math.acos
import kotlin.math.min
import kotlin.math.sqrt

data class TrackingStatus(
    val isTracking: Boolean,
    val linearSpeed: Double, // m/s
    val angularSpeed: Double, // rad/s
    val lateralParallaxMeters: Double,
    val parallaxProgress: Float, // 0..1
    val isParallaxComplete: Boolean,
    val isMovingTooFast: Boolean,
    val currentHint: String?,
    val canPlacePoint: Boolean
)

enum class TrackingFailureReasonEnum {
    NONE,
    BAD_STATE,
    INSUFFICIENT_LIGHT,
    EXCESSIVE_MOTION,
    INSUFFICIENT_FEATURES,
    CAMERA_UNAVAILABLE
}

class TrackingMonitor {

    private var previousTimestampNs: Long = 0L
    private var previousTx: Float = 0f
    private var previousTy: Float = 0f
    private var previousTz: Float = 0f
    private var previousQx: Float = 0f
    private var previousQy: Float = 0f
    private var previousQz: Float = 0f
    private var previousQw: Float = 1f
    private var hasPreviousPose: Boolean = false

    private var cumulativeLateralTranslationMeters: Double = 0.0
    private var isParallaxComplete: Boolean = false

    var linearSpeedMps: Double = 0.0
        private set
    var angularSpeedRadps: Double = 0.0
        private set

    fun reset() {
        previousTimestampNs = 0L
        hasPreviousPose = false
        cumulativeLateralTranslationMeters = 0.0
        isParallaxComplete = false
        linearSpeedMps = 0.0
        angularSpeedRadps = 0.0
    }

    /**
     * Updates camera tracking telemetry.
     * [tx], [ty], [tz]: Camera position in world space.
     * [qx], [qy], [qz], [qw]: Camera rotation quaternion in world space.
     * [targetDistanceMeters]: Distance to target surface, or null if no surface hit.
     */
    fun update(
        timestampNs: Long,
        isTracking: Boolean,
        failureReason: TrackingFailureReasonEnum,
        tx: Float,
        ty: Float,
        tz: Float,
        qx: Float,
        qy: Float,
        qz: Float,
        qw: Float,
        isDepthReady: Boolean,
        targetDistanceMeters: Float?
    ): TrackingStatus {
        if (!hasPreviousPose) {
            previousTx = tx
            previousTy = ty
            previousTz = tz
            previousQx = qx
            previousQy = qy
            previousQz = qz
            previousQw = qw
            previousTimestampNs = timestampNs
            hasPreviousPose = true
            return buildStatus(
                isTracking = isTracking,
                failureReason = failureReason,
                isDepthReady = isDepthReady,
                targetDistance = targetDistanceMeters
            )
        }

        val dtSeconds = if (previousTimestampNs > 0 && timestampNs > previousTimestampNs) {
            (timestampNs - previousTimestampNs) / 1_000_000_000.0
        } else {
            1.0 / 30.0
        }

        if (dtSeconds > 0.001) {
            val dx = tx - previousTx
            val dy = ty - previousTy
            val dz = tz - previousTz
            val linearDist = sqrt((dx * dx + dy * dy + dz * dz).toDouble())
            linearSpeedMps = linearDist / dtSeconds

            // Camera right vector in world space: rotate (1, 0, 0) by camera orientation
            // q * (1, 0, 0) * q^-1
            val rx = 1f - 2f * (qy * qy + qz * qz)
            val ry = 2f * (qx * qy + qz * qw)
            val rz = 2f * (qx * qz - qy * qw)
            val lateralDelta = kotlin.math.abs(dx * rx + dy * ry + dz * rz).toDouble()

            if (!isParallaxComplete && isTracking) {
                // Ignore fast jerks or huge jumps
                if (linearSpeedMps < 1.0) {
                    cumulativeLateralTranslationMeters += lateralDelta
                    if (cumulativeLateralTranslationMeters >= MeasureTuning.MIN_PARALLAX_LATERAL_METERS) {
                        isParallaxComplete = true
                    }
                }
            }

            // Angular speed: difference between quaternions
            val dotQ = kotlin.math.abs(qx * previousQx + qy * previousQy + qz * previousQz + qw * previousQw)
                .coerceIn(0f, 1f)
            val angleRad = 2.0 * acos(dotQ.toDouble())
            angularSpeedRadps = angleRad / dtSeconds
        }

        previousTx = tx
        previousTy = ty
        previousTz = tz
        previousQx = qx
        previousQy = qy
        previousQz = qz
        previousQw = qw
        previousTimestampNs = timestampNs

        return buildStatus(
            isTracking = isTracking,
            failureReason = failureReason,
            isDepthReady = isDepthReady,
            targetDistance = targetDistanceMeters
        )
    }

    private fun buildStatus(
        isTracking: Boolean,
        failureReason: TrackingFailureReasonEnum,
        isDepthReady: Boolean,
        targetDistance: Float?
    ): TrackingStatus {
        val parallaxProgress = min(
            1.0f,
            (cumulativeLateralTranslationMeters / MeasureTuning.MIN_PARALLAX_LATERAL_METERS).toFloat()
        )
        val isMovingTooFast = linearSpeedMps > MeasureTuning.MAX_LINEAR_SPEED_MPS ||
                angularSpeedRadps > MeasureTuning.MAX_ANGULAR_SPEED_RADPS

        var hint: String? = null
        var canPlace = false

        if (!isTracking) {
            hint = when (failureReason) {
                TrackingFailureReasonEnum.CAMERA_UNAVAILABLE, TrackingFailureReasonEnum.BAD_STATE ->
                    "Camera problem. Close and reopen the tool."
                TrackingFailureReasonEnum.INSUFFICIENT_LIGHT ->
                    "Too dark. Turn on a light."
                TrackingFailureReasonEnum.EXCESSIVE_MOTION ->
                    "Move slower."
                TrackingFailureReasonEnum.INSUFFICIENT_FEATURES ->
                    "Aim at a surface with some detail, or move closer."
                else -> "Looking for tracking..."
            }
        } else if (isMovingTooFast) {
            hint = "Hold steady."
        } else if (!isParallaxComplete) {
            hint = "Move your phone slowly sideways."
        } else if (targetDistance == null) {
            hint = if (!isDepthReady) {
                "Move a little to scan this surface."
            } else {
                "Aim at a surface with some detail, or move closer."
            }
        } else if (targetDistance < MeasureTuning.MIN_TARGET_DISTANCE_METERS) {
            hint = "Move back a little."
        } else if (targetDistance > MeasureTuning.MAX_TARGET_DISTANCE_METERS) {
            hint = "Too far. Move closer."
        } else {
            canPlace = true
            if (targetDistance > MeasureTuning.WARN_TARGET_DISTANCE_METERS) {
                hint = "Move closer for a better reading."
            }
        }

        return TrackingStatus(
            isTracking = isTracking,
            linearSpeed = linearSpeedMps,
            angularSpeed = angularSpeedRadps,
            lateralParallaxMeters = cumulativeLateralTranslationMeters,
            parallaxProgress = parallaxProgress,
            isParallaxComplete = isParallaxComplete,
            isMovingTooFast = isMovingTooFast,
            currentHint = hint,
            canPlacePoint = canPlace
        )
    }
}
