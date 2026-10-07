package app.quacky.feature.level.domain

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

object LevelMath {

    /**
     * Pitch in degrees (-180° to +180°).
     * Forward/backward tilt along the phone's Y axis.
     */
    fun computePitch(x: Float, y: Float, z: Float): Float {
        val pitchRad = atan2(y.toDouble(), sqrt((x * x + z * z).toDouble()))
        return (pitchRad * (180.0 / PI)).toFloat()
    }

    /**
     * Roll in degrees (-180° to +180°).
     * Left/right tilt along the phone's X axis.
     */
    fun computeRoll(x: Float, y: Float, z: Float): Float {
        val rollRad = atan2(-x.toDouble(), z.toDouble())
        return (rollRad * (180.0 / PI)).toFloat()
    }

    /**
     * Overall tilt deviation from pure flat surface (0° is perfectly flat).
     */
    fun computeTiltAngle(x: Float, y: Float, z: Float): Float {
        val magnitude = sqrt((x * x + y * y + z * z).toDouble())
        if (magnitude == 0.0) return 0f
        val cosAlpha = (kotlin.math.abs(z.toDouble()) / magnitude).coerceIn(-1.0, 1.0)
        return (acos(cosAlpha) * (180.0 / PI)).toFloat()
    }

    /**
     * Returns true if phone is lying flat on a surface (Z axis dominates gravity).
     */
    fun isFlatSurface(z: Float): Boolean {
        return kotlin.math.abs(z) > 6.5f
    }

    /**
     * Checks if angles are within leveling tolerance threshold (e.g. 0.4°).
     */
    fun isLevel(angle1: Float, angle2: Float, thresholdDegrees: Float = 0.4f): Boolean {
        return kotlin.math.abs(angle1) <= thresholdDegrees && kotlin.math.abs(angle2) <= thresholdDegrees
    }
}
