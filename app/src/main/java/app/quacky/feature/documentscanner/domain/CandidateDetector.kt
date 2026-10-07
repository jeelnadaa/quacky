package app.quacky.feature.documentscanner.domain

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Multi-generator candidate quadrilateral detector based on OpenCV.
 */
class CandidateDetector {

    data class RawQuad(
        val corners: List<Point>, // Exactly 4 points: TL, TR, BR, BL in detection coordinates
        val source: String
    )

    fun detectCandidates(
        grayInput: Mat,
        colorInput: Mat? = null,
        targetLongEdge: Int = ScanTuning.DETECTION_LONG_EDGE
    ): List<RawQuad> {
        val origW = grayInput.cols()
        val origH = grayInput.rows()
        if (origW <= 0 || origH <= 0) return emptyList()

        val scale = targetLongEdge.toDouble() / max(origW, origH).toDouble()
        val scaledW = (origW * scale).toInt()
        val scaledH = (origH * scale).toInt()

        val gray = Mat()
        Imgproc.resize(grayInput, gray, Size(scaledW.toDouble(), scaledH.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)

        // 3.1 Illumination Normalization
        val bg = Mat()
        val ksize = ScanTuning.ILLUMINATION_BLUR_KSIZE.toDouble()
        Imgproc.GaussianBlur(gray, bg, Size(ksize, ksize), 0.0)

        val norm = Mat()
        val grayF = Mat()
        val bgF = Mat()
        gray.convertTo(grayF, CvType.CV_32F)
        bg.convertTo(bgF, CvType.CV_32F)
        Core.divide(grayF, bgF, norm)
        Core.multiply(norm, Scalar(255.0), norm)
        norm.convertTo(norm, CvType.CV_8U)

        grayF.release()
        bgF.release()
        bg.release()

        // Light 3x3 denoise
        val denoised = Mat()
        Imgproc.GaussianBlur(norm, denoised, Size(3.0, 3.0), 0.0)

        val totalArea = (scaledW * scaledH).toDouble()
        val candidates = mutableListOf<RawQuad>()

        // Generator A: Edge-based (Auto-Canny)
        runCandidateA(denoised, totalArea, scaledW, scaledH)?.let { candidates.add(it) }

        // Generator B: Brightness / Otsu + Adaptive
        runCandidateB(denoised, totalArea, scaledW, scaledH)?.let { candidates.add(it) }

        // Generator C: Background-distance
        runCandidateC(denoised, totalArea, scaledW, scaledH)?.let { candidates.add(it) }

        // Generator D: Line segments
        runCandidateD(denoised, scaledW, scaledH)?.let { candidates.add(it) }

        // Scale candidates back up to original input coordinates
        val invScale = 1.0 / scale
        val scaledCandidates = candidates.map { raw ->
            val unscaledPoints = raw.corners.map { pt ->
                Point(pt.x * invScale, pt.y * invScale)
            }
            RawQuad(unscaledPoints, raw.source)
        }

        gray.release()
        norm.release()
        denoised.release()

        return scaledCandidates
    }

    private fun runCandidateA(norm: Mat, totalArea: Double, w: Int, h: Int): RawQuad? {
        val edges = Mat()
        val meanVal = Core.mean(norm).`val`[0]
        val lowThresh = (meanVal * 0.66).coerceIn(20.0, 150.0)
        val highThresh = (meanVal * 1.33).coerceIn(50.0, 200.0)
        Imgproc.Canny(norm, edges, lowThresh, highThresh)

        // Close 3x3 once, erode 1px so net growth is zero
        val kernel3 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(edges, edges, Imgproc.MORPH_CLOSE, kernel3)
        Imgproc.erode(edges, edges, kernel3, Point(-1.0, -1.0), 1)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        kernel3.release()
        edges.release()

        var bestQuad: RawQuad? = null
        var maxArea = 0.0

        for (c in contours) {
            val area = Imgproc.contourArea(c)
            if (area >= totalArea * ScanTuning.MIN_CONTOUR_AREA_FRACTION && area > maxArea) {
                val quad = contourToQuad(c, w, h)
                if (quad != null) {
                    maxArea = area
                    bestQuad = RawQuad(quad, "Edge-Canny")
                }
            }
            c.release()
        }
        return bestQuad
    }

    private fun runCandidateB(norm: Mat, totalArea: Double, w: Int, h: Int): RawQuad? {
        val thresh = Mat()
        Imgproc.threshold(norm, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)

        val kernel3 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        val kernel5 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_OPEN, kernel3)
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, kernel5)
        Imgproc.erode(thresh, thresh, kernel3, Point(-1.0, -1.0), 1)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(thresh, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        kernel3.release()
        kernel5.release()
        thresh.release()

        var bestQuad: RawQuad? = null
        var maxArea = 0.0

        for (c in contours) {
            val area = Imgproc.contourArea(c)
            if (area >= totalArea * ScanTuning.MIN_CONTOUR_AREA_FRACTION && area > maxArea) {
                val quad = contourToQuad(c, w, h)
                if (quad != null) {
                    maxArea = area
                    bestQuad = RawQuad(quad, "Otsu-Brightness")
                }
            }
            c.release()
        }
        return bestQuad
    }

    private fun runCandidateC(norm: Mat, totalArea: Double, w: Int, h: Int): RawQuad? {
        // Sample 6% border ring to model background
        val borderX = (w * 0.06).toInt()
        val borderY = (h * 0.06).toInt()

        val borderSamples = mutableListOf<Double>()
        for (x in 0 until w step 4) {
            borderSamples.add(norm.get(borderY, x)[0])
            borderSamples.add(norm.get(h - borderY - 1, x)[0])
        }
        for (y in 0 until h step 4) {
            borderSamples.add(norm.get(y, borderX)[0])
            borderSamples.add(norm.get(y, w - borderX - 1)[0])
        }

        if (borderSamples.isEmpty()) return null
        val meanBg = borderSamples.average()
        var variance = 0.0
        for (v in borderSamples) variance += (v - meanBg) * (v - meanBg)
        variance /= borderSamples.size

        if (variance > 900.0) return null // Cluttered background

        val diff = Mat()
        Core.absdiff(norm, Scalar(meanBg), diff)
        val mask = Mat()
        Imgproc.threshold(diff, mask, 25.0, 255.0, Imgproc.THRESH_BINARY)
        diff.release()

        val kernel3 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel3)
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel3)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(mask, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        kernel3.release()
        mask.release()

        var bestQuad: RawQuad? = null
        var maxArea = 0.0

        for (c in contours) {
            val area = Imgproc.contourArea(c)
            if (area >= totalArea * ScanTuning.MIN_CONTOUR_AREA_FRACTION && area > maxArea) {
                val quad = contourToQuad(c, w, h)
                if (quad != null) {
                    maxArea = area
                    bestQuad = RawQuad(quad, "Bg-Distance")
                }
            }
            c.release()
        }
        return bestQuad
    }

    private fun runCandidateD(norm: Mat, w: Int, h: Int): RawQuad? {
        val edges = Mat()
        Imgproc.Canny(norm, edges, 40.0, 120.0)

        val lines = Mat()
        Imgproc.HoughLinesP(edges, lines, 1.0, Math.PI / 180.0, 40, (w * 0.15), 12.0)
        edges.release()

        if (lines.rows() < 4) {
            lines.release()
            return null
        }

        // Group into segments and cluster
        data class LineSeg(val p1: Point, val p2: Point, val angleDeg: Double)
        val segments = mutableListOf<LineSeg>()

        for (i in 0 until lines.rows()) {
            val vec = lines.get(i, 0) ?: continue
            val p1 = Point(vec[0], vec[1])
            val p2 = Point(vec[2], vec[3])
            val angle = Math.toDegrees(atan2(p2.y - p1.y, p2.x - p1.x))
            val normalizedAngle = (angle + 180.0) % 180.0
            segments.add(LineSeg(p1, p2, normalizedAngle))
        }
        lines.release()

        val horizontal = segments.filter { it.angleDeg in 0.0..40.0 || it.angleDeg in 140.0..180.0 }
        val vertical = segments.filter { it.angleDeg in 50.0..130.0 }

        if (horizontal.size < 2 || vertical.size < 2) return null

        val topH = horizontal.minByOrNull { (it.p1.y + it.p2.y) / 2.0 } ?: return null
        val bottomH = horizontal.maxByOrNull { (it.p1.y + it.p2.y) / 2.0 } ?: return null
        val leftV = vertical.minByOrNull { (it.p1.x + it.p2.x) / 2.0 } ?: return null
        val rightV = vertical.maxByOrNull { (it.p1.x + it.p2.x) / 2.0 } ?: return null

        val pTL = intersectLines(topH.p1, topH.p2, leftV.p1, leftV.p2) ?: return null
        val pTR = intersectLines(topH.p1, topH.p2, rightV.p1, rightV.p2) ?: return null
        val pBR = intersectLines(bottomH.p1, bottomH.p2, rightV.p1, rightV.p2) ?: return null
        val pBL = intersectLines(bottomH.p1, bottomH.p2, leftV.p1, leftV.p2) ?: return null

        val quad = orderCorners(listOf(pTL, pTR, pBR, pBL))
        return RawQuad(quad, "Hough-Lines")
    }

    private fun contourToQuad(contour: MatOfPoint, w: Int, h: Int): List<Point>? {
        val c2f = MatOfPoint2f(*contour.toArray())
        val peri = Imgproc.arcLength(c2f, true)

        for (epsRatio in ScanTuning.EPSILON_SWEEPS) {
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(c2f, approx, epsRatio * peri, true)
            if (approx.rows() == 4 && Imgproc.isContourConvex(MatOfPoint(*approx.toArray()))) {
                val pts = approx.toArray().toList()
                approx.release()
                c2f.release()
                return orderCorners(pts)
            }
            approx.release()
        }

        // Hull vertex-removal reduction to 4
        val hullIndices = org.opencv.core.MatOfInt()
        Imgproc.convexHull(contour, hullIndices)
        val hullPoints = mutableListOf<Point>()
        val allPoints = contour.toArray()
        val indices = hullIndices.toArray()
        for (idx in indices) {
            hullPoints.add(allPoints[idx])
        }
        hullIndices.release()
        c2f.release()

        if (hullPoints.size < 4) return null
        val reduced = reducePolygonToFourVertices(hullPoints) ?: return null
        return orderCorners(reduced)
    }

    private fun reducePolygonToFourVertices(pts: List<Point>): List<Point>? {
        val list = pts.toMutableList()
        while (list.size > 4) {
            var minAddedArea = Double.MAX_VALUE
            var bestIdx = -1
            val n = list.size
            for (i in 0 until n) {
                val prev = list[(i - 1 + n) % n]
                val curr = list[i]
                val next = list[(i + 1) % n]
                val triArea = abs((curr.x * (next.y - prev.y) + next.x * (prev.y - curr.y) + prev.x * (curr.y - next.y)) / 2.0)
                if (triArea < minAddedArea) {
                    minAddedArea = triArea
                    bestIdx = i
                }
            }
            if (bestIdx >= 0) {
                list.removeAt(bestIdx)
            } else {
                break
            }
        }
        return if (list.size == 4) list else null
    }

    private fun orderCorners(pts: List<Point>): List<Point> {
        val cx = pts.map { it.x }.average()
        val cy = pts.map { it.y }.average()

        val sorted = pts.sortedBy { atan2(it.y - cy, it.x - cx) }
        // Determine top-left (smallest x + y)
        val tlIdx = sorted.indices.minByOrNull { sorted[it].x + sorted[it].y } ?: 0
        val n = sorted.size
        return listOf(
            sorted[tlIdx],
            sorted[(tlIdx + 1) % n],
            sorted[(tlIdx + 2) % n],
            sorted[(tlIdx + 3) % n]
        )
    }

    private fun intersectLines(p1: Point, p2: Point, p3: Point, p4: Point): Point? {
        val d = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x)
        if (abs(d) < 1e-4) return null
        val xi = ((p1.x * p2.y - p1.y * p2.x) * (p3.x - p4.x) - (p1.x - p2.x) * (p3.x * p4.y - p3.y * p4.x)) / d
        val yi = ((p1.x * p2.y - p1.y * p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x * p4.y - p3.y * p4.x)) / d
        return Point(xi, yi)
    }
}
