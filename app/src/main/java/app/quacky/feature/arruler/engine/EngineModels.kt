package app.quacky.feature.arruler.engine

enum class SurfaceKind(val label: String) {
    FLOOR("Floor"),
    CEILING("Ceiling"),
    WALL("Wall"),
    TOP_SURFACE("Top surface"),
    ANGLED("Angled surface"),
    CURVED_OBJECT("Curved object"),
    UNKNOWN("Surface")
}

enum class HitSource {
    PLANE,
    DEPTH_FIT,
    DEPTH_POINT,
    FUSED
}

enum class ConfidenceReason(val userMessage: String) {
    FAR_AWAY("Far away from target"),
    TOO_CLOSE("Too close to target"),
    LOW_DEPTH_DATA("Not much depth data here"),
    SHINY_OR_TRANSPARENT("Shiny or transparent surface"),
    PHONE_MOVING("Phone was moving"),
    SURFACES_DISAGREED("Surface estimates disagreed"),
    UNSTABLE("Target reading was fluctuating"),
    INSUFFICIENT_PARALLAX("Move phone sideways to calibrate scale")
}

data class SurfaceHit(
    val worldPoint: Vec3,
    val normal: Vec3,
    val kind: SurfaceKind,
    val source: HitSource,
    val distanceFromCamera: Float,
    val confidence: Float,
    val reasons: Set<ConfidenceReason> = emptySet()
)

data class WeightedSample(
    val point: Vec3,
    val weight: Double,
    val isFromFullDepth: Boolean = false
)
