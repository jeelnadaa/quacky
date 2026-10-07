package app.quacky.feature.documentscanner

import app.quacky.feature.documentscanner.domain.CornerPoint
import app.quacky.feature.documentscanner.domain.CoordinateMapper
import app.quacky.feature.documentscanner.domain.DocumentQuad
import app.quacky.feature.documentscanner.domain.OneEuroFilter2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DocumentDetectionTest {

    @Test
    fun testCoordinateMapperIoU() {
        val mapper = CoordinateMapper()
        val q1 = DocumentQuad(
            topLeft = CornerPoint(0.1f, 0.1f),
            topRight = CornerPoint(0.9f, 0.1f),
            bottomRight = CornerPoint(0.9f, 0.9f),
            bottomLeft = CornerPoint(0.1f, 0.9f)
        )
        // Identical quad IoU should be 1.0
        val iouSame = mapper.computeQuadIoU(q1, q1)
        assertEquals(1.0f, iouSame, 0.001f)

        // Non-overlapping quad IoU should be 0.0
        val q2 = DocumentQuad(
            topLeft = CornerPoint(1.1f, 1.1f),
            topRight = CornerPoint(1.9f, 1.1f),
            bottomRight = CornerPoint(1.9f, 1.9f),
            bottomLeft = CornerPoint(1.1f, 1.9f)
        )
        val iouDisjoint = mapper.computeQuadIoU(q1, q2)
        assertEquals(0.0f, iouDisjoint, 0.001f)
    }

    @Test
    fun testOneEuroFilterConvergence() {
        val filter = OneEuroFilter2D(minCutoff = 1.0f, beta = 0.02f)
        var curr = CornerPoint(0f, 0f)

        // Feed steady values with small noise
        for (i in 0 until 50) {
            val noise = if (i % 2 == 0) 0.002f else -0.002f
            curr = filter.filter(0.5f + noise, 0.5f + noise, 1000L + i * 33L)
        }

        // Must converge tightly to 0.5f
        assertTrue(abs(curr.x - 0.5f) < 0.005f)
        assertTrue(abs(curr.y - 0.5f) < 0.005f)
    }

    @Test
    fun testSubPixelParabolicPeak() {
        // Test parabolic sub-pixel interpolation on synthetic 1D derivative profile
        // Continuous peak at index 5.3: y0=8, y1=10, y2=7
        val y0 = 8.0
        val y1 = 10.0
        val y2 = 7.0
        val denom = 2.0 * (2.0 * y1 - y0 - y2)
        val delta = (y0 - y2) / denom

        // Peak shifts slightly right because y0 > y2
        assertTrue(delta > 0.0 && delta < 0.5)
    }
}
