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

/**
 * High-accuracy live document edge detector running at 10-15 Hz with One Euro filtering.
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
            if (planes.isEmpty()) {
                image.close()
                return
            }

            val yPlane = planes[0]
            val yBuffer: ByteBuffer = yPlane.buffer
            val width = image.width
            val height = image.height
            val rowStride = yPlane.rowStride

            val yMat = Mat(height, width, CvType.CV_8UC1)
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
            when (rotation) {
                90 -> {
                    Core.rotate(yMat, uprightMat, Core.ROTATE_90_CLOCKWISE)
                }
                180 -> {
                    Core.rotate(yMat, uprightMat, Core.ROTATE_180)
                }
                270 -> {
                    Core.rotate(yMat, uprightMat, Core.ROTATE_90_COUNTERCLOCKWISE)
                }
                else -> {
                    yMat.copyTo(uprightMat)
                }
            }
            yMat.release()

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
                        // Smooth adaptation
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

            uprightMat.release()
        } catch (_: Exception) {
            onEdgeDetected(null)
        } finally {
            image.close()
        }
    }
}
