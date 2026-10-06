package app.quacky.feature.arruler.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ArMath {

    fun distance(p1: Vector3, p2: Vector3): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val dz = p2.z - p1.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun pathDistance(points: List<Vector3>): Float {
        if (points.size < 2) return 0f
        var total = 0f
        for (i in 0 until points.size - 1) {
            total += distance(points[i], points[i + 1])
        }
        return total
    }

    fun heightDistance(floor: Vector3, top: Vector3): Float {
        return abs(top.y - floor.y)
    }

    fun angleDegrees(p1: Vector3, vertex: Vector3, p2: Vector3): Double {
        val v1x = p1.x - vertex.x
        val v1y = p1.y - vertex.y
        val v1z = p1.z - vertex.z

        val v2x = p2.x - vertex.x
        val v2y = p2.y - vertex.y
        val v2z = p2.z - vertex.z

        val m1 = sqrt(v1x * v1x + v1y * v1y + v1z * v1z)
        val m2 = sqrt(v2x * v2x + v2y * v2y + v2z * v2z)

        if (m1 < 1e-6f || m2 < 1e-6f) return 0.0

        val dot = v1x * v2x + v1y * v2y + v1z * v2z
        val cosVal = (dot / (m1 * m2)).toDouble().coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosVal))
    }

    fun formatMeasurement(meters: Float, unit: ArUnit): String {
        return when (unit) {
            ArUnit.CM -> String.format(Locale.US, "%.1f cm", meters * 100f)
            ArUnit.MM -> String.format(Locale.US, "%.0f mm", meters * 1000f)
            ArUnit.M -> String.format(Locale.US, "%.2f m", meters)
            ArUnit.IN -> String.format(Locale.US, "%.1f in", meters * 39.3700787f)
            ArUnit.FT_IN -> {
                val totalInches = meters * 39.3700787f
                var feet = (totalInches / 12f).toInt()
                var inches = (totalInches % 12f).roundToInt()
                if (inches == 12) {
                    feet += 1
                    inches = 0
                }
                "$feet ft $inches in"
            }
        }
    }

    fun formatAngle(degrees: Double): String {
        return String.format(Locale.US, "%.1f°", degrees)
    }

    /**
     * Column-major 4x4 matrix multiplication: result = a * b
     */
    fun multiplyMatrix(a: FloatArray, b: FloatArray): FloatArray {
        val result = FloatArray(16)
        for (col in 0..3) {
            for (row in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    // a[k * 4 + row] * b[col * 4 + k]
                    sum += a[k * 4 + row] * b[col * 4 + k]
                }
                result[col * 4 + row] = sum
            }
        }
        return result
    }

    /**
     * Projects a 3D point in world space to 2D screen coordinates using View and Projection matrices.
     */
    fun projectWorldToScreen(
        worldPos: Vector3,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        screenWidth: Float,
        screenHeight: Float
    ): ScreenPoint? {
        val vp = multiplyMatrix(projectionMatrix, viewMatrix)

        val x = worldPos.x
        val y = worldPos.y
        val z = worldPos.z
        val w = 1f

        val clipX = vp[0] * x + vp[4] * y + vp[8] * z + vp[12] * w
        val clipY = vp[1] * x + vp[5] * y + vp[9] * z + vp[13] * w
        val clipZ = vp[2] * x + vp[6] * y + vp[10] * z + vp[14] * w
        val clipW = vp[3] * x + vp[7] * y + vp[11] * z + vp[15] * w

        // Point is behind the camera
        if (clipW <= 0.001f) return null

        val ndcX = clipX / clipW
        val ndcY = clipY / clipW

        val screenX = (ndcX + 1f) * 0.5f * screenWidth
        val screenY = (1f - ndcY) * 0.5f * screenHeight

        return ScreenPoint(screenX, screenY, distanceToCamera = clipW)
    }
}
