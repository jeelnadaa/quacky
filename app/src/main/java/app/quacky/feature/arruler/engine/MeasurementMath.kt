package app.quacky.feature.arruler.engine

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.sqrt

data class DistanceMeasurement(
    val lengthMeters: Double,
    val pointA: Vec3,
    val pointB: Vec3
)

data class HeightMeasurement(
    val verticalHeightMeters: Double,
    val straightDistanceMeters: Double,
    val bottomPoint: Vec3,
    val topPoint: Vec3
)

data class AngleMeasurement(
    val degrees: Double,
    val vertex: Vec3,
    val armA: Vec3,
    val armB: Vec3,
    val isValid: Boolean
)

data class PathMeasurement(
    val totalLengthMeters: Double,
    val segmentLengthsMeters: List<Double>,
    val points: List<Vec3>
)

data class PolygonMeasurement(
    val perimeterMeters: Double,
    val areaSqMeters: Double,
    val isPlanar: Boolean,
    val points: List<Vec3>
)

object MeasurementMath {

    fun computeDistance(
        p1: Vec3,
        p2: Vec3,
        scaleFactor: Double = 1.0
    ): DistanceMeasurement {
        val dist = (p2 - p1).length() * scaleFactor
        return DistanceMeasurement(
            lengthMeters = dist,
            pointA = p1,
            pointB = p2
        )
    }

    fun computeHeight(
        p1: Vec3,
        p2: Vec3,
        floorY: Double? = null,
        scaleFactor: Double = 1.0
    ): HeightMeasurement {
        var startY = p1.y
        if (floorY != null && abs(p1.y - floorY) <= MeasureTuning.FLOOR_PROXIMITY_METERS) {
            startY = floorY
        }
        val verticalHeight = abs(p2.y - startY) * scaleFactor
        val straightDist = (p2 - p1).length() * scaleFactor

        val (bottom, top) = if (p1.y <= p2.y) p1 to p2 else p2 to p1

        return HeightMeasurement(
            verticalHeightMeters = verticalHeight,
            straightDistanceMeters = straightDist,
            bottomPoint = bottom,
            topPoint = top
        )
    }

    fun computeAngle(
        vertex: Vec3,
        armA: Vec3,
        armB: Vec3
    ): AngleMeasurement {
        val ba = armA - vertex
        val bc = armB - vertex
        val lenA = ba.length()
        val lenB = bc.length()

        if (lenA < 0.03 || lenB < 0.03) {
            return AngleMeasurement(
                degrees = 0.0,
                vertex = vertex,
                armA = armA,
                armB = armB,
                isValid = false
            )
        }

        val dot = (ba.dot(bc) / (lenA * lenB)).coerceIn(-1.0, 1.0)
        val rad = acos(dot)
        val deg = Math.toDegrees(rad)

        return AngleMeasurement(
            degrees = deg,
            vertex = vertex,
            armA = armA,
            armB = armB,
            isValid = true
        )
    }

    fun computePath(
        points: List<Vec3>,
        scaleFactor: Double = 1.0
    ): PathMeasurement {
        if (points.size < 2) {
            return PathMeasurement(0.0, emptyList(), points)
        }

        val segments = mutableListOf<Double>()
        var total = 0.0

        for (i in 0 until points.size - 1) {
            val segLen = (points[i + 1] - points[i]).length() * scaleFactor
            segments.add(segLen)
            total += segLen
        }

        return PathMeasurement(
            totalLengthMeters = total,
            segmentLengthsMeters = segments,
            points = points
        )
    }

    /**
     * Computes polygon area using Newell's method in 3D.
     */
    fun computePolygon(
        points: List<Vec3>,
        scaleFactor: Double = 1.0
    ): PolygonMeasurement {
        val n = points.size
        if (n < 3) {
            return PolygonMeasurement(0.0, 0.0, false, points)
        }

        // 1. Perimeter
        var perimeter = 0.0
        for (i in 0 until n) {
            val next = (i + 1) % n
            perimeter += (points[next] - points[i]).length()
        }
        perimeter *= scaleFactor

        // 2. Newell's method for area normal vector
        var nx = 0.0
        var ny = 0.0
        var nz = 0.0

        for (i in 0 until n) {
            val curr = points[i]
            val next = points[(i + 1) % n]

            nx += (curr.y - next.y) * (curr.z + next.z)
            ny += (curr.z - next.z) * (curr.x + next.x)
            nz += (curr.x - next.x) * (curr.y + next.y)
        }

        val normalLength = sqrt(nx * nx + ny * ny + nz * nz)
        val rawArea = 0.5 * normalLength
        val area = rawArea * scaleFactor * scaleFactor

        // 3. Planarity check: plane through centroid with Newell normal
        val centroid = points.fold(Vec3.ZERO) { acc, p -> acc + p } / n.toDouble()
        val planeNormal = if (normalLength > 1e-6) Vec3(nx / normalLength, ny / normalLength, nz / normalLength) else Vec3.UP

        val maxDeviation = points.maxOfOrNull { abs((it - centroid).dot(planeNormal)) } ?: 0.0
        val isPlanar = maxDeviation <= 0.01 // Within 1 cm

        return PolygonMeasurement(
            perimeterMeters = perimeter,
            areaSqMeters = area,
            isPlanar = isPlanar,
            points = points
        )
    }
}
