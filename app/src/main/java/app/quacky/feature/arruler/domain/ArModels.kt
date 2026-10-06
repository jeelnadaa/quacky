package app.quacky.feature.arruler.domain

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class ArRulerMode {
    DISTANCE,
    PATH,
    HEIGHT,
    ANGLE
}

enum class ArUnit(val label: String, val toMetersFactor: Float) {
    CM("cm", 100f),
    MM("mm", 1000f),
    M("m", 1f),
    IN("in", 39.3700787f),
    FT_IN("ft+in", 39.3700787f)
}

data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float
)

data class ScreenPoint(
    val x: Float,
    val y: Float,
    val distanceToCamera: Float = 0f
)

data class ArPoint(
    val id: String = UUID.randomUUID().toString(),
    val worldPosition: Vector3,
    val screenPoint: ScreenPoint? = null,
    val isFloorAnchor: Boolean = false
)

data class ArSegment(
    val from: ArPoint,
    val to: ArPoint,
    val lengthMeters: Float,
    val formattedLength: String,
    val midScreenX: Float = 0f,
    val midScreenY: Float = 0f
)

enum class ArTrackingStatus {
    SEARCHING_SURFACE,
    SURFACE_FOUND,
    TRACKING_LOST
}

/**
 * Distinct gray tones for finished measurements on screen
 */
val MeasurementTones = listOf(
    Color(0xFFE2E2E2), // Crisp light gray
    Color(0xFFBEBEBE), // Neutral medium gray
    Color(0xFF9E9E9E), // Charcoal tint
    Color(0xFFD4D4D4), // Soft silver
    Color(0xFFAFAFAF)  // Slate gray
)

/**
 * A single discrete measurement inside an AR session (e.g. M1, M2...).
 */
data class ArSingleMeasurement(
    val id: String = UUID.randomUUID().toString(),
    val index: Int = 1,
    val name: String = "M$index",
    val mode: ArRulerMode = ArRulerMode.DISTANCE,
    val unit: ArUnit = ArUnit.CM,
    val points: List<ArPoint> = emptyList(),
    val segments: List<ArSegment> = emptyList(),
    val totalValueMeters: Float = 0f,
    val formattedValue: String = "0.0 cm",
    val angleDegrees: Double? = null,
    val formattedAngle: String? = null,
    val isFinished: Boolean = false,
    val isFavorite: Boolean = false,
    val note: String = "",
    val thumbnailUri: String? = null,
    val colorToneIndex: Int = 0
) {
    val displayValue: String
        get() = if (mode == ArRulerMode.ANGLE && formattedAngle != null) formattedAngle else formattedValue

    val midPointScreen: ScreenPoint?
        get() {
            if (segments.isNotEmpty()) {
                val seg = segments.first()
                if (seg.midScreenX > 0 && seg.midScreenY > 0) {
                    return ScreenPoint(seg.midScreenX, seg.midScreenY)
                }
            }
            val validScreenPoints = points.mapNotNull { it.screenPoint }
            if (validScreenPoints.isNotEmpty()) {
                val avgX = validScreenPoints.map { it.x }.average().toFloat()
                val avgY = validScreenPoints.map { it.y }.average().toFloat()
                return ScreenPoint(avgX, avgY)
            }
            return null
        }
}

/**
 * Saved AR Session detail for history viewing and static review.
 */
data class ArSessionDetail(
    val sessionId: String,
    val sessionName: String,
    val timestamp: Long,
    val coverScreenshotPath: String?,
    val measurements: List<ArSingleMeasurement>
)
