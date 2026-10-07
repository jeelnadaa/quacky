package app.quacky.feature.arruler.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import java.util.Random

/**
 * Pure Kotlin RANSAC + PCA plane fitting on weighted 3D points.
 */
class PlaneFitter {

    data class PlaneFitResult(
        val normal: Vec3,
        val offset: Double,          // plane equation: normal . x - offset = 0
        val inlierRatio: Double,
        val rmsResidual: Double,
        val isPlanar: Boolean
    )

    fun fitPlane(
        samples: List<WeightedSample>,
        cameraPosition: Vec3 = Vec3.ZERO,
        medianDistance: Double = 1.0,
        randomSeed: Long = 42L
    ): PlaneFitResult? {
        if (samples.size < 3) return null

        val tau = max(
            MeasureTuning.RANSAC_BASE_THRESHOLD_METERS,
            MeasureTuning.RANSAC_DISTANCE_SCALE * medianDistance
        )

        val totalWeight = samples.sumOf { it.weight }
        if (totalWeight <= 0.0) return null

        val rng = Random(randomSeed)
        var bestInliers = emptyList<WeightedSample>()
        var bestInlierWeight = 0.0
        var bestNormal = Vec3.UP
        var bestOffset = 0.0

        val n = samples.size

        // 1. RANSAC 48 iterations
        for (iter in 0 until MeasureTuning.RANSAC_ITERATIONS) {
            val i1 = rng.nextInt(n)
            var i2 = rng.nextInt(n)
            while (i2 == i1) i2 = rng.nextInt(n)
            var i3 = rng.nextInt(n)
            while (i3 == i1 || i3 == i2) i3 = rng.nextInt(n)

            val p1 = samples[i1].point
            val p2 = samples[i2].point
            val p3 = samples[i3].point

            val v1 = p2 - p1
            val v2 = p3 - p1
            val cross = v1.cross(v2)
            if (cross.lengthSquared() < 1e-10) continue // Collinear

            val candNormal = cross.normalized()
            val candOffset = candNormal.dot(p1)

            var inlierWeight = 0.0
            val inliers = mutableListOf<WeightedSample>()

            for (s in samples) {
                val dist = abs(candNormal.dot(s.point) - candOffset)
                if (dist <= tau) {
                    inlierWeight += s.weight
                    inliers.add(s)
                }
            }

            if (inlierWeight > bestInlierWeight) {
                bestInlierWeight = inlierWeight
                bestInliers = inliers
                bestNormal = candNormal
                bestOffset = candOffset
            }
        }

        if (bestInliers.size < 3) return null

        // 2. PCA Refinement on inliers (twice)
        var currentInliers = bestInliers
        var refinedNormal = bestNormal
        var refinedOffset = bestOffset

        for (pass in 0..1) {
            val sumW = currentInliers.sumOf { it.weight }
            if (sumW <= 0.0) break

            var cx = 0.0
            var cy = 0.0
            var cz = 0.0
            for (s in currentInliers) {
                cx += s.weight * s.point.x
                cy += s.weight * s.point.y
                cz += s.weight * s.point.z
            }
            val centroid = Vec3(cx / sumW, cy / sumW, cz / sumW)

            // 3x3 weighted covariance matrix
            var c00 = 0.0
            var c01 = 0.0
            var c02 = 0.0
            var c11 = 0.0
            var c12 = 0.0
            var c22 = 0.0

            for (s in currentInliers) {
                val dx = s.point.x - centroid.x
                val dy = s.point.y - centroid.y
                val dz = s.point.z - centroid.z
                val w = s.weight
                c00 += w * dx * dx
                c01 += w * dx * dy
                c02 += w * dx * dz
                c11 += w * dy * dy
                c12 += w * dy * dz
                c22 += w * dz * dz
            }

            val cov = doubleArrayOf(
                c00, c01, c02,
                c01, c11, c12,
                c02, c12, c22
            )

            // Eigen decomposition: eigenvector for smallest eigenvalue
            val normal = findSmallestEigenvectorSymmetric3x3(cov)
            refinedNormal = normal
            refinedOffset = normal.dot(centroid)

            // Re-evaluate inliers
            currentInliers = samples.filter { abs(refinedNormal.dot(it.point) - refinedOffset) <= tau }
        }

        // 3. Orient normal toward camera
        val centroid = Vec3(
            currentInliers.map { it.point.x }.average(),
            currentInliers.map { it.point.y }.average(),
            currentInliers.map { it.point.z }.average()
        )
        val toCam = cameraPosition - centroid
        if (refinedNormal.dot(toCam) < 0.0) {
            refinedNormal = -refinedNormal
            refinedOffset = -refinedOffset
        }

        // 4. Inlier ratio and RMS residual
        val inlierWeight = currentInliers.sumOf { it.weight }
        val inlierRatio = (inlierWeight / totalWeight).coerceIn(0.0, 1.0)

        var sumSqResidual = 0.0
        for (s in currentInliers) {
            val res = refinedNormal.dot(s.point) - refinedOffset
            sumSqResidual += s.weight * res * res
        }
        val rms = if (inlierWeight > 0.0) sqrt(sumSqResidual / inlierWeight) else 1.0

        val isPlanar = (inlierRatio >= MeasureTuning.PLANE_MIN_INLIER_RATIO) &&
            (rms <= MeasureTuning.PLANE_MAX_RMS_RATIO * tau)

        return PlaneFitResult(
            normal = refinedNormal,
            offset = refinedOffset,
            inlierRatio = inlierRatio,
            rmsResidual = rms,
            isPlanar = isPlanar
        )
    }

    /**
     * Cyclic Jacobi rotation solver for 3x3 symmetric matrix.
     * Returns the unit eigenvector corresponding to the smallest eigenvalue.
     */
    private fun findSmallestEigenvectorSymmetric3x3(a: DoubleArray): Vec3 {
        // A is 3x3 symmetric in row-major order: indices 0..8
        val A = a.clone()
        val V = doubleArrayOf(
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 1.0
        )

        for (iter in 0 until 20) {
            var maxOff = 0.0
            var p = 0
            var q = 1

            if (abs(A[1]) > maxOff) { maxOff = abs(A[1]); p = 0; q = 1 }
            if (abs(A[2]) > maxOff) { maxOff = abs(A[2]); p = 0; q = 2 }
            if (abs(A[5]) > maxOff) { maxOff = abs(A[5]); p = 1; q = 2 }

            if (maxOff < 1e-12) break

            val app = A[p * 3 + p]
            val aqq = A[q * 3 + q]
            val apq = A[p * 3 + q]

            val phi = 0.5 * kotlin.math.atan2(2.0 * apq, aqq - app)
            val c = kotlin.math.cos(phi)
            val s = kotlin.math.sin(phi)

            // Update A
            val newApp = c * c * app - 2.0 * s * c * apq + s * s * aqq
            val newAqq = s * s * app + 2.0 * s * c * apq + c * c * aqq
            A[p * 3 + p] = newApp
            A[q * 3 + q] = newAqq
            A[p * 3 + q] = 0.0
            A[q * 3 + p] = 0.0

            val other = 3 - p - q
            val aip = A[other * 3 + p]
            val aiq = A[other * 3 + q]
            A[other * 3 + p] = c * aip - s * aiq
            A[p * 3 + other] = A[other * 3 + p]
            A[other * 3 + q] = s * aip + c * aiq
            A[q * 3 + other] = A[other * 3 + q]

            // Update eigenvectors V
            for (i in 0 until 3) {
                val vip = V[i * 3 + p]
                val viq = V[i * 3 + q]
                V[i * 3 + p] = c * vip - s * viq
                V[i * 3 + q] = s * vip + c * viq
            }
        }

        // Eigenvalues on diagonal
        val e0 = A[0]
        val e1 = A[4]
        val e2 = A[8]

        val minCol = when {
            e0 <= e1 && e0 <= e2 -> 0
            e1 <= e0 && e1 <= e2 -> 1
            else -> 2
        }

        return Vec3(V[minCol], V[3 + minCol], V[6 + minCol]).normalized()
    }
}
