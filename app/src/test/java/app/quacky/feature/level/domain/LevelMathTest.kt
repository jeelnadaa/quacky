package app.quacky.feature.level.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelMathTest {

    @Test
    fun `flat phone returns zero pitch and roll and is level`() {
        val pitch = LevelMath.computePitch(0f, 0f, 9.8f)
        val roll = LevelMath.computeRoll(0f, 0f, 9.8f)
        val tilt = LevelMath.computeTiltAngle(0f, 0f, 9.8f)

        assertEquals(0f, pitch, 0.01f)
        assertEquals(0f, roll, 0.01f)
        assertEquals(0f, tilt, 0.01f)
        assertTrue(LevelMath.isFlatSurface(9.8f))
        assertTrue(LevelMath.isLevel(pitch, roll, 0.4f))
    }

    @Test
    fun `vertical phone is detected as edge mode not flat surface`() {
        // Phone upright in portrait (gravity along Y axis)
        assertFalse(LevelMath.isFlatSurface(0f))
        assertFalse(LevelMath.isFlatSurface(1.2f))
    }

    @Test
    fun `pitch and roll detect tilt correctly`() {
        // Tilted forward along Y axis
        val pitch = LevelMath.computePitch(0f, 5f, 5f)
        assertTrue("Pitch should be positive when tilted on Y", pitch > 0f)

        // Tilted right along X axis
        val roll = LevelMath.computeRoll(5f, 0f, 5f)
        assertTrue("Roll should be negative when tilted right", roll < 0f)
    }

    @Test
    fun `isLevel threshold strictly enforced`() {
        assertTrue(LevelMath.isLevel(0.2f, -0.3f, 0.4f))
        assertFalse(LevelMath.isLevel(0.5f, 0.0f, 0.4f))
        assertFalse(LevelMath.isLevel(0.1f, -0.8f, 0.4f))
    }
}
