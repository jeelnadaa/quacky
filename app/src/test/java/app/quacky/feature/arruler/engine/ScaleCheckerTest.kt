package app.quacky.feature.arruler.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleCheckerTest {

    @Test
    fun `calibrate accepts scaling within 10 percent bounds`() {
        val checker = ScaleChecker()

        // 85.6 mm card measured as 84.0 mm -> k = 85.6 / 84.0 = 1.019 (valid)
        val res = checker.calibrate(trueLengthMeters = 0.0856, measuredLengthMeters = 0.0840)
        assertTrue(res.success)
        assertTrue(checker.isCalibrated)
        assertEquals(0.0856 / 0.0840, checker.currentScaleFactor, 1e-4)

        // Apply scale
        val scaled = checker.applyScale(1.0)
        assertEquals(0.0856 / 0.0840, scaled, 1e-4)
    }

    @Test
    fun `calibrate rejects measurements outside 0 point 90 to 1 point 10 range`() {
        val checker = ScaleChecker()

        // Measured 2x too small -> k = 2.0 (rejected)
        val res = checker.calibrate(trueLengthMeters = 0.10, measuredLengthMeters = 0.05)
        assertFalse(res.success)
        assertEquals(1.0, checker.currentScaleFactor, 1e-4)
    }
}
