package app.quacky.feature.arruler.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArMultiMeasurementTest {

    @Test
    fun defaultMeasurementHasCorrectNamingAndTone() {
        val m1 = ArSingleMeasurement(index = 1, name = "M1", colorToneIndex = 0)
        assertEquals("M1", m1.name)
        assertEquals(0, m1.colorToneIndex)
        assertFalse(m1.isFinished)

        val m2 = ArSingleMeasurement(index = 2, name = "M2", colorToneIndex = 1)
        assertEquals("M2", m2.name)
        assertEquals(1, m2.colorToneIndex)
    }

    @Test
    fun tonePaletteContainsFiveDistinctColors() {
        assertEquals(5, MeasurementTones.size)
        // Verify distinct tones
        val distinctTones = MeasurementTones.toSet()
        assertEquals(5, distinctTones.size)
    }

    @Test
    fun displayValueChoosesAngleWhenInAngleMode() {
        val angleM = ArSingleMeasurement(
            index = 1,
            mode = ArRulerMode.ANGLE,
            formattedValue = "0.0 cm",
            angleDegrees = 45.2,
            formattedAngle = "45.2°"
        )
        assertEquals("45.2°", angleM.displayValue)

        val distM = ArSingleMeasurement(
            index = 2,
            mode = ArRulerMode.DISTANCE,
            formattedValue = "124 cm"
        )
        assertEquals("124 cm", distM.displayValue)
    }

    @Test
    fun sessionDetailContainsAllMeasurements() {
        val detail = ArSessionDetail(
            sessionId = "sess-123",
            sessionName = "Living room",
            timestamp = 1000L,
            coverScreenshotPath = "/path/to/cover.png",
            measurements = listOf(
                ArSingleMeasurement(index = 1, name = "M1", formattedValue = "120 cm"),
                ArSingleMeasurement(index = 2, name = "M2", formattedValue = "240 cm")
            )
        )
        assertEquals("Living room", detail.sessionName)
        assertEquals(2, detail.measurements.size)
        assertEquals("120 cm", detail.measurements[0].formattedValue)
    }
}
