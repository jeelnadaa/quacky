package app.quacky.feature.arruler.engine

import kotlin.math.abs

class SurfaceClassifier {

    private var trackedFloorY: Double? = null

    fun reset() {
        trackedFloorY = null
    }

    fun updateFloorY(newFloorY: Double) {
        val current = trackedFloorY
        if (current == null || newFloorY < current) {
            trackedFloorY = newFloorY
        }
    }

    val floorY: Double?
        get() = trackedFloorY

    fun classify(
        point: Vec3,
        normal: Vec3,
        isPlanar: Boolean
    ): SurfaceKind {
        if (!isPlanar) {
            return SurfaceKind.CURVED_OBJECT
        }

        // cosUp with world up (0, 1, 0) is simply normal.y
        val cosUp = normal.y

        return when {
            cosUp > MeasureTuning.SURFACE_FLOOR_COS_UP -> {
                val fY = trackedFloorY
                if (fY != null) {
                    if (abs(point.y - fY) <= MeasureTuning.FLOOR_PROXIMITY_METERS) {
                        SurfaceKind.FLOOR
                    } else if (point.y >= fY + MeasureTuning.TOP_SURFACE_MIN_HEIGHT_ABOVE_FLOOR_METERS) {
                        SurfaceKind.TOP_SURFACE
                    } else {
                        SurfaceKind.FLOOR
                    }
                } else {
                    SurfaceKind.TOP_SURFACE
                }
            }
            cosUp < MeasureTuning.SURFACE_CEILING_COS_UP -> {
                SurfaceKind.CEILING
            }
            abs(cosUp) < MeasureTuning.SURFACE_WALL_MAX_COS_UP -> {
                SurfaceKind.WALL
            }
            else -> {
                SurfaceKind.ANGLED
            }
        }
    }
}
