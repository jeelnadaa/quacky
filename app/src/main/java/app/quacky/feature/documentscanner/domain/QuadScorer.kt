package app.quacky.feature.documentscanner.domain

import org.opencv.core.Mat
import org.opencv.core.Point
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Validates hard geometric filters and calculates multi-criteria quality scores for document quads.
 */
class QuadScorer {

    data class ScoredQuad(
        val corners: List<Point>,
        val score: Float,
        val isFrameLimited: Boolean,
        val source: String
    )

    fun scoreAndSelectBest(
        candidates: List<CandidateDetector.RawQuad>,
        grayMat: Mat,
        previousTrackedQuad: List<Point>? = null
    ): ScoredQuad? {
        val scoredList = candidates.mapNotNull { cand ->
            scoreCandidate(cand, grayMat, previousTrackedQuad)
        }
        return scoredList.maxByOrNull { it.score }
    }

    private fun scoreCandidate(
        candidate: CandidateDetector.RawQuad,
        gray: Mat,
        previousTrackedQuad: List<Point>?
    ): ScoredQuad? {
        val pts = candidate.corners
        if (pts.size != 4) return null
        val w = gray.cols()
        val h = gray.rows()

        // 4.1 Hard Filters
        // 1. Frame bounds & area
        val area = polygonArea(pts)
        val frameArea = (w * h).toDouble()
        val areaRatio = area / frameArea
        if (areaRatio < ScanTuning.MIN_CONTOUR_AREA_FRACTION || areaRatio > ScanTuning.MAX_CONTOUR_AREA_FRACTION) {
            return null
        }

        // 2. Interior angles between 55° and 125°
        for (i in 0 until 4) {
            val pPrev = pts[(i + 3) % 4]
            val pCurr = pts[i]
            val pNext = pts[(i + 1) % 4]
            val angle = angleBetween(pPrev, pCurr, pNext)
            if (angle < ScanTuning.MIN_INTERIOR_ANGLE_DEG || angle > ScanTuning.MAX_INTERIOR_ANGLE_DEG) {
                return null
            }
        }

        // 3. Opposite side length ratios between 0.4 and 2.5
        val topLen = distance(pts[0], pts[1])
        val rightLen = distance(pts[1], pts[2])
        val bottomLen = distance(pts[3], pts[2])
        val leftLen = distance(pts[0], pts[3])

        val tbRatio = if (bottomLen > 0) topLen / bottomLen else 0.0
        val lrRatio = if (rightLen > 0) leftLen / rightLen else 0.0
        if (tbRatio !in ScanTuning.MIN_OPPOSITE_SIDE_RATIO..ScanTuning.MAX_OPPOSITE_SIDE_RATIO) return null
        if (lrRatio !in ScanTuning.MIN_OPPOSITE_SIDE_RATIO..ScanTuning.MAX_OPPOSITE_SIDE_RATIO) return null

        // 4.3 Frame-border check
        val isFrameLimited = pts.any {
            it.x <= ScanTuning.FRAME_BORDER_MARGIN_PX ||
            it.x >= w - ScanTuning.FRAME_BORDER_MARGIN_PX ||
            it.y <= ScanTuning.FRAME_BORDER_MARGIN_PX ||
            it.y >= h - ScanTuning.FRAME_BORDER_MARGIN_PX
        }

        // 4.2 Scoring Components
        val edgeSupportScores = mutableListOf<Float>()
        var totalPolarityScore = 0f
        var totalContrast = 0f

        val samplesPerSide = 30
        for (side in 0 until 4) {
            val p1 = pts[side]
            val p2 = pts[(side + 1) % 4]
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val len = hypot(dx, dy)
            if (len <= 0) continue

            val nx = -dy / len
            val ny = dx / len

            var sideHits = 0
            var polarityAgreements = 0

            for (s in 3 until samplesPerSide - 3) {
                val t = s.toDouble() / samplesPerSide.toDouble()
                val sx = p1.x + t * dx
                val sy = p1.y + t * dy

                val inX = (sx - nx * 5.0).toInt().coerceIn(0, w - 1)
                val inY = (sy - ny * 5.0).toInt().coerceIn(0, h - 1)
                val outX = (sx + nx * 5.0).toInt().coerceIn(0, w - 1)
                val outY = (sy + ny * 5.0).toInt().coerceIn(0, h - 1)

                val valIn = gray.get(inY, inX)[0]
                val valOut = gray.get(outY, outX)[0]
                val diff = valIn - valOut

                if (abs(diff) > 20.0) sideHits++
                if (diff > 0.0) polarityAgreements++ // White paper on darker background
                totalContrast += abs(diff).toFloat()
            }

            val validSamples = (samplesPerSide - 6).coerceAtLeast(1)
            edgeSupportScores.add(sideHits.toFloat() / validSamples)
            totalPolarityScore += polarityAgreements.toFloat() / validSamples
        }

        val meanEdgeSupport = if (edgeSupportScores.isNotEmpty()) edgeSupportScores.average().toFloat() else 0f
        val minEdgeSupport = edgeSupportScores.minOrNull() ?: 0f
        val polarity = (totalPolarityScore / 4f).coerceIn(0f, 1f)
        val contrast = (totalContrast / (4f * (samplesPerSide - 6) * 128f)).coerceIn(0f, 1f)

        // Geometry plausibility (closeness to 90 degrees)
        var angleDiffSum = 0.0
        for (i in 0 until 4) {
            val a = angleBetween(pts[(i + 3) % 4], pts[i], pts[(i + 1) % 4])
            angleDiffSum += abs(a - 90.0)
        }
        val geometryPlausibility = (1.0 - (angleDiffSum / (4.0 * 35.0))).coerceIn(0.0, 1.0).toFloat()

        // Temporal IoU consistency
        val temporalConsistency = if (previousTrackedQuad != null && previousTrackedQuad.size == 4) {
            computePolygonIoU(pts, previousTrackedQuad)
        } else 0.5f

        val totalScore = (
            ScanTuning.WEIGHT_MEAN_EDGE_SUPPORT * meanEdgeSupport +
            ScanTuning.WEIGHT_MIN_EDGE_SUPPORT * minEdgeSupport +
            ScanTuning.WEIGHT_POLARITY * polarity +
            ScanTuning.WEIGHT_CONTRAST * contrast +
            ScanTuning.WEIGHT_GEOMETRY * geometryPlausibility +
            ScanTuning.WEIGHT_TEMPORAL * temporalConsistency
        ).coerceIn(0f, 1f)

        return ScoredQuad(pts, totalScore, isFrameLimited, candidate.source)
    }

    private fun polygonArea(pts: List<Point>): Double {
        var area = 0.0
        val n = pts.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += pts[i].x * pts[j].y - pts[j].x * pts[i].y
        }
        return abs(area / 2.0)
    }

    private fun distance(p1: Point, p2: Point): Double = hypot(p1.x - p2.x, p1.y - p2.y)

    private fun angleBetween(p1: Point, p2: Point, p3: Point): Double {
        val v1x = p1.x - p2.x
        val v1y = p1.y - p2.y
        val v2x = p3.x - p2.x
        val v2y = p3.y - p2.y
        val dot = v1x * v2x + v1y * v2y
        val l1 = hypot(v1x, v1y)
        val l2 = hypot(v2x, v2y)
        if (l1 <= 0 || l2 <= 0) return 90.0
        val cos = (dot / (l1 * l2)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cos))
    }

    private fun computePolygonIoU(p1: List<Point>, p2: List<Point>): Float {
        val minX1 = p1.minOf { it.x }
        val maxX1 = p1.maxOf { it.x }
        val minY1 = p1.minOf { it.y }
        val maxY1 = p1.maxOf { it.y }

        val minX2 = p2.minOf { it.x }
        val maxX2 = p2.maxOf { it.x }
        val minY2 = p2.minOf { it.y }
        val maxY2 = p2.maxOf { it.y }

        val interLeft = max(minX1, minX2)
        val interTop = max(minY1, minY2)
        val interRight = min(maxX1, maxX2)
        val interBottom = min(maxY1, maxY2)

        val interW = max(0.0, interRight - interLeft)
        val interH = max(0.0, interBottom - interTop)
        val interArea = interW * interH

        val area1 = max(0.0, maxX1 - minX1) * max(0.0, maxY1 - minY1)
        val area2 = max(0.0, maxX2 - minX2) * max(0.0, maxY2 - minY2)
        val unionArea = area1 + area2 - interArea

        return if (unionArea > 0.0) (interArea / unionArea).toFloat().coerceIn(0f, 1f) else 0f
    }
}
