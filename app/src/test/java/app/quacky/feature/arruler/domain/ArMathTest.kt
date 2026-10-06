package app.quacky.feature.arruler.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ArMathTest {

    @Test
    fun testDistance3D() {
        // 3-4-5 triangle in XY plane
        val p1 = Vector3(0f, 0f, 0f)
        val p2 = Vector3(3f, 4f, 0f)
        assertEquals(5.0f, ArMath.distance(p1, p2), 0.001f)

        // 1-2-2 vector (length = sqrt(1 + 4 + 4) = 3)
        val p3 = Vector3(1f, 2f, 2f)
        assertEquals(3.0f, ArMath.distance(p1, p3), 0.001f)

        // Known 30cm object
        val start = Vector3(0.1f, 0.5f, -1.2f)
        val end = Vector3(0.4f, 0.5f, -1.2f)
        assertEquals(0.3f, ArMath.distance(start, end), 0.001f)
    }

    @Test
    fun testPathDistance() {
        val path = listOf(
            Vector3(0f, 0f, 0f),
            Vector3(1f, 0f, 0f),
            Vector3(1f, 2f, 0f),
            Vector3(1f, 2f, 2f)
        )
        // Segments: 1 + 2 + 2 = 5
        assertEquals(5.0f, ArMath.pathDistance(path), 0.001f)
    }

    @Test
    fun testHeightDistance() {
        val floor = Vector3(0.2f, -0.85f, -1.5f)
        val ceiling = Vector3(0.25f, 1.65f, -1.55f)
        // Vertical delta: abs(1.65 - (-0.85)) = 2.50m
        assertEquals(2.50f, ArMath.heightDistance(floor, ceiling), 0.001f)
    }

    @Test
    fun testAngleDegrees() {
        val vertex = Vector3(0f, 0f, 0f)

        // Right angle (90 degrees)
        val a = Vector3(1f, 0f, 0f)
        val b = Vector3(0f, 1f, 0f)
        assertEquals(90.0, ArMath.angleDegrees(a, vertex, b), 0.01)

        // Straight line (180 degrees)
        val c = Vector3(-1f, 0f, 0f)
        assertEquals(180.0, ArMath.angleDegrees(a, vertex, c), 0.01)

        // 45 degree angle
        val d = Vector3(1f, 1f, 0f)
        assertEquals(45.0, ArMath.angleDegrees(a, vertex, d), 0.01)
    }

    @Test
    fun testFormatMeasurement() {
        assertEquals("30.0 cm", ArMath.formatMeasurement(0.3f, ArUnit.CM))
        assertEquals("300 mm", ArMath.formatMeasurement(0.3f, ArUnit.MM))
        assertEquals("1.25 m", ArMath.formatMeasurement(1.25f, ArUnit.M))
        assertEquals("39.4 in", ArMath.formatMeasurement(1.0f, ArUnit.IN))
        assertEquals("6 ft 0 in", ArMath.formatMeasurement(1.8288f, ArUnit.FT_IN))
        assertEquals("5 ft 4 in", ArMath.formatMeasurement(1.6256f, ArUnit.FT_IN))
    }

    @Test
    fun testProjectWorldToScreen() {
        // Standard identity view matrix
        val viewMatrix = FloatArray(16) { i -> if (i % 5 == 0) 1f else 0f }

        // Simple perspective projection matrix (focal length = 1, aspect = 1, near = 0.1, far = 100)
        val projMatrix = FloatArray(16)
        projMatrix[0] = 1f   // [0,0]
        projMatrix[5] = 1f   // [1,1]
        projMatrix[10] = -1f // [2,2]
        projMatrix[11] = -1f // [2,3]
        projMatrix[14] = -0.2f // [3,2]

        val screenW = 1080f
        val screenH = 1920f

        // Point directly in front of camera along -Z axis
        val inFront = Vector3(0f, 0f, -2f)
        val projected = ArMath.projectWorldToScreen(inFront, viewMatrix, projMatrix, screenW, screenH)
        assertNotNull(projected)
        assertEquals(540f, projected!!.x, 1f)
        assertEquals(960f, projected.y, 1f)

        // Point behind camera (+Z)
        val behind = Vector3(0f, 0f, 2f)
        val projectedBehind = ArMath.projectWorldToScreen(behind, viewMatrix, projMatrix, screenW, screenH)
        assertNull(projectedBehind)

        // Off-axis points (to the left and above)
        val leftAbove = Vector3(-0.5f, 0.5f, -2f)
        val projectedLeft = ArMath.projectWorldToScreen(leftAbove, viewMatrix, projMatrix, screenW, screenH)
        assertNotNull(projectedLeft)
        assertEquals(405f, projectedLeft!!.x, 1f)
        assertEquals(720f, projectedLeft.y, 1f)
    }

    @Test
    fun testArMathEdgeCases() {
        // Path with fewer than 2 points returns 0
        assertEquals(0f, ArMath.pathDistance(emptyList()), 0.001f)
        assertEquals(0f, ArMath.pathDistance(listOf(Vector3(1f, 2f, 3f))), 0.001f)

        // Degenerate vector angle returns 0.0
        val p = Vector3(1f, 1f, 1f)
        assertEquals(0.0, ArMath.angleDegrees(p, p, p), 0.001)

        // Matrix multiplication with identity matrix
        val identity = FloatArray(16) { i -> if (i % 5 == 0) 1f else 0f }
        val testMat = FloatArray(16) { i -> (i + 1).toFloat() }
        val result = ArMath.multiplyMatrix(testMat, identity)
        for (i in 0..15) {
            assertEquals(testMat[i], result[i], 0.0001f)
        }
    }
}
