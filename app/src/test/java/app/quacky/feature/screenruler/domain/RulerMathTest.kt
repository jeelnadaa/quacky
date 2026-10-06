package app.quacky.feature.screenruler.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RulerMathTest {

    @Test
    fun `test pixels per mm and inch calculations`() {
        val baseDpi = 254f // 254 dpi means 10 px per mm
        val factor = 1.0f

        val pxPerMm = RulerMath.pixelsPerMm(baseDpi, factor)
        val pxPerInch = RulerMath.pixelsPerInch(baseDpi, factor)

        assertEquals(10f, pxPerMm, 0.001f)
        assertEquals(254f, pxPerInch, 0.001f)
    }

    @Test
    fun `test distance calculation with calibration factor`() {
        val baseDpi = 254f // 10 px/mm nominal
        val factor = 1.1f // 11 px/mm calibrated

        val deltaPx = 110f
        val distanceMm = RulerMath.calculateDistanceMm(deltaPx, baseDpi, factor)

        // 110 px / 11 px per mm = 10 mm
        assertEquals(10f, distanceMm, 0.001f)
    }

    @Test
    fun `test fractional inch formatting`() {
        assertEquals("0\"", RulerMath.formatFractionalInches(0f))
        assertEquals("1\"", RulerMath.formatFractionalInches(1.0f))
        assertEquals("1/2\"", RulerMath.formatFractionalInches(0.5f))
        assertEquals("1/4\"", RulerMath.formatFractionalInches(0.25f))
        assertEquals("3/4\"", RulerMath.formatFractionalInches(0.75f))
        assertEquals("1/8\"", RulerMath.formatFractionalInches(0.125f))
        assertEquals("1 5/16\"", RulerMath.formatFractionalInches(1.3125f))
        assertEquals("7/16\"", RulerMath.formatFractionalInches(0.4375f))
    }

    @Test
    fun `test format distance string`() {
        val mm = 50.0f
        assertEquals("50.0 mm", RulerMath.formatDistance(mm, RulerUnit.MM))
        assertEquals("5.00 cm", RulerMath.formatDistance(mm, RulerUnit.CM))
        val inchStr = RulerMath.formatDistance(25.4f, RulerUnit.INCH)
        assertTrue(inchStr.contains("1.00 in"))
        assertTrue(inchStr.contains("1\""))
    }
}
