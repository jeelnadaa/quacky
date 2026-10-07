package app.quacky.feature.arruler.engine

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class MeasurementMode {
    DISTANCE,
    HEIGHT,
    ANGLE,
    PATH
}

enum class RulerUnit {
    METERS,
    CENTIMETERS,
    INCHES,
    FEET_INCHES
}

data class ScreenDot(
    val index: Int,
    val worldPoint: Vec3,
    val screenX: Float,
    val screenY: Float,
    val isVisible: Boolean,
    val offScreenArrowAngleRad: Float? = null,
    val offScreenClampedX: Float = 0f,
    val offScreenClampedY: Float = 0f,
    val distanceFromCameraMeters: Float = 0f,
    val isPulsing: Boolean = false,
    val isTracking: Boolean = true
)

data class ScreenSegment(
    val startIndex: Int,
    val endIndex: Int,
    val startScreenX: Float,
    val startScreenY: Float,
    val endScreenX: Float,
    val endScreenY: Float,
    val lengthMeters: Double,
    val labelText: String,
    val labelScreenX: Float,
    val labelScreenY: Float,
    val isLive: Boolean = false
)

data class LiveFrameState(
    val dots: List<ScreenDot>,
    val solidSegments: List<ScreenSegment>,
    val liveSegment: ScreenSegment?,
    val reticleScreenX: Float,
    val reticleScreenY: Float,
    val reticleState: ReticleState,
    val surfaceKind: SurfaceKind?,
    val isSnapActive: Boolean,
    val snapLabel: String?,
    val levelPlumbHint: String?,
    val primaryReadoutText: String,
    val secondaryReadoutText: String?,
    val guidanceHint: String?,
    val canPlace: Boolean,
    val isCloseShapeSnap: Boolean = false,
    val liveArcAngleDeg: Double? = null,
    val liveArcVertexX: Float? = null,
    val liveArcVertexY: Float? = null
)

class LiveMeasurementController {

    private val placedPoints = mutableListOf<Vec3>()
    private var lastValidSurfaceHit: SurfaceHit? = null
    private var lastValidHitTimestampMs: Long = 0L

    // Hysteresis & rate-limiting for live readout display
    private var lastDisplayedLengthMeters: Double = 0.0
    private var lastDisplayUpdateTimeMs: Long = 0L
    private var cachedFormattedText: String = ""

    private var previousWasLevelOrPlumb: Boolean = false

    fun reset() {
        placedPoints.clear()
        lastValidSurfaceHit = null
        lastValidHitTimestampMs = 0L
        lastDisplayedLengthMeters = 0.0
        lastDisplayUpdateTimeMs = 0L
        cachedFormattedText = ""
        previousWasLevelOrPlumb = false
    }

    fun addPoint(point: Vec3) {
        placedPoints.add(point)
    }

    fun removeLastPoint(): Boolean {
        if (placedPoints.isNotEmpty()) {
            placedPoints.removeAt(placedPoints.size - 1)
            return true
        }
        return false
    }

    val placedCount: Int
        get() = placedPoints.size

    val currentPoints: List<Vec3>
        get() = ArrayList(placedPoints)

    /**
     * Updates frame state every frame. Pure math, zero Android UI dependencies.
     */
    fun updateFrame(
        mode: MeasurementMode,
        unit: RulerUnit,
        scaleFactor: Double,
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        cameraPosition: Vec3,
        screenWidth: Float,
        screenHeight: Float,
        currentHit: SurfaceHit?,
        reticleState: ReticleState,
        trackingStatus: TrackingStatus,
        snappedPoint: Vec3?,
        snapLabel: String?,
        timestampMs: Long
    ): LiveFrameState {
        val reticleX = screenWidth / 2f
        val reticleY = screenHeight / 2f

        if (currentHit != null) {
            lastValidSurfaceHit = currentHit
            lastValidHitTimestampMs = timestampMs
        }

        val isSurfaceStale = (timestampMs - lastValidHitTimestampMs) > MeasureTuning.LIVE_STALE_TIMEOUT_MS
        val effectiveAimPoint = snappedPoint ?: currentHit?.worldPoint ?: if (!isSurfaceStale) lastValidSurfaceHit?.worldPoint else null

        // 1. Project placed dots
        val dots = mutableListOf<ScreenDot>()
        for (i in placedPoints.indices) {
            val pt = placedPoints[i]
            val proj = ProjectionUtils.projectToScreen(pt, viewMatrix, projMatrix, screenWidth, screenHeight)
            val distToCam = (pt - cameraPosition).length().toFloat()

            dots.add(
                ScreenDot(
                    index = i,
                    worldPoint = pt,
                    screenX = proj.screenX,
                    screenY = proj.screenY,
                    isVisible = proj.isVisible,
                    offScreenArrowAngleRad = proj.edgeArrowAngleRad,
                    offScreenClampedX = proj.edgeClampedX,
                    offScreenClampedY = proj.edgeClampedY,
                    distanceFromCameraMeters = distToCam,
                    isPulsing = (i == 0 && placedPoints.size == 1),
                    isTracking = trackingStatus.isTracking
                )
            )
        }

        // 2. Build solid placed segments
        val solidSegments = mutableListOf<ScreenSegment>()
        if (placedPoints.size >= 2) {
            for (i in 0 until placedPoints.size - 1) {
                val p1 = placedPoints[i]
                val p2 = placedPoints[i + 1]
                val dot1 = dots[i]
                val dot2 = dots[i + 1]

                val segDist = (p2 - p1).length() * scaleFactor
                val segText = formatLength(segDist, unit)
                val midX = (dot1.screenX + dot2.screenX) / 2f
                val midY = (dot1.screenY + dot2.screenY) / 2f

                solidSegments.add(
                    ScreenSegment(
                        startIndex = i,
                        endIndex = i + 1,
                        startScreenX = dot1.screenX,
                        startScreenY = dot1.screenY,
                        endScreenX = dot2.screenX,
                        endScreenY = dot2.screenY,
                        lengthMeters = segDist,
                        labelText = segText,
                        labelScreenX = midX,
                        labelScreenY = midY,
                        isLive = false
                    )
                )
            }
        }

        // 3. Live rubber-band line from last dot to reticle/effectiveAimPoint
        var liveSegment: ScreenSegment? = null
        var liveDistance = 0.0
        var levelPlumbHint: String? = null
        var isCloseShape = false

        if (placedPoints.isNotEmpty() && effectiveAimPoint != null && !isSurfaceStale) {
            val lastIdx = placedPoints.size - 1
            val lastDot = dots[lastIdx]
            val lastWorldPt = placedPoints[lastIdx]

            // Check close shape snap in Path mode if >= 3 points
            if (mode == MeasurementMode.PATH && placedPoints.size >= 3) {
                val distToFirst = (effectiveAimPoint - placedPoints[0]).length()
                val screenDistToFirst = sqrt(
                    ((dots[0].screenX - reticleX) * (dots[0].screenX - reticleX) +
                            (dots[0].screenY - reticleY) * (dots[0].screenY - reticleY)).toDouble()
                )
                if (distToFirst <= MeasureTuning.CLOSE_SHAPE_SNAP_DISTANCE_METERS || screenDistToFirst <= 30.0) {
                    isCloseShape = true
                }
            }

            val targetWorldPt = if (isCloseShape) placedPoints[0] else effectiveAimPoint
            liveDistance = (targetWorldPt - lastWorldPt).length() * scaleFactor

            // Check Level / Plumb orientation: vector direction vs world Up (0, 1, 0)
            val segVec = (targetWorldPt - lastWorldPt).normalized()
            val absY = abs(segVec.y)
            // Vertical: |y| close to 1.0; Horizontal: |y| close to 0.0
            val angleFromHorizDeg = Math.toDegrees(asin(absY))
            if (angleFromHorizDeg <= MeasureTuning.LEVEL_PLUMB_TOLERANCE_DEG) {
                levelPlumbHint = "Level"
            } else if (abs(90.0 - angleFromHorizDeg) <= MeasureTuning.LEVEL_PLUMB_TOLERANCE_DEG) {
                levelPlumbHint = "Vertical"
            }

            // Hysteresis & rate-limiting for text
            val hysteresis = MeasureTuning.LIVE_VALUE_HYSTERESIS_METERS
            val minIntervalMs = 1000L / MeasureTuning.LIVE_MAX_UPDATE_RATE_HZ
            if (abs(liveDistance - lastDisplayedLengthMeters) >= hysteresis &&
                (timestampMs - lastDisplayUpdateTimeMs) >= minIntervalMs
            ) {
                lastDisplayedLengthMeters = liveDistance
                lastDisplayUpdateTimeMs = timestampMs
                val prefix = if ((currentHit?.confidence ?: 1f) < 0.5f) "≈ " else ""
                cachedFormattedText = prefix + formatLength(liveDistance, unit)
            }

            // Live line screen start and end
            val startX = if (lastDot.isVisible) lastDot.screenX else lastDot.offScreenClampedX
            val startY = if (lastDot.isVisible) lastDot.screenY else lastDot.offScreenClampedY

            liveSegment = ScreenSegment(
                startIndex = lastIdx,
                endIndex = -1,
                startScreenX = startX,
                startScreenY = startY,
                endScreenX = reticleX,
                endScreenY = reticleY,
                lengthMeters = liveDistance,
                labelText = cachedFormattedText.ifEmpty { formatLength(liveDistance, unit) },
                labelScreenX = (startX + reticleX) / 2f,
                labelScreenY = (startY + reticleY) / 2f,
                isLive = true
            )
        }

        // 4. Build primary and secondary readout texts
        var primaryReadout = ""
        var secondaryReadout: String? = null

        when (mode) {
            MeasurementMode.DISTANCE -> {
                if (placedPoints.isEmpty()) {
                    primaryReadout = "Aim and tap to place start"
                } else if (liveSegment != null) {
                    primaryReadout = liveSegment.labelText
                } else {
                    primaryReadout = if (isSurfaceStale) "Aim at a surface" else "Ready"
                }
            }
            MeasurementMode.PATH -> {
                val placedSum = solidSegments.sumOf { it.lengthMeters }
                val totalWithLive = placedSum + (liveSegment?.lengthMeters ?: 0.0)
                if (placedPoints.isEmpty()) {
                    primaryReadout = "Tap to place first point"
                } else {
                    primaryReadout = formatLength(totalWithLive, unit)
                    secondaryReadout = String.format(
                        "Placed %s · Live %s",
                        formatLength(placedSum, unit),
                        formatLength(liveSegment?.lengthMeters ?: 0.0, unit)
                    )
                }
            }
            MeasurementMode.HEIGHT -> {
                if (placedPoints.isEmpty()) {
                    primaryReadout = "Tap at floor or base"
                } else if (effectiveAimPoint != null) {
                    val p1 = placedPoints[0]
                    val heightVal = abs(effectiveAimPoint.y - p1.y) * scaleFactor
                    primaryReadout = "Height: " + formatLength(heightVal, unit)
                    secondaryReadout = "Distance: " + formatLength(liveDistance, unit)
                } else {
                    primaryReadout = "Aim at target top"
                }
            }
            MeasurementMode.ANGLE -> {
                if (placedPoints.size < 2) {
                    primaryReadout = if (placedPoints.isEmpty()) "Tap first arm point" else "Tap vertex point"
                } else if (effectiveAimPoint != null) {
                    val angleResult = MeasurementMath.computeAngle(
                        vertex = placedPoints[1],
                        armA = placedPoints[0],
                        armB = effectiveAimPoint
                    )
                    primaryReadout = String.format("%.1f°", angleResult.degrees)
                } else {
                    primaryReadout = "Aim second arm"
                }
            }
        }

        val canPlace = trackingStatus.canPlacePoint &&
                effectiveAimPoint != null &&
                !isSurfaceStale &&
                !trackingStatus.isMovingTooFast

        return LiveFrameState(
            dots = dots,
            solidSegments = solidSegments,
            liveSegment = liveSegment,
            reticleScreenX = reticleX,
            reticleScreenY = reticleY,
            reticleState = reticleState,
            surfaceKind = currentHit?.kind,
            isSnapActive = snappedPoint != null,
            snapLabel = snapLabel,
            levelPlumbHint = levelPlumbHint,
            primaryReadoutText = primaryReadout,
            secondaryReadoutText = secondaryReadout,
            guidanceHint = trackingStatus.currentHint,
            canPlace = canPlace,
            isCloseShapeSnap = isCloseShape
        )
    }

    private fun formatLength(meters: Double, unit: RulerUnit): String {
        return when (unit) {
            RulerUnit.METERS -> String.format("%.3f m", meters)
            RulerUnit.CENTIMETERS -> String.format("%.1f cm", meters * 100.0)
            RulerUnit.INCHES -> {
                val totalInches = meters * 39.37007874
                String.format("%.1f in", totalInches)
            }
            RulerUnit.FEET_INCHES -> {
                val totalInches = meters * 39.37007874
                val feet = (totalInches / 12.0).toInt()
                val inches = totalInches % 12.0
                String.format("%d' %.1f\"", feet, inches)
            }
        }
    }
}
