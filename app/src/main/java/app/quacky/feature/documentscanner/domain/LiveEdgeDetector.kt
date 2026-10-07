package app.quacky.feature.documentscanner.domain

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * High-accuracy live document edge detector running at 10-15 Hz with One Euro filtering.
 * Features automatic failover to lightweight pure-Kotlin analysis if native CV libraries are unavailable.
 */
class LiveEdgeDetector(
    private val onEdgeDetected: (DocumentQuad?) -> Unit
) : ImageAnalysis.Analyzer {

    private val candidateDetector = CandidateDetector()
    private val quadScorer = QuadScorer()
    private val subPixelRefiner = SubPixelRefiner()

    private val filterTL = OneEuroFilter2D()
    private val filterTR = OneEuroFilter2D()
    private val filterBR = OneEuroFilter2D()
    private val filterBL = OneEuroFilter2D()

    private var trackedQuad: List<Point>? = null
    private var trackedScore: Float = 0f
    private var consecutiveBeatenFrames = 0
    private var lockedFramesCount = 0

    private var lastAnalyzedTime = 0L
    private var previousFallbackQuad: DocumentQuad? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        // Throttle analysis to ~12 fps so UI and preview remain silky smooth
        if (currentTime - lastAnalyzedTime < 80) {
            image.close()
            return
        }
        lastAnalyzedTime = currentTime

        try {
            val planes = image.planes
            if (planes.isEmpty()) return

            if (OpenCvInitializer.isAvailable()) {
                analyzeWithOpenCv(image, currentTime)
            } else {
                analyzeWithFallback(image)
            }
        } catch (_: Throwable) {
            try {
                analyzeWithFallback(image)
            } catch (_: Throwable) {
                onEdgeDetected(null)
            }
        } finally {
            image.close()
        }
    }

    private fun analyzeWithOpenCv(image: ImageProxy, currentTime: Long) {
        val yPlane = image.planes[0]
        val yBuffer: ByteBuffer = yPlane.buffer
        val width = image.width
        val height = image.height
        val rowStride = yPlane.rowStride

        val yMat = Mat(height, width, CvType.CV_8UC1)
        try {
            val yBytes = ByteArray(yBuffer.remaining())
            yBuffer.get(yBytes)

            if (rowStride == width) {
                yMat.put(0, 0, yBytes)
            } else {
                for (row in 0 until height) {
                    yMat.put(row, 0, yBytes, row * rowStride, width)
                }
            }

            // Rotate based on CameraX image rotationDegrees so processing is upright
            val rotation = image.imageInfo.rotationDegrees
            val uprightMat = Mat()
            try {
                when (rotation) {
                    90 -> Core.rotate(yMat, uprightMat, Core.ROTATE_90_CLOCKWISE)
                    180 -> Core.rotate(yMat, uprightMat, Core.ROTATE_180)
                    270 -> Core.rotate(yMat, uprightMat, Core.ROTATE_90_COUNTERCLOCKWISE)
                    else -> yMat.copyTo(uprightMat)
                }

                val upW = uprightMat.cols().toFloat()
                val upH = uprightMat.rows().toFloat()

                // 1. Detect candidate quads
                val candidates = candidateDetector.detectCandidates(uprightMat)

                // 2. Score and select best candidate
                val bestScored = quadScorer.scoreAndSelectBest(candidates, uprightMat, trackedQuad)

                if (bestScored != null) {
                    // 3. Temporal hysteresis
                    val currentTracked = trackedQuad
                    if (currentTracked == null) {
                        trackedQuad = bestScored.corners
                        trackedScore = bestScored.score
                        consecutiveBeatenFrames = 0
                    } else {
                        if (bestScored.score > trackedScore * (1f + ScanTuning.TRACKED_SWITCH_THRESHOLD)) {
                            consecutiveBeatenFrames++
                            if (consecutiveBeatenFrames >= ScanTuning.TRACKED_SWITCH_CONSECUTIVE_FRAMES) {
                                trackedQuad = bestScored.corners
                                trackedScore = bestScored.score
                                consecutiveBeatenFrames = 0
                                filterTL.reset()
                                filterTR.reset()
                                filterBR.reset()
                                filterBL.reset()
                            }
                        } else {
                            consecutiveBeatenFrames = 0
                            trackedQuad = bestScored.corners
                            trackedScore = bestScored.score
                        }
                    }

                    val current = trackedQuad ?: bestScored.corners

                    // 4. Sub-pixel edge refinement (inside-out scan removes over-crop)
                    val refined = subPixelRefiner.refineQuad(current, uprightMat)

                    // 5. One Euro Filter stabilization
                    val sTL = filterTL.filter((refined[0].x / upW).toFloat(), (refined[0].y / upH).toFloat(), currentTime)
                    val sTR = filterTR.filter((refined[1].x / upW).toFloat(), (refined[1].y / upH).toFloat(), currentTime)
                    val sBR = filterBR.filter((refined[2].x / upW).toFloat(), (refined[2].y / upH).toFloat(), currentTime)
                    val sBL = filterBL.filter((refined[3].x / upW).toFloat(), (refined[3].y / upH).toFloat(), currentTime)

                    // 6. Lock state tracking
                    if (bestScored.score >= ScanTuning.LOCKED_MIN_SCORE && !bestScored.isFrameLimited) {
                        lockedFramesCount++
                    } else {
                        lockedFramesCount = 0
                    }
                    val isLocked = lockedFramesCount >= ScanTuning.LOCKED_MIN_CONSECUTIVE_FRAMES

                    val resultQuad = DocumentQuad(
                        topLeft = sTL,
                        topRight = sTR,
                        bottomRight = sBR,
                        bottomLeft = sBL,
                        score = bestScored.score,
                        isLocked = isLocked,
                        isFrameLimited = bestScored.isFrameLimited
                    )

                    onEdgeDetected(resultQuad)
                } else {
                    trackedQuad = null
                    trackedScore = 0f
                    lockedFramesCount = 0
                    onEdgeDetected(null)
                }
            } finally {
                uprightMat.release()
            }
        } finally {
            yMat.release()
        }
    }

    private fun analyzeWithFallback(image: ImageProxy) {
        val planes = image.planes
        if (planes.isEmpty()) return
        val yPlane = planes[0]
        val yBuffer: ByteBuffer = yPlane.buffer
        val width = image.width
        val height = image.height
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride

        val sampleW = 64
        val sampleH = 48
        val stepX = max(1, width / sampleW)
        val stepY = max(1, height / sampleH)

        val sampled = IntArray(sampleW * sampleH)
        var sumLum = 0L
        var count = 0

        for (sy in 0 until sampleH) {
            val imgY = min(height - 1, sy * stepY)
            val rowOffset = imgY * rowStride
            for (sx in 0 until sampleW) {
                val imgX = min(width - 1, sx * stepX)
                val pos = rowOffset + imgX * pixelStride
                if (pos < yBuffer.capacity()) {
                    val lum = yBuffer.get(pos).toInt() and 0xFF
                    sampled[sy * sampleW + sx] = lum
                    sumLum += lum
                    count++
                }
            }
        }

        if (count == 0) return

        val grad = IntArray(sampleW * sampleH)
        var sumGrad = 0L
        var gradCount = 0

        for (sy in 1 until sampleH - 1) {
            for (sx in 1 until sampleW - 1) {
                val gx = abs(sampled[sy * sampleW + (sx + 1)] - sampled[sy * sampleW + (sx - 1)])
                val gy = abs(sampled[(sy + 1) * sampleW + sx] - sampled[(sy - 1) * sampleW + sx])
                val magnitude = gx + gy
                grad[sy * sampleW + sx] = magnitude
                sumGrad += magnitude
                gradCount++
            }
        }

        val avgGrad = if (gradCount > 0) (sumGrad / gradCount).toInt() else 15
        val edgeThreshold = max(25, avgGrad * 2)

        var leftEdge = 0
        for (sx in 2 until sampleW / 2) {
            var hits = 0
            for (sy in sampleH / 5 until (sampleH * 4) / 5) {
                if (grad[sy * sampleW + sx] > edgeThreshold) hits++
            }
            if (hits >= sampleH / 8) {
                leftEdge = sx
                break
            }
        }

        var rightEdge = sampleW - 1
        for (sx in sampleW - 3 downTo sampleW / 2) {
            var hits = 0
            for (sy in sampleH / 5 until (sampleH * 4) / 5) {
                if (grad[sy * sampleW + sx] > edgeThreshold) hits++
            }
            if (hits >= sampleH / 8) {
                rightEdge = sx
                break
            }
        }

        var topEdge = 0
        for (sy in 2 until sampleH / 2) {
            var hits = 0
            for (sy2 in sampleW / 5 until (sampleW * 4) / 5) {
                if (grad[sy * sampleW + sy2] > edgeThreshold) hits++
            }
            if (hits >= sampleW / 8) {
                topEdge = sy
                break
            }
        }

        var bottomEdge = sampleH - 1
        for (sy in sampleH - 3 downTo sampleH / 2) {
            var hits = 0
            for (sy2 in sampleW / 5 until (sampleW * 4) / 5) {
                if (grad[sy * sampleW + sy2] > edgeThreshold) hits++
            }
            if (hits >= sampleW / 8) {
                bottomEdge = sy
                break
            }
        }

        val minX = leftEdge.coerceAtLeast(1)
        val maxX = rightEdge.coerceAtMost(sampleW - 2)
        val minY = topEdge.coerceAtLeast(1)
        val maxY = bottomEdge.coerceAtMost(sampleH - 2)

        val padX = (maxX - minX) * 0.02f
        val padY = (maxY - minY) * 0.02f

        val normLeft = ((minX - padX) / sampleW).coerceIn(0.03f, 0.90f)
        val normRight = ((maxX + padX) / sampleW).coerceIn(0.10f, 0.97f)
        val normTop = ((minY - padY) / sampleH).coerceIn(0.03f, 0.90f)
        val normBottom = ((maxY + padY) / sampleH).coerceIn(0.10f, 0.97f)

        if (normRight > normLeft + 0.15f && normBottom > normTop + 0.15f) {
            val newQuad = DocumentQuad(
                topLeft = CornerPoint(normLeft, normTop),
                topRight = CornerPoint(normRight, normTop),
                bottomRight = CornerPoint(normRight, normBottom),
                bottomLeft = CornerPoint(normLeft, normBottom)
            )
            val prev = previousFallbackQuad
            val smoothed = if (prev != null) {
                DocumentQuad(
                    topLeft = CornerPoint(prev.topLeft.x * 0.7f + newQuad.topLeft.x * 0.3f, prev.topLeft.y * 0.7f + newQuad.topLeft.y * 0.3f),
                    topRight = CornerPoint(prev.topRight.x * 0.7f + newQuad.topRight.x * 0.3f, prev.topRight.y * 0.7f + newQuad.topRight.y * 0.3f),
                    bottomRight = CornerPoint(prev.bottomRight.x * 0.7f + newQuad.bottomRight.x * 0.3f, prev.bottomRight.y * 0.7f + newQuad.bottomRight.y * 0.3f),
                    bottomLeft = CornerPoint(prev.bottomLeft.x * 0.7f + newQuad.bottomLeft.x * 0.3f, prev.bottomLeft.y * 0.7f + newQuad.bottomLeft.y * 0.3f)
                )
            } else {
                newQuad
            }
            previousFallbackQuad = smoothed
            onEdgeDetected(smoothed)
        } else {
            val fallback = DocumentQuad(
                topLeft = CornerPoint(0.08f, 0.08f),
                topRight = CornerPoint(0.92f, 0.08f),
                bottomRight = CornerPoint(0.92f, 0.92f),
                bottomLeft = CornerPoint(0.08f, 0.92f)
            )
            onEdgeDetected(fallback)
        }
    }
}
