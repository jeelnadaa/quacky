package app.quacky.feature.areavolume.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class AreaVolumeMathTest {

    @Test
    fun `rectangle calculates area and perimeter accurately`() {
        val (area, perimeter) = AreaVolumeMath.rectangle(5.0, 3.0)
        assertEquals(15.0, area, 1e-6)
        assertEquals(16.0, perimeter, 1e-6)
    }

    @Test
    fun `square calculates area and perimeter accurately`() {
        val (area, perimeter) = AreaVolumeMath.square(4.0)
        assertEquals(16.0, area, 1e-6)
        assertEquals(16.0, perimeter, 1e-6)
    }

    @Test
    fun `triangleHeron validates triangle inequality and calculates area`() {
        // Valid 3-4-5 right triangle
        val result = AreaVolumeMath.triangleHeron(3.0, 4.0, 5.0)
        assertNotNull(result)
        result?.let { (area, perimeter) ->
            assertEquals(6.0, area, 1e-6)
            assertEquals(12.0, perimeter, 1e-6)
        }

        // Invalid triangle (violates triangle inequality: 1 + 2 < 10)
        val invalid = AreaVolumeMath.triangleHeron(1.0, 2.0, 10.0)
        assertNull(invalid)
    }

    @Test
    fun `circle calculates area and circumference accurately`() {
        val (area, perimeter) = AreaVolumeMath.circle(1.0)
        assertEquals(PI, area, 1e-6)
        assertEquals(2 * PI, perimeter, 1e-6)
    }

    @Test
    fun `shoelace formula accurately calculates polygon area and perimeter`() {
        // Unit square: (0,0), (1,0), (1,1), (0,1)
        val squarePoints = listOf(
            Point2D(0.0, 0.0),
            Point2D(1.0, 0.0),
            Point2D(1.0, 1.0),
            Point2D(0.0, 1.0)
        )
        val (area, perimeter) = AreaVolumeMath.shoelace(squarePoints)
        assertEquals(1.0, area, 1e-6)
        assertEquals(4.0, perimeter, 1e-6)

        // Triangle: (0,0), (4,0), (0,3)
        val trianglePoints = listOf(
            Point2D(0.0, 0.0),
            Point2D(4.0, 0.0),
            Point2D(0.0, 3.0)
        )
        val (tArea, tPerimeter) = AreaVolumeMath.shoelace(trianglePoints)
        assertEquals(6.0, tArea, 1e-6)
        assertEquals(12.0, tPerimeter, 1e-6)
    }

    @Test
    fun `cube calculates volume and surface area`() {
        val (vol, sa) = AreaVolumeMath.cube(2.0)
        assertEquals(8.0, vol, 1e-6)
        assertEquals(24.0, sa, 1e-6)
    }

    @Test
    fun `cuboid calculates volume and surface area`() {
        val (vol, sa) = AreaVolumeMath.cuboid(2.0, 3.0, 4.0)
        assertEquals(24.0, vol, 1e-6)
        assertEquals(52.0, sa, 1e-6)
    }

    @Test
    fun `cylinder calculates volume and surface area`() {
        val (vol, sa) = AreaVolumeMath.cylinder(1.0, 2.0)
        assertEquals(2 * PI, vol, 1e-6)
        assertEquals(6 * PI, sa, 1e-6)
    }

    @Test
    fun `unit conversions convert lengths, areas, and volumes accurately`() {
        // Length: 100 cm = 1.0 m
        assertEquals(1.0, AreaVolumeMath.toMeters(100.0, LengthUnit.CM), 1e-6)
        assertEquals(100.0, AreaVolumeMath.fromMeters(1.0, LengthUnit.CM), 1e-6)

        // Area: 1 m² = 10,000 cm²
        assertEquals(10000.0, AreaVolumeMath.convertArea(1.0, AreaUnit.CM2), 1e-6)

        // Volume: 1 m³ = 1000 L
        assertEquals(1000.0, AreaVolumeMath.convertVolume(1.0, VolumeUnit.L), 1e-6)
    }

    @Test
    fun `estimatePaint calculates required litres`() {
        val litres = AreaVolumeMath.estimatePaint(
            wallWidthM = 5.0,
            wallHeightM = 2.8,
            openingsAreaM2 = 0.0,
            coats = 2,
            coveragePerLitreM2 = 10.0
        )
        assertEquals(2.8, litres, 1e-6)
    }

    @Test
    fun `estimateConcrete calculates volume and bag count`() {
        val (vol, bags) = AreaVolumeMath.estimateConcrete(3.0, 2.0, 0.1)
        assertEquals(0.6, vol, 1e-6)
        assertEquals(65, bags)
    }
}
