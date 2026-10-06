package app.quacky.feature.arruler.domain

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

data class ArMeasurementState(
    val mode: ArRulerMode = ArRulerMode.DISTANCE,
    val unit: ArUnit = ArUnit.CM,
    val points: List<ArPoint> = emptyList(),
    val segments: List<ArSegment> = emptyList(),
    val totalValueMeters: Float = 0f,
    val formattedTotal: String = "0.0 cm",
    val angleDegrees: Double? = null,
    val formattedAngle: String? = null,
    val heightFloorPoint: ArPoint? = null,
    val heightTopPoint: ArPoint? = null
)
