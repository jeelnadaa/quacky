package app.quacky.feature.converter.model

import androidx.annotation.StringRes
import app.quacky.R

enum class UnitCategory(@StringRes val titleRes: Int) {
    LENGTH(R.string.unit_cat_length),
    MASS(R.string.unit_cat_mass),
    TEMPERATURE(R.string.unit_cat_temperature),
    SPEED(R.string.unit_cat_speed),
    AREA(R.string.unit_cat_area),
    PRESSURE(R.string.unit_cat_pressure),
    DIGITAL_STORAGE(R.string.unit_cat_digital),
    FUEL_ECONOMY(R.string.unit_cat_fuel),
    VOLUME(R.string.unit_cat_volume)
}

data class UnitItem(
    val id: String,
    val name: String,
    val symbol: String,
    val category: UnitCategory,
    val factorToBase: Double = 1.0 // Multiplier to get base unit
)

object UnitRegistry {

    // Base: Meter (m)
    val LENGTH_UNITS = listOf(
        UnitItem("mm", "Millimeter", "mm", UnitCategory.LENGTH, 0.001),
        UnitItem("cm", "Centimeter", "cm", UnitCategory.LENGTH, 0.01),
        UnitItem("m", "Meter", "m", UnitCategory.LENGTH, 1.0),
        UnitItem("km", "Kilometer", "km", UnitCategory.LENGTH, 1000.0),
        UnitItem("in", "Inch", "in", UnitCategory.LENGTH, 0.0254),
        UnitItem("ft", "Foot", "ft", UnitCategory.LENGTH, 0.3048),
        UnitItem("yd", "Yard", "yd", UnitCategory.LENGTH, 0.9144),
        UnitItem("mi", "Mile", "mi", UnitCategory.LENGTH, 1609.344),
        UnitItem("nmi", "Nautical Mile", "nmi", UnitCategory.LENGTH, 1852.0)
    )

    // Base: Kilogram (kg)
    val MASS_UNITS = listOf(
        UnitItem("mg", "Milligram", "mg", UnitCategory.MASS, 0.000001),
        UnitItem("g", "Gram", "g", UnitCategory.MASS, 0.001),
        UnitItem("kg", "Kilogram", "kg", UnitCategory.MASS, 1.0),
        UnitItem("t", "Metric Ton", "t", UnitCategory.MASS, 1000.0),
        UnitItem("oz", "Ounce", "oz", UnitCategory.MASS, 0.028349523125),
        UnitItem("lb", "Pound", "lb", UnitCategory.MASS, 0.45359237),
        UnitItem("st", "Stone", "st", UnitCategory.MASS, 6.35029318)
    )

    // Temperature handled via custom affine transforms
    val TEMPERATURE_UNITS = listOf(
        UnitItem("c", "Celsius", "°C", UnitCategory.TEMPERATURE),
        UnitItem("f", "Fahrenheit", "°F", UnitCategory.TEMPERATURE),
        UnitItem("k", "Kelvin", "K", UnitCategory.TEMPERATURE)
    )

    // Base: Meter per second (m/s)
    val SPEED_UNITS = listOf(
        UnitItem("ms", "Meter / second", "m/s", UnitCategory.SPEED, 1.0),
        UnitItem("kmh", "Kilometer / hour", "km/h", UnitCategory.SPEED, 1.0 / 3.6),
        UnitItem("mph", "Mile / hour", "mph", UnitCategory.SPEED, 0.44704),
        UnitItem("kn", "Knot", "kn", UnitCategory.SPEED, 0.514444),
        UnitItem("fts", "Foot / second", "ft/s", UnitCategory.SPEED, 0.3048)
    )

    // Base: Square Meter (m²)
    val AREA_UNITS = listOf(
        UnitItem("sqmm", "Square Millimeter", "mm²", UnitCategory.AREA, 1e-6),
        UnitItem("sqcm", "Square Centimeter", "cm²", UnitCategory.AREA, 1e-4),
        UnitItem("sqm", "Square Meter", "m²", UnitCategory.AREA, 1.0),
        UnitItem("sqkm", "Square Kilometer", "km²", UnitCategory.AREA, 1e6),
        UnitItem("ha", "Hectare", "ha", UnitCategory.AREA, 10000.0),
        UnitItem("ac", "Acre", "ac", UnitCategory.AREA, 4046.8564224),
        UnitItem("sqin", "Square Inch", "in²", UnitCategory.AREA, 0.00064516),
        UnitItem("sqft", "Square Foot", "ft²", UnitCategory.AREA, 0.09290304),
        UnitItem("sqyd", "Square Yard", "yd²", UnitCategory.AREA, 0.83612736),
        UnitItem("sqmi", "Square Mile", "mi²", UnitCategory.AREA, 2589988.110336)
    )

    // Base: Pascal (Pa)
    val PRESSURE_UNITS = listOf(
        UnitItem("pa", "Pascal", "Pa", UnitCategory.PRESSURE, 1.0),
        UnitItem("hpa", "Hectopascal", "hPa", UnitCategory.PRESSURE, 100.0),
        UnitItem("bar", "Bar", "bar", UnitCategory.PRESSURE, 100000.0),
        UnitItem("mbar", "Millibar", "mbar", UnitCategory.PRESSURE, 100.0),
        UnitItem("atm", "Atmosphere", "atm", UnitCategory.PRESSURE, 101325.0),
        UnitItem("psi", "Pound / sq inch", "psi", UnitCategory.PRESSURE, 6894.757293),
        UnitItem("mmhg", "Millimeter of Mercury", "mmHg", UnitCategory.PRESSURE, 133.322387415)
    )

    // Base: Byte (B)
    val DIGITAL_STORAGE_UNITS = listOf(
        UnitItem("bit", "Bit", "b", UnitCategory.DIGITAL_STORAGE, 0.125),
        UnitItem("byte", "Byte", "B", UnitCategory.DIGITAL_STORAGE, 1.0),
        UnitItem("kb", "Kilobyte (decimal)", "KB", UnitCategory.DIGITAL_STORAGE, 1000.0),
        UnitItem("mb", "Megabyte (decimal)", "MB", UnitCategory.DIGITAL_STORAGE, 1e6),
        UnitItem("gb", "Gigabyte (decimal)", "GB", UnitCategory.DIGITAL_STORAGE, 1e9),
        UnitItem("tb", "Terabyte (decimal)", "TB", UnitCategory.DIGITAL_STORAGE, 1e12),
        UnitItem("kib", "Kibibyte (binary)", "KiB", UnitCategory.DIGITAL_STORAGE, 1024.0),
        UnitItem("mib", "Mebibyte (binary)", "MiB", UnitCategory.DIGITAL_STORAGE, 1048576.0),
        UnitItem("gib", "Gibibyte (binary)", "GiB", UnitCategory.DIGITAL_STORAGE, 1073741824.0),
        UnitItem("tib", "Tebibyte (binary)", "TiB", UnitCategory.DIGITAL_STORAGE, 1099511627776.0)
    )

    // Base: Liters per 100km (L/100km)
    val FUEL_ECONOMY_UNITS = listOf(
        UnitItem("l100km", "Liters / 100 km", "L/100km", UnitCategory.FUEL_ECONOMY),
        UnitItem("mpg_us", "Miles / gallon (US)", "mpg US", UnitCategory.FUEL_ECONOMY),
        UnitItem("mpg_uk", "Miles / gallon (UK)", "mpg UK", UnitCategory.FUEL_ECONOMY),
        UnitItem("kml", "Kilometers / liter", "km/L", UnitCategory.FUEL_ECONOMY)
    )

    // Base: Liter (L)
    val VOLUME_UNITS = listOf(
        UnitItem("ml", "Milliliter", "mL", UnitCategory.VOLUME, 0.001),
        UnitItem("l", "Liter", "L", UnitCategory.VOLUME, 1.0),
        UnitItem("cu_m", "Cubic Meter", "m³", UnitCategory.VOLUME, 1000.0),
        UnitItem("floz", "Fluid Ounce (US)", "fl oz", UnitCategory.VOLUME, 0.0295735295625),
        UnitItem("cup", "Cup (US)", "cup", UnitCategory.VOLUME, 0.2365882365),
        UnitItem("pt", "Pint (US)", "pt", UnitCategory.VOLUME, 0.473176473),
        UnitItem("qt", "Quart (US)", "qt", UnitCategory.VOLUME, 0.946352946),
        UnitItem("gal_us", "Gallon (US)", "gal", UnitCategory.VOLUME, 3.785411784),
        UnitItem("gal_uk", "Gallon (UK)", "imp gal", UnitCategory.VOLUME, 4.54609)
    )

    fun getUnitsForCategory(category: UnitCategory): List<UnitItem> {
        return when (category) {
            UnitCategory.LENGTH -> LENGTH_UNITS
            UnitCategory.MASS -> MASS_UNITS
            UnitCategory.TEMPERATURE -> TEMPERATURE_UNITS
            UnitCategory.SPEED -> SPEED_UNITS
            UnitCategory.AREA -> AREA_UNITS
            UnitCategory.PRESSURE -> PRESSURE_UNITS
            UnitCategory.DIGITAL_STORAGE -> DIGITAL_STORAGE_UNITS
            UnitCategory.FUEL_ECONOMY -> FUEL_ECONOMY_UNITS
            UnitCategory.VOLUME -> VOLUME_UNITS
        }
    }
}
