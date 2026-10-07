package app.quacky.feature.converter.domain

import app.quacky.feature.converter.model.UnitCategory
import app.quacky.feature.converter.model.UnitRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConverterEngineTest {

    @Test
    fun `convert linear length units correctly`() {
        val meter = UnitRegistry.LENGTH_UNITS.first { it.id == "m" }
        val kilometer = UnitRegistry.LENGTH_UNITS.first { it.id == "km" }
        val inch = UnitRegistry.LENGTH_UNITS.first { it.id == "in" }
        val foot = UnitRegistry.LENGTH_UNITS.first { it.id == "ft" }

        // 1000 meters = 1 kilometer
        assertEquals(1.0, UnitConverterEngine.convert(1000.0, meter, kilometer), 0.0001)

        // 1 meter = 39.3701 inches
        assertEquals(39.3701, UnitConverterEngine.convert(1.0, meter, inch), 0.001)

        // 1 foot = 12 inches
        assertEquals(12.0, UnitConverterEngine.convert(1.0, foot, inch), 0.001)
    }

    @Test
    fun `convert mass units correctly`() {
        val kg = UnitRegistry.MASS_UNITS.first { it.id == "kg" }
        val lb = UnitRegistry.MASS_UNITS.first { it.id == "lb" }
        val g = UnitRegistry.MASS_UNITS.first { it.id == "g" }

        // 1 kg = 1000 g
        assertEquals(1000.0, UnitConverterEngine.convert(1.0, kg, g), 0.0001)

        // 1 kg approx 2.20462 lb
        assertEquals(2.20462, UnitConverterEngine.convert(1.0, kg, lb), 0.001)
    }

    @Test
    fun `convert temperature units correctly`() {
        val c = UnitRegistry.TEMPERATURE_UNITS.first { it.id == "c" }
        val f = UnitRegistry.TEMPERATURE_UNITS.first { it.id == "f" }
        val k = UnitRegistry.TEMPERATURE_UNITS.first { it.id == "k" }

        // 0 C = 32 F = 273.15 K
        assertEquals(32.0, UnitConverterEngine.convert(0.0, c, f), 0.001)
        assertEquals(273.15, UnitConverterEngine.convert(0.0, c, k), 0.001)

        // 100 C = 212 F
        assertEquals(212.0, UnitConverterEngine.convert(100.0, c, f), 0.001)

        // -40 C = -40 F
        assertEquals(-40.0, UnitConverterEngine.convert(-40.0, c, f), 0.001)
        assertEquals(-40.0, UnitConverterEngine.convert(-40.0, f, c), 0.001)
    }

    @Test
    fun `convert digital storage units decimal and binary`() {
        val mb = UnitRegistry.DIGITAL_STORAGE_UNITS.first { it.id == "mb" }
        val gb = UnitRegistry.DIGITAL_STORAGE_UNITS.first { it.id == "gb" }
        val kib = UnitRegistry.DIGITAL_STORAGE_UNITS.first { it.id == "kib" }
        val byte = UnitRegistry.DIGITAL_STORAGE_UNITS.first { it.id == "byte" }

        // 1000 MB = 1 GB (decimal)
        assertEquals(1.0, UnitConverterEngine.convert(1000.0, mb, gb), 0.0001)

        // 1 KiB = 1024 Bytes
        assertEquals(1024.0, UnitConverterEngine.convert(1.0, kib, byte), 0.0001)
    }

    @Test
    fun `convert fuel economy reciprocal units correctly`() {
        val l100 = UnitRegistry.FUEL_ECONOMY_UNITS.first { it.id == "l100km" }
        val mpgUs = UnitRegistry.FUEL_ECONOMY_UNITS.first { it.id == "mpg_us" }
        val kml = UnitRegistry.FUEL_ECONOMY_UNITS.first { it.id == "kml" }

        // 10 L/100km = 10 km/L
        assertEquals(10.0, UnitConverterEngine.convert(10.0, l100, kml), 0.01)

        // 10 L/100km approx 23.52 mpg US
        assertEquals(23.52, UnitConverterEngine.convert(10.0, l100, mpgUs), 0.01)

        // 23.52 mpg US approx 10 L/100km
        assertEquals(10.0, UnitConverterEngine.convert(23.5214583, mpgUs, l100), 0.01)
    }

    @Test
    fun `formatDisplay formats numbers cleanly without floating point artifacts`() {
        assertEquals("12", UnitConverterEngine.formatDisplay(12.0))
        assertEquals("0.5", UnitConverterEngine.formatDisplay(0.5))
        assertEquals("0", UnitConverterEngine.formatDisplay(0.0))
        assertTrue(UnitConverterEngine.formatDisplay(1e12).contains("E"))
    }
}
