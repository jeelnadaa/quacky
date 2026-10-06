package app.quacky.feature.colorpicker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathTest {

    @Test
    fun `test HEX RGB HSL conversions for pure red`() {
        val redInt = ColorMath.RED // 0xFFFF0000
        val formats = ColorMath.toFormats(redInt)

        assertEquals("#FF0000", formats.hex)
        assertEquals(255, formats.rgb.first)
        assertEquals(0, formats.rgb.second)
        assertEquals(0, formats.rgb.third)
        assertEquals(0f, formats.hsl.first, 0.01f)
        assertEquals(100f, formats.hsl.second, 0.01f)
        assertEquals(50f, formats.hsl.third, 0.01f)
        assertEquals("Color(0xFFFF0000)", formats.composeString)
    }

    @Test
    fun `test WCAG contrast ratio for black and white`() {
        val black = ColorMath.BLACK
        val white = ColorMath.WHITE

        val contrast = ColorMath.calculateContrast(black, white)
        assertEquals(21f, contrast.ratio, 0.1f)
        assertTrue(contrast.normalAa)
        assertTrue(contrast.normalAaa)
        assertTrue(contrast.largeAa)
        assertTrue(contrast.largeAaa)
    }

    @Test
    fun `test CIELAB Delta E zero distance for identical colors`() {
        val c = ColorMath.BLUE
        val dist = ColorMath.deltaE(c, c)
        assertEquals(0f, dist, 0.001f)
    }

    @Test
    fun `test nearest color name resolution`() {
        val red = ColorMath.RED
        val (name, dist) = NamedColors.findNearest(red)
        assertEquals("Red", name)
        assertEquals(0f, dist, 0.001f)

        val black = ColorMath.BLACK
        val (blackName, blackDist) = NamedColors.findNearest(black)
        assertEquals("Black", blackName)
        assertEquals(0f, blackDist, 0.001f)
    }

    @Test
    fun `test tints and shades generation count`() {
        val (tints, shades) = ColorMath.generateTintsAndShades(ColorMath.GREEN)
        assertEquals(5, tints.size)
        assertEquals(5, shades.size)
    }

    @Test
    fun `test harmonies generation categories`() {
        val harmonies = ColorMath.generateHarmonies(ColorMath.CYAN)
        assertNotNull(harmonies["Complementary"])
        assertNotNull(harmonies["Analogous"])
        assertNotNull(harmonies["Triadic"])
        assertNotNull(harmonies["Split-Comp"])
    }
}
