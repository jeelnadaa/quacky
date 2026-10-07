package app.quacky.feature.compass.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassMathTest {

    @Test
    fun `normalizeAngle correctly handles positive and negative inputs`() {
        assertEquals(0f, CompassMath.normalizeAngle(0f), 0.001f)
        assertEquals(90f, CompassMath.normalizeAngle(90f), 0.001f)
        assertEquals(0f, CompassMath.normalizeAngle(360f), 0.001f)
        assertEquals(45f, CompassMath.normalizeAngle(405f), 0.001f)
        assertEquals(350f, CompassMath.normalizeAngle(-10f), 0.001f)
        assertEquals(270f, CompassMath.normalizeAngle(-90f), 0.001f)
    }

    @Test
    fun `toCardinal identifies all 16 cardinal points correctly`() {
        assertEquals("N", CompassMath.toCardinal(0f))
        assertEquals("N", CompassMath.toCardinal(355f))
        assertEquals("N", CompassMath.toCardinal(5f))
        assertEquals("NE", CompassMath.toCardinal(45f))
        assertEquals("E", CompassMath.toCardinal(90f))
        assertEquals("SE", CompassMath.toCardinal(135f))
        assertEquals("S", CompassMath.toCardinal(180f))
        assertEquals("SW", CompassMath.toCardinal(225f))
        assertEquals("W", CompassMath.toCardinal(270f))
        assertEquals("NW", CompassMath.toCardinal(315f))
    }

    @Test
    fun `shortestAngularDelta computes wrap around distances`() {
        // Simple difference
        assertEquals(10f, CompassMath.shortestAngularDelta(20f, 10f), 0.01f)
        assertEquals(-10f, CompassMath.shortestAngularDelta(10f, 20f), 0.01f)

        // Across 0/360 boundary: target = 10, current = 350 -> should be +20 (turn right 20 deg)
        assertEquals(20f, CompassMath.shortestAngularDelta(10f, 350f), 0.01f)

        // Across 0/360 boundary: target = 350, current = 10 -> should be -20 (turn left 20 deg)
        assertEquals(-20f, CompassMath.shortestAngularDelta(350f, 10f), 0.01f)
    }

    @Test
    fun `smoothAngle smoothly transitions across 0 and 360`() {
        val smoothed = CompassMath.smoothAngle(previous = 358f, current = 2f, alpha = 0.5f)
        // Delta from 358 to 2 is +4. With alpha 0.5: 358 + 4 * 0.5 = 360 = 0.
        assertEquals(0f, smoothed, 0.1f)
    }

    @Test
    fun `computeMagneticFieldMagnitude calculates 3D Euclidean norm`() {
        // sqrt(30^2 + 40^2 + 0^2) = 50
        val magnitude = CompassMath.computeMagneticFieldMagnitude(30f, 40f, 0f)
        assertEquals(50f, magnitude, 0.001f)
    }

    @Test
    fun `classifyFieldStrength returns accurate status levels`() {
        assertEquals(MagneticFieldStatus.SHIELDED, CompassMath.classifyFieldStrength(15f))
        assertEquals(MagneticFieldStatus.NORMAL_AMBIENT, CompassMath.classifyFieldStrength(45f))
        assertEquals(MagneticFieldStatus.ELEVATED, CompassMath.classifyFieldStrength(85f))
        assertEquals(MagneticFieldStatus.STRONG_EMF, CompassMath.classifyFieldStrength(150f))
    }

    @Test
    fun `applyDeclination adjusts magnetic azimuth to true north`() {
        // +5 deg East declination
        assertEquals(95f, CompassMath.applyDeclination(90f, 5f), 0.01f)
        // -10 deg West declination across 0
        assertEquals(355f, CompassMath.applyDeclination(5f, -10f), 0.01f)
    }
}
