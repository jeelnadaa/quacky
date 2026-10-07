package app.quacky.feature.arruler.engine

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin 3D vector for AR math and physics (no Android/GL dependencies).
 */
data class Vec3(
    val x: Double,
    val y: Double,
    val z: Double
) {
    constructor(x: Float, y: Float, z: Float) : this(x.toDouble(), y.toDouble(), z.toDouble())

    operator fun plus(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Double): Vec3 = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Double): Vec3 = Vec3(x / scalar, y / scalar, z / scalar)
    operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)

    fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun lengthSquared(): Double = x * x + y * y + z * z

    fun length(): Double = sqrt(lengthSquared())

    fun distance(other: Vec3): Double = (this - other).length()

    fun normalized(): Vec3 {
        val len = length()
        return if (len > 1e-12) this / len else Vec3(0.0, 1.0, 0.0)
    }

    fun toFloatArray(): FloatArray = floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat())

    companion object {
        val ZERO = Vec3(0.0, 0.0, 0.0)
        val UP = Vec3(0.0, 1.0, 0.0)
        val DOWN = Vec3(0.0, -1.0, 0.0)
        val FORWARD = Vec3(0.0, 0.0, -1.0)
        val RIGHT = Vec3(1.0, 0.0, 0.0)
    }
}
