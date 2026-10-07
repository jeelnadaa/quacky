package app.quacky.feature.arruler.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class ScreenPointResult(
    val screenX: Float,
    val screenY: Float,
    val isVisible: Boolean,
    val isBehindCamera: Boolean,
    val edgeArrowAngleRad: Float? = null,
    val edgeClampedX: Float = 0f,
    val edgeClampedY: Float = 0f
)

object ProjectionUtils {

    /**
     * Projects a 3D world position into 2D screen space coordinates using ARCore view & projection matrices.
     */
    fun projectToScreen(
        worldPoint: Vec3,
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        screenWidth: Float,
        screenHeight: Float
    ): ScreenPointResult {
        // 1. World to View space: eye = viewMatrix * worldPoint
        val wx = worldPoint.x.toFloat()
        val wy = worldPoint.y.toFloat()
        val wz = worldPoint.z.toFloat()

        val eyeX = viewMatrix[0] * wx + viewMatrix[4] * wy + viewMatrix[8] * wz + viewMatrix[12]
        val eyeY = viewMatrix[1] * wx + viewMatrix[5] * wy + viewMatrix[9] * wz + viewMatrix[13]
        val eyeZ = viewMatrix[2] * wx + viewMatrix[6] * wy + viewMatrix[10] * wz + viewMatrix[14]
        val eyeW = viewMatrix[3] * wx + viewMatrix[7] * wy + viewMatrix[11] * wz + viewMatrix[15]

        // 2. View to Clip space: clip = projMatrix * eye
        val clipX = projMatrix[0] * eyeX + projMatrix[4] * eyeY + projMatrix[8] * eyeZ + projMatrix[12] * eyeW
        val clipY = projMatrix[1] * eyeX + projMatrix[5] * eyeY + projMatrix[9] * eyeZ + projMatrix[13] * eyeW
        val clipZ = projMatrix[2] * eyeX + projMatrix[6] * eyeY + projMatrix[10] * eyeZ + projMatrix[14] * eyeW
        val clipW = projMatrix[3] * eyeX + projMatrix[7] * eyeY + projMatrix[11] * eyeZ + projMatrix[15] * eyeW

        val isBehind = clipW <= 0.05f

        val ndcX = if (isBehind) -clipX / (if (clipW == 0f) 0.001f else -clipW) else clipX / clipW
        val ndcY = if (isBehind) -clipY / (if (clipW == 0f) 0.001f else -clipW) else clipY / clipW

        val screenX = (ndcX + 1.0f) * 0.5f * screenWidth
        val screenY = (1.0f - ndcY) * 0.5f * screenHeight

        val margin = 24f
        val isInside = !isBehind &&
                screenX in margin..(screenWidth - margin) &&
                screenY in margin..(screenHeight - margin)

        var edgeAngle: Float? = null
        var clampedX = screenX
        var clampedY = screenY

        if (!isInside) {
            val cx = screenWidth / 2f
            val cy = screenHeight / 2f
            val dx = screenX - cx
            val dy = screenY - cy
            edgeAngle = atan2(dy, dx)

            // Clamp to screen edge rect with margin
            val edgeMargin = 40f
            clampedX = screenX.coerceIn(edgeMargin, screenWidth - edgeMargin)
            clampedY = screenY.coerceIn(edgeMargin, screenHeight - edgeMargin)
        }

        return ScreenPointResult(
            screenX = screenX,
            screenY = screenY,
            isVisible = isInside,
            isBehindCamera = isBehind,
            edgeArrowAngleRad = edgeAngle,
            edgeClampedX = clampedX,
            edgeClampedY = clampedY
        )
    }
}
