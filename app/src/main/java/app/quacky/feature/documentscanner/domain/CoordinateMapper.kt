package app.quacky.feature.documentscanner.domain

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF

/**
 * Maps coordinates between camera analysis buffer, UI preview viewport, and high-resolution still capture.
 */
class CoordinateMapper(
    val analysisWidth: Int = 1280,
    val analysisHeight: Int = 960,
    val previewWidth: Int = 1080,
    val previewHeight: Int = 1440,
    val stillWidth: Int = 4000,
    val stillHeight: Int = 3000
) {
    /**
     * Maps a normalized point (0..1, 0..1) from analysis space to normalized preview view space,
     * maintaining uniform 4:3 aspect ratio centering (matching PreviewView scaleType).
     */
    fun analysisNormalizedToPreviewNormalized(pt: CornerPoint): CornerPoint {
        // Since both analysis and preview are locked to 4:3, coordinate spaces are congruent
        val clampedX = pt.x.coerceIn(0f, 1f)
        val clampedY = pt.y.coerceIn(0f, 1f)
        return CornerPoint(clampedX, clampedY)
    }

    /**
     * Maps a normalized document quad to still image pixel coordinates.
     */
    fun normalizedQuadToStillPixels(
        quad: DocumentQuad,
        targetWidth: Int = stillWidth,
        targetHeight: Int = stillHeight
    ): Array<PointF> {
        val w = targetWidth.toFloat()
        val h = targetHeight.toFloat()
        return arrayOf(
            PointF(quad.topLeft.x * w, quad.topLeft.y * h),
            PointF(quad.topRight.x * w, quad.topRight.y * h),
            PointF(quad.bottomRight.x * w, quad.bottomRight.y * h),
            PointF(quad.bottomLeft.x * w, quad.bottomLeft.y * h)
        )
    }

    /**
     * Converts pixel points back into a normalized DocumentQuad.
     */
    fun stillPixelsToNormalizedQuad(
        points: Array<PointF>,
        targetWidth: Int = stillWidth,
        targetHeight: Int = stillHeight,
        score: Float = 1.0f,
        isLocked: Boolean = true
    ): DocumentQuad {
        val w = targetWidth.toFloat().coerceAtLeast(1f)
        val h = targetHeight.toFloat().coerceAtLeast(1f)
        return DocumentQuad(
            topLeft = CornerPoint((points[0].x / w).coerceIn(0f, 1f), (points[0].y / h).coerceIn(0f, 1f)),
            topRight = CornerPoint((points[1].x / w).coerceIn(0f, 1f), (points[1].y / h).coerceIn(0f, 1f)),
            bottomRight = CornerPoint((points[2].x / w).coerceIn(0f, 1f), (points[2].y / h).coerceIn(0f, 1f)),
            bottomLeft = CornerPoint((points[3].x / w).coerceIn(0f, 1f), (points[3].y / h).coerceIn(0f, 1f)),
            score = score,
            isLocked = isLocked
        )
    }

    /**
     * Computes the IoU (Intersection-over-Union) between two quads for tracking & temporal hysteresis.
     */
    fun computeQuadIoU(q1: DocumentQuad, q2: DocumentQuad): Float {
        val minX1 = minOf(q1.topLeft.x, q1.bottomLeft.x)
        val maxX1 = maxOf(q1.topRight.x, q1.bottomRight.x)
        val minY1 = minOf(q1.topLeft.y, q1.topRight.y)
        val maxY1 = maxOf(q1.bottomLeft.y, q1.bottomRight.y)

        val minX2 = minOf(q2.topLeft.x, q2.bottomLeft.x)
        val maxX2 = maxOf(q2.topRight.x, q2.bottomRight.x)
        val minY2 = minOf(q2.topLeft.y, q2.topRight.y)
        val maxY2 = maxOf(q2.bottomLeft.y, q2.bottomRight.y)

        val interLeft = maxOf(minX1, minX2)
        val interTop = maxOf(minY1, minY2)
        val interRight = minOf(maxX1, maxX2)
        val interBottom = minOf(maxY1, maxY2)

        val interArea = maxOf(0f, interRight - interLeft) * maxOf(0f, interBottom - interTop)
        val area1 = maxOf(0f, maxX1 - minX1) * maxOf(0f, maxY1 - minY1)
        val area2 = maxOf(0f, maxX2 - minX2) * maxOf(0f, maxY2 - minY2)
        val unionArea = area1 + area2 - interArea

        return if (unionArea > 0f) (interArea / unionArea).coerceIn(0f, 1f) else 0f
    }
}
