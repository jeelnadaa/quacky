package app.quacky.feature.documentscanner.domain

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Sub-pixel edge refiner that eliminates over-crop by scanning outward from the inside of the document.
 */
class SubPixelRefiner {

    fun refineQuad(
        initialQuad: List<Point>,
        gray: Mat
    ): List<Point> {
        if (initialQuad.size != 4) return initialQuad
        val w = gray.cols()
        val h = gray.rows()
        val diag = hypot(w.toDouble(), h.toDouble())

        val windowW = max(
            ScanTuning.REFINEMENT_MIN_WINDOW_PX.toDouble(),
            diag * ScanTuning.REFINEMENT_WINDOW_DIAGONAL_RATIO
        ).toInt()

        val fittedLines = mutableListOf<Line2D?>()

        for (side in 0 until 4) {
            val p1 = initialQuad[side]
            val p2 = initialQuad[(side + 1) % 4]
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val len = hypot(dx, dy)
            if (len <= 0) {
                fittedLines.add(null)
                continue
            }

            // Normal pointing outward from document
            val nx = -dy / len
            val ny = dx / len

            val samplePoints = mutableListOf<Point>()
            val sampleOffsets = mutableListOf<Double>()
            val numSamples = ScanTuning.REFINEMENT_SAMPLES_PER_SIDE

            for (s in 5 until numSamples - 5) {
                val t = s.toDouble() / numSamples.toDouble()
                val cx = p1.x + t * dx
                val cy = p1.y + t * dy

                // Extract 1D intensity profile along normal: from -windowW (inside) to +windowW (outside)
                val profile = DoubleArray(windowW * 2 + 1)
                for (k in -windowW..windowW) {
                    val px = (cx + nx * k).toInt().coerceIn(0, w - 1)
                    val py = (cy + ny * k).toInt().coerceIn(0, h - 1)
                    profile[k + windowW] = gray.get(py, px)?.getOrNull(0) ?: 0.0
                }

                // Smooth lightly with [1, 2, 1] Gaussian kernel
                val smoothed = DoubleArray(profile.size)
                for (k in 1 until profile.size - 1) {
                    smoothed[k] = (profile[k - 1] + 2.0 * profile[k] + profile[k + 1]) / 4.0
                }
                smoothed[0] = profile[0]
                smoothed[profile.size - 1] = profile[profile.size - 1]

                // Compute derivative along normal (inside -> outside)
                val deriv = DoubleArray(profile.size - 1)
                var maxDerivMag = 0.0
                for (k in 0 until profile.size - 1) {
                    val d = smoothed[k + 1] - smoothed[k]
                    deriv[k] = d
                    if (abs(d) > maxDerivMag) maxDerivMag = abs(d)
                }

                if (maxDerivMag < 8.0) continue // Too weak contrast

                // Scan FROM INSIDE OUTWARD to find the FIRST significant falling edge (paper -> background)
                var bestPeakIdx = -1
                val thresh = maxDerivMag * ScanTuning.REFINEMENT_PEAK_THRESHOLD_RATIO

                for (k in 1 until deriv.size - 1) {
                    // Looking for local maximum in magnitude of drop
                    val valK = deriv[k]
                    if (abs(valK) >= thresh && abs(valK) >= abs(deriv[k - 1]) && abs(valK) >= abs(deriv[k + 1])) {
                        bestPeakIdx = k
                        break // Pick first peak from inside outward to prevent picking outer shadow or table
                    }
                }

                if (bestPeakIdx in 1 until deriv.size - 1) {
                    // Parabolic sub-pixel peak interpolation
                    val y0 = abs(deriv[bestPeakIdx - 1])
                    val y1 = abs(deriv[bestPeakIdx])
                    val y2 = abs(deriv[bestPeakIdx + 1])
                    val denom = 2.0 * (2.0 * y1 - y0 - y2)
                    val subDelta = if (abs(denom) > 1e-4) (y0 - y2) / denom else 0.0

                    val refinedK = (bestPeakIdx - windowW).toDouble() + subDelta
                    val edgeX = cx + nx * refinedK
                    val edgeY = cy + ny * refinedK

                    samplePoints.add(Point(edgeX, edgeY))
                    sampleOffsets.add(refinedK)
                }
            }

            if (samplePoints.size < (numSamples * ScanTuning.REFINEMENT_MIN_INLIER_RATIO).toInt()) {
                // Not enough samples, use original line
                fittedLines.add(Line2D(p1, p2))
                continue
            }

            // Filter outliers by Median Absolute Deviation (MAD) of offsets
            val medianOffset = sampleOffsets.sorted()[sampleOffsets.size / 2]
            val mads = sampleOffsets.map { abs(it - medianOffset) }.sorted()
            val mad = mads[mads.size / 2]
            val maxMadDist = 3.0 * mad + 1.5

            val inlierPoints = mutableListOf<Point>()
            for (i in samplePoints.indices) {
                if (abs(sampleOffsets[i] - medianOffset) <= maxMadDist) {
                    inlierPoints.add(samplePoints[i])
                }
            }

            if (inlierPoints.size < 8) {
                fittedLines.add(Line2D(p1, p2))
            } else {
                // Robust least squares fit
                val fitted = fitLineThroughPoints(inlierPoints)
                fittedLines.add(fitted ?: Line2D(p1, p2))
            }
        }

        // Intersect adjacent lines to get 4 refined corners
        val refinedCorners = mutableListOf<Point>()
        for (i in 0 until 4) {
            val l1 = fittedLines[i] ?: Line2D(initialQuad[i], initialQuad[(i + 1) % 4])
            val l0 = fittedLines[(i + 3) % 4] ?: Line2D(initialQuad[(i + 3) % 4], initialQuad[i])
            val inter = intersectLines(l0, l1) ?: initialQuad[i]
            refinedCorners.add(inter)
        }

        // Safety check: if any corner moved > 4% diagonal, reject refinement
        val maxAllowedShift = diag * ScanTuning.REFINEMENT_MAX_DEVIATION_DIAGONAL_RATIO
        for (i in 0 until 4) {
            val shift = hypot(refinedCorners[i].x - initialQuad[i].x, refinedCorners[i].y - initialQuad[i].y)
            if (shift > maxAllowedShift) {
                return initialQuad // Revert to unrefined
            }
        }

        return refinedCorners
    }

    private data class Line2D(val p1: Point, val p2: Point)

    private fun fitLineThroughPoints(points: List<Point>): Line2D? {
        val mat = Mat(points.size, 1, CvType.CV_32FC2)
        val data = FloatArray(points.size * 2)
        for (i in points.indices) {
            data[i * 2] = points[i].x.toFloat()
            data[i * 2 + 1] = points[i].y.toFloat()
        }
        mat.put(0, 0, data)

        val lineParams = Mat()
        Imgproc.fitLine(mat, lineParams, Imgproc.DIST_HUBER, 0.0, 0.01, 0.01)
        mat.release()

        val vx = lineParams.get(0, 0)[0]
        val vy = lineParams.get(1, 0)[0]
        val x0 = lineParams.get(2, 0)[0]
        val y0 = lineParams.get(3, 0)[0]
        lineParams.release()

        val p1 = Point(x0 - vx * 1000.0, y0 - vy * 1000.0)
        val p2 = Point(x0 + vx * 1000.0, y0 + vy * 1000.0)
        return Line2D(p1, p2)
    }

    private fun intersectLines(l1: Line2D, l2: Line2D): Point? {
        val p1 = l1.p1
        val p2 = l1.p2
        val p3 = l2.p1
        val p4 = l2.p2

        val d = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x)
        if (abs(d) < 1e-4) return null
        val xi = ((p1.x * p2.y - p1.y * p2.x) * (p3.x - p4.x) - (p1.x - p2.x) * (p3.x * p4.y - p3.y * p4.x)) / d
        val yi = ((p1.x * p2.y - p1.y * p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x * p4.y - p3.y * p4.x)) / d
        return Point(xi, yi)
    }
}
