package app.quacky.feature.converter.domain

import app.quacky.feature.converter.model.UnitCategory
import app.quacky.feature.converter.model.UnitItem
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object UnitConverterEngine {

    /**
     * Converts a numeric value between two units of the same category.
     */
    fun convert(value: Double, fromUnit: UnitItem, toUnit: UnitItem): Double {
        if (fromUnit.id == toUnit.id) return value

        return when (fromUnit.category) {
            UnitCategory.TEMPERATURE -> convertTemperature(value, fromUnit.id, toUnit.id)
            UnitCategory.FUEL_ECONOMY -> convertFuelEconomy(value, fromUnit.id, toUnit.id)
            else -> convertLinear(value, fromUnit, toUnit)
        }
    }

    private fun convertLinear(value: Double, fromUnit: UnitItem, toUnit: UnitItem): Double {
        if (toUnit.factorToBase == 0.0) return 0.0
        val baseValue = value * fromUnit.factorToBase
        return baseValue / toUnit.factorToBase
    }

    private fun convertTemperature(value: Double, fromId: String, toId: String): Double {
        // Step 1: Normalize to Celsius
        val celsius = when (fromId) {
            "c" -> value
            "f" -> (value - 32.0) * (5.0 / 9.0)
            "k" -> value - 273.15
            else -> value
        }

        // Step 2: Convert Celsius to target
        return when (toId) {
            "c" -> celsius
            "f" -> (celsius * (9.0 / 5.0)) + 32.0
            "k" -> celsius + 273.15
            else -> celsius
        }
    }

    private fun convertFuelEconomy(value: Double, fromId: String, toId: String): Double {
        if (value <= 0.0) return 0.0

        // Step 1: Normalize to L/100km
        val l100km = when (fromId) {
            "l100km" -> value
            "mpg_us" -> 235.214583 / value
            "mpg_uk" -> 282.4809363 / value
            "kml" -> 100.0 / value
            else -> value
        }

        // Step 2: Convert L/100km to target
        return when (toId) {
            "l100km" -> l100km
            "mpg_us" -> 235.214583 / l100km
            "mpg_uk" -> 282.4809363 / l100km
            "kml" -> 100.0 / l100km
            else -> l100km
        }
    }

    /**
     * Formats a double value with high aesthetic precision, stripping unnecessary trailing zeros
     * and switching to scientific notation for extreme magnitudes.
     */
    fun formatDisplay(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        if (value == 0.0) return "0"

        val absVal = abs(value)
        val symbols = DecimalFormatSymbols(Locale.US)

        return if (absVal >= 1e10 || (absVal < 1e-4 && absVal > 0)) {
            val scientificFormat = DecimalFormat("0.####E0", symbols)
            scientificFormat.format(value)
        } else {
            // Up to 6 decimal places, clean formatting
            val bd = BigDecimal(value, MathContext(8, RoundingMode.HALF_UP))
            val stripped = bd.stripTrailingZeros()
            stripped.toPlainString()
        }
    }
}
