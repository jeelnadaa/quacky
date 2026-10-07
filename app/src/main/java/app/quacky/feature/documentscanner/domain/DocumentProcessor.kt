package app.quacky.feature.documentscanner.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.max

object DocumentProcessor {

    /**
     * Re-detects and performs full-resolution sub-pixel edge refinement on the captured still.
     */
    fun refineCapturedCorners(stillBitmap: Bitmap, initialQuad: DocumentQuad?): DocumentQuad {
        if (!OpenCvInitializer.isAvailable()) return initialQuad ?: DocumentQuad()
        try {
            val w = stillBitmap.width
            val h = stillBitmap.height
            if (w <= 0 || h <= 0) return initialQuad ?: DocumentQuad()

            val srcMat = Mat()
            org.opencv.android.Utils.bitmapToMat(stillBitmap, srcMat)
            val grayMat = Mat()
            Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGR2GRAY)
            srcMat.release()

            val detector = CandidateDetector()
            val scorer = QuadScorer()
            val refiner = SubPixelRefiner()

            // Candidates from still
            val candidates = detector.detectCandidates(grayMat, targetLongEdge = 1280).toMutableList()

            // Include initial live quad if available
            if (initialQuad != null) {
                val livePoints = listOf(
                    Point(initialQuad.topLeft.x.toDouble() * w, initialQuad.topLeft.y.toDouble() * h),
                    Point(initialQuad.topRight.x.toDouble() * w, initialQuad.topRight.y.toDouble() * h),
                    Point(initialQuad.bottomRight.x.toDouble() * w, initialQuad.bottomRight.y.toDouble() * h),
                    Point(initialQuad.bottomLeft.x.toDouble() * w, initialQuad.bottomLeft.y.toDouble() * h)
                )
                candidates.add(CandidateDetector.RawQuad(livePoints, "Live-Mapped"))
            }

            val best = scorer.scoreAndSelectBest(candidates, grayMat)
            if (best != null) {
                val refined = refiner.refineQuad(best.corners, grayMat)
                grayMat.release()
                return DocumentQuad(
                    topLeft = CornerPoint((refined[0].x / w).toFloat().coerceIn(0f, 1f), (refined[0].y / h).toFloat().coerceIn(0f, 1f)),
                    topRight = CornerPoint((refined[1].x / w).toFloat().coerceIn(0f, 1f), (refined[1].y / h).toFloat().coerceIn(0f, 1f)),
                    bottomRight = CornerPoint((refined[2].x / w).toFloat().coerceIn(0f, 1f), (refined[2].y / h).toFloat().coerceIn(0f, 1f)),
                    bottomLeft = CornerPoint((refined[3].x / w).toFloat().coerceIn(0f, 1f), (refined[3].y / h).toFloat().coerceIn(0f, 1f)),
                    score = best.score,
                    isLocked = true,
                    isFrameLimited = best.isFrameLimited
                )
            }
            grayMat.release()
        } catch (_: Throwable) {}
        return initialQuad ?: DocumentQuad()
    }

    /**
     * Warps an arbitrary quadrilateral region on [src] to a rectangular page.
     */
    fun warpPerspective(src: Bitmap, quad: DocumentQuad): Bitmap {
        val w = src.width.toFloat()
        val h = src.height.toFloat()

        val tlX = (quad.topLeft.x * w).coerceIn(0f, w)
        val tlY = (quad.topLeft.y * h).coerceIn(0f, h)

        val trX = (quad.topRight.x * w).coerceIn(0f, w)
        val trY = (quad.topRight.y * h).coerceIn(0f, h)

        val brX = (quad.bottomRight.x * w).coerceIn(0f, w)
        val brY = (quad.bottomRight.y * h).coerceIn(0f, h)

        val blX = (quad.bottomLeft.x * w).coerceIn(0f, w)
        val blY = (quad.bottomLeft.y * h).coerceIn(0f, h)

        // Calculate output dimensions based on quad edge lengths
        val topWidth = hypot(trX - tlX, trY - tlY)
        val bottomWidth = hypot(brX - blX, brY - blY)
        val leftHeight = hypot(blX - tlX, blY - tlY)
        val rightHeight = hypot(brX - trX, brY - trY)

        val rawWidth = max(topWidth, bottomWidth)
        val rawHeight = max(leftHeight, rightHeight)

        // Compensate for perspective foreshortening (shooting document at an angle)
        val keystoneRatio = if (topWidth > 1f) (bottomWidth / topWidth).coerceIn(0.5f, 2.5f) else 1f
        val adjustedHeight = if (keystoneRatio > 1.05f) {
            rawHeight * (1f + (keystoneRatio - 1f) * 0.75f)
        } else {
            rawHeight
        }

        val targetWidth = rawWidth.coerceIn(100f, 2400f)
        val targetHeight = adjustedHeight.coerceIn(100f, 3200f)

        val srcPts = floatArrayOf(
            tlX, tlY,
            trX, trY,
            brX, brY,
            blX, blY
        )

        val dstPts = floatArrayOf(
            0f, 0f,
            targetWidth, 0f,
            targetWidth, targetHeight,
            0f, targetHeight
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)
        if (!success) {
            return src
        }

        val outBitmap = Bitmap.createBitmap(
            targetWidth.toInt().coerceAtLeast(1),
            targetHeight.toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(outBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(src, matrix, paint)

        return outBitmap
    }

    /**
     * Rotates [bitmap] clockwise by the specified degrees (0, 90, 180, 270).
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Applies Microsoft Lens-style document enhancement filters.
     */
    fun applyFilter(bitmap: Bitmap, filter: DocFilterType): Bitmap {
        return when (filter) {
            DocFilterType.ORIGINAL -> bitmap
            DocFilterType.MAGIC_COLOR -> applyMagicColor(bitmap)
            DocFilterType.BW -> applyBlackAndWhite(bitmap)
            DocFilterType.GRAYSCALE -> applyGrayscale(bitmap)
        }
    }

    /**
     * Magic Color: Enhances document readability, brightens background, boosts text contrast.
     */
    private fun applyMagicColor(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val cm = ColorMatrix()
        // Contrast enhancement and slight shadow lift
        val contrast = 1.35f
        val brightness = 20f
        val translate = (-0.5f * contrast + 0.5f) * 255f + brightness

        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /**
     * High-contrast document binarization (pure black text on white paper).
     */
    private fun applyBlackAndWhite(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val cm = ColorMatrix().apply {
            setSaturation(0f) // First convert to grayscale
        }

        // Steep contrast threshold to eliminate paper shadows
        val thresholdMatrix = ColorMatrix(floatArrayOf(
            3.2f, 0f, 0f, 0f, -270f,
            0f, 3.2f, 0f, 0f, -270f,
            0f, 0f, 3.2f, 0f, -270f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(thresholdMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /**
     * Standard 8-bit monochromatic document representation.
     */
    private fun applyGrayscale(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val cm = ColorMatrix().apply { setSaturation(0f) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }
}
