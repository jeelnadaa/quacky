package app.quacky.feature.arruler.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MeasurementMathTest {

    @Test
    fun `computeDistance calculates correct Euclidean distance and scales`() {
        val p1 = Vec3(0.0, 0.0, 0.0)
        val p2 = Vec3(3.0, 4.0, 0.0) // 5.0m
        val dist = MeasurementMath.computeDistance(p1, p2, scaleFactor = 1.0)
        assertEquals(5.0, dist.lengthMeters, 1e-4)

        val scaled = MeasurementMath.computeDistance(p1, p2, scaleFactor = 1.02)
        assertEquals(5.10, scaled.lengthMeters, 1e-4)
    }

    @Test
    fun `computeHeight snaps floor when close and computes vertical delta`() {
        val floorPoint = Vec3(0.0, -0.98, -1.0)
        val ceilingPoint = Vec3(0.0, 1.50, -1.0)

        // floorY known at -1.00m (within 0.10m proximity threshold)
        val height = MeasurementMath.computeHeight(
            p1 = floorPoint,
            p2 = ceilingPoint,
            floorY = -1.00
        )

        // Vertical height should snap from -1.00 to 1.50 = 2.50m
        assertEquals(2.50, height.verticalHeightMeters, 1e-4)
    }

    @Test
    fun `computeAngle calculates correct degrees and rejects short arms`() {
        // 90 degree angle
        val vertex = Vec3(0.0, 0.0, 0.0)
        val armA = Vec3(1.0, 0.0, 0.0)
        val armB = Vec3(0.0, 1.0, 0.0)

        val angle90 = MeasurementMath.computeAngle(vertex, armA, armB)
        assertTrue(angle90.isValid)
        assertEquals(90.0, angle90.degrees, 1e-3)

        // 45 degree angle
        val arm45 = Vec3(1.0, 1.0, 0.0)
        val angle45 = MeasurementMath.computeAngle(vertex, armA, arm45)
        assertTrue(angle45.isValid)
        assertEquals(45.0, angle45.degrees, 1e-3)

        // Too short arm (< 3 cm)
        val shortArm = Vec3(0.01, 0.0, 0.0)
        val invalid = MeasurementMath.computeAngle(vertex, shortArm, armB)
        assertFalse(invalid.isValid)
    }

    @Test
    fun `computePath sums consecutive segments accurately`() {
        val pts = listOf(
            Vec3(0.0, 0.0, 0.0),
            Vec3(1.0, 0.0, 0.0), // 1.0m
            Vec3(1.0, 2.0, 0.0), // 2.0m
            Vec3(1.0, 2.0, 2.0)  // 2.0m
        )
        val path = MeasurementMath.computePath(pts)
        assertEquals(5.0, path.totalLengthMeters, 1e-4)
        assertEquals(3, path.segmentLengthsMeters.size)
    }

    @Test
    fun `computePolygon calculates Newell 3D area on planar rectangle`() {
        // 2m x 3m rectangle on z = -1.0 plane -> Area = 6.0 m^2, Perimeter = 10.0 m
        val pts = listOf(
            Vec3(0.0, 0.0, -1.0),
            Vec3(2.0, 0.0, -1.0),
            Vec3(2.0, 3.0, -1.0),
            Vec3(0.0, 3.0, -1.0)
        )
        val poly = MeasurementMath.computePolygon(pts)
        assertTrue(poly.isPlanar)
        assertEquals(6.0, poly.areaSqMeters, 1e-4)
        assertEquals(10.0, poly.perimeterMeters, 1e-4)
    }
}
