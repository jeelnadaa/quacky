package app.quacky.feature.compass.domain

import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class MagneticFieldStatus {
    SHIELDED,
    NORMAL_AMBIENT,
    ELEVATED,
    STRONG_EMF
}

data class OrientationData(
    val azimuthDegrees: Float,
    val pitchDegrees: Float,
    val rollDegrees: Float,
    val isFlat: Boolean
)

object CompassMath {

    /**
     * Computes orientation (azimuth, pitch, roll) from raw accelerometer and magnetometer readings.
     * Uses Android's SensorManager rotation matrix calculation.
     */
    fun computeOrientation(gravity: FloatArray, geomagnetic: FloatArray): OrientationData? {
        val r = FloatArray(9)
        val i = FloatArray(9)
        val success = SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)
        if (!success) return null

        val orientation = FloatArray(3)
        SensorManager.getOrientation(r, orientation)

        // Convert radians to degrees
        var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (azimuthDeg < 0) {
            azimuthDeg += 360f
        }

        val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()

        // Device is considered flat if pitch and roll are within 15 degrees
        val isFlat = abs(pitchDeg) < 15f && abs(rollDeg) < 15f

        return OrientationData(
            azimuthDegrees = azimuthDeg % 360f,
            pitchDegrees = pitchDeg,
            rollDegrees = rollDeg,
            isFlat = isFlat
        )
    }

    /**
     * Normalizes an angle into the [0, 360) degree range.
     */
    fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360f
        if (normalized < 0f) {
            normalized += 360f
        }
        return normalized
    }

    /**
     * Applies magnetic declination to convert Magnetic North azimuth to True North.
     * Declination is positive for East (+), negative for West (-).
     */
    fun applyDeclination(magneticAzimuth: Float, declination: Float): Float {
        return normalizeAngle(magneticAzimuth + declination)
    }

    /**
     * Calculates the shortest angular difference from current to target angle,
     * handling 0/360 wrap-around properly.
     * Returns a value between -180 and +180 degrees.
     */
    fun shortestAngularDelta(target: Float, current: Float): Float {
        val diff = (target - current + 180f) % 360f - 180f
        return if (diff < -180f) diff + 360f else diff
    }

    /**
     * Performs circular low-pass filtering on an angle in degrees.
     */
    fun smoothAngle(previous: Float, current: Float, alpha: Float = 0.85f): Float {
        val delta = shortestAngularDelta(current, previous)
        return normalizeAngle(previous + delta * (1f - alpha))
    }

    /**
     * Returns cardinal / intercardinal abbreviation for a given azimuth.
     */
    fun toCardinal(azimuthDegrees: Float): String {
        val normalized = normalizeAngle(azimuthDegrees)
        val index = ((normalized + 11.25f) / 22.5f).toInt() % 16
        val cardinals = arrayOf(
            "N", "NNE", "NE", "ENE",
            "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW",
            "W", "WNW", "NW", "NNW"
        )
        return cardinals[index]
    }

    /**
     * Computes the total magnetic field magnitude: |B| = sqrt(Bx² + By² + Bz²) in µT.
     */
    fun computeMagneticFieldMagnitude(bx: Float, by: Float, bz: Float): Float {
        return sqrt(bx * bx + by * by + bz * bz)
    }

    /**
     * Classifies magnetic field strength based on ambient Earth standards.
     * Typical ambient Earth magnetic flux is 25 µT to 65 µT.
     */
    fun classifyFieldStrength(magnitudeMicroTesla: Float): MagneticFieldStatus {
        return when {
            magnitudeMicroTesla < 20f -> MagneticFieldStatus.SHIELDED
            magnitudeMicroTesla <= 65f -> MagneticFieldStatus.NORMAL_AMBIENT
            magnitudeMicroTesla <= 120f -> MagneticFieldStatus.ELEVATED
            else -> MagneticFieldStatus.STRONG_EMF
        }
    }
}
