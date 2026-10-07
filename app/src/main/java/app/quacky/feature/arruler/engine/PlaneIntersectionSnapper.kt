package app.quacky.feature.arruler.engine

import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import kotlin.math.abs
import kotlin.math.acos

data class PlaneSnapResult(
    val snappedPoint: Vec3,
    val label: String
)

class PlaneIntersectionSnapper {

    data class PlaneData(
        val normal: Vec3,
        val point: Vec3, // Point on plane
        val extent: Double
    )

    fun checkSnap(
        aimPoint: Vec3,
        cameraPosition: Vec3,
        rayDirection: Vec3,
        planes: List<Plane>
    ): PlaneSnapResult? {
        val eligible = mutableListOf<PlaneData>()

        for (plane in planes) {
            if (plane.subsumedBy != null || plane.trackingState != TrackingState.TRACKING) continue
            val extent = (plane.extentX * plane.extentZ).toDouble()
            if (extent < 0.5) continue

            val center = plane.centerPose
            val pPt = Vec3(center.tx().toDouble(), center.ty().toDouble(), center.tz().toDouble())
            val normalAxis = center.yAxis
            val pNorm = Vec3(normalAxis[0].toDouble(), normalAxis[1].toDouble(), normalAxis[2].toDouble()).normalized()

            // Distance from aim point to plane: |(aimPoint - pPt) . pNorm|
            val distToPlane = abs((aimPoint - pPt).dot(pNorm))
            if (distToPlane <= MeasureTuning.SNAP_PLANES_MAX_DISTANCE_METERS) {
                eligible.add(PlaneData(pNorm, pPt, extent))
            }
        }

        if (eligible.size < 2) return null

        // 1. Try 3-plane intersection (room corner vertex)
        if (eligible.size >= 3) {
            for (i in 0 until eligible.size - 2) {
                for (j in i + 1 until eligible.size - 1) {
                    for (k in j + 1 until eligible.size) {
                        val p1 = eligible[i]
                        val p2 = eligible[j]
                        val p3 = eligible[k]

                        val n1 = p1.normal
                        val n2 = p2.normal
                        val n3 = p3.normal

                        // Determinant of [n1, n2, n3] = n1 . (n2 x n3)
                        val det = n1.dot(n2.cross(n3))
                        if (abs(det) > MeasureTuning.SNAP_THREE_PLANES_MIN_DET) {
                            val d1 = n1.dot(p1.point)
                            val d2 = n2.dot(p2.point)
                            val d3 = n3.dot(p3.point)

                            // Vertex = (d1 * (n2 x n3) + d2 * (n3 x n1) + d3 * (n1 x n2)) / det
                            val vertex = (n2.cross(n3) * d1 + n3.cross(n1) * d2 + n1.cross(n2) * d3) / det
                            if ((vertex - aimPoint).length() <= MeasureTuning.SNAP_THREE_PLANES_VERTEX_MAX_DIST_METERS) {
                                return PlaneSnapResult(vertex, "Corner")
                            }
                        }
                    }
                }
            }
        }

        // 2. Try 2-plane intersection (wall-floor / wall-wall line)
        for (i in 0 until eligible.size - 1) {
            for (j in i + 1 until eligible.size) {
                val p1 = eligible[i]
                val p2 = eligible[j]

                val n1 = p1.normal
                val n2 = p2.normal
                val dotNormals = n1.dot(n2).coerceIn(-1.0, 1.0)
                val angleDeg = Math.toDegrees(acos(abs(dotNormals)))

                if (angleDeg >= MeasureTuning.SNAP_PLANES_MIN_ANGLE_DEG) {
                    // Line direction = n1 x n2
                    val lineDir = n1.cross(n2).normalized()
                    if (lineDir.lengthSquared() > 1e-4) {
                        // Point on line: solve n1 . x = d1, n2 . x = d2, lineDir . x = 0
                        val d1 = n1.dot(p1.point)
                        val d2 = n2.dot(p2.point)
                        val det2 = 1.0 - dotNormals * dotNormals
                        if (det2 > 1e-4) {
                            val c1 = (d1 - dotNormals * d2) / det2
                            val c2 = (d2 - dotNormals * d1) / det2
                            val pOnLine = n1 * c1 + n2 * c2

                            // Closest point on line to aimPoint
                            val t = (aimPoint - pOnLine).dot(lineDir)
                            val closestPtOnLine = pOnLine + lineDir * t

                            if ((closestPtOnLine - aimPoint).length() <= MeasureTuning.SNAP_TWO_PLANES_LINE_MAX_DIST_METERS) {
                                return PlaneSnapResult(closestPtOnLine, "Corner line")
                            }
                        }
                    }
                }
            }
        }

        return null
    }
}
