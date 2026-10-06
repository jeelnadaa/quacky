package app.quacky.feature.screenruler.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class RulerUnit(val symbol: String) {
    MM("mm"),
    CM("cm"),
    INCH("in")
}

object RulerMath {
    // Standard ISO/IEC 7810 ID-1 card dimensions
    const val CREDIT_CARD_WIDTH_MM = 85.60f
    const val CREDIT_CARD_HEIGHT_MM = 53.98f
    const val MM_PER_INCH = 25.4f

    /**
     * Computes pixels per millimetre using the base DPI and calibration multiplier.
     */
    fun pixelsPerMm(baseDpi: Float, calibrationFactor: Float): Float {
        val safeDpi = if (baseDpi > 0f) baseDpi else 160f
        val safeFactor = if (calibrationFactor > 0f) calibrationFactor else 1.0f
        return (safeDpi / MM_PER_INCH) * safeFactor
    }

    /**
     * Computes pixels per inch using the base DPI and calibration multiplier.
     */
    fun pixelsPerInch(baseDpi: Float, calibrationFactor: Float): Float {
        val safeDpi = if (baseDpi > 0f) baseDpi else 160f
        val safeFactor = if (calibrationFactor > 0f) calibrationFactor else 1.0f
        return safeDpi * safeFactor
    }

    /**
     * Calculates distance in millimetres between two pixel points along the ruler axis.
     */
    fun calculateDistanceMm(deltaPx: Float, baseDpi: Float, calibrationFactor: Float): Float {
        val pxPerMm = pixelsPerMm(baseDpi, calibrationFactor)
        if (pxPerMm <= 0f) return 0f
        return abs(deltaPx) / pxPerMm
    }

    /**
     * Formats distance value cleanly with proper decimals.
     */
    fun formatDistance(distanceMm: Float, unit: RulerUnit): String {
        return when (unit) {
            RulerUnit.MM -> String.format(Locale.US, "%.1f mm", distanceMm)
            RulerUnit.CM -> String.format(Locale.US, "%.2f cm", distanceMm / 10f)
            RulerUnit.INCH -> {
                val inches = distanceMm / MM_PER_INCH
                val fractional = formatFractionalInches(inches)
                String.format(Locale.US, "%.2f in (%s)", inches, fractional)
            }
        }
    }

    /**
     * Formats a decimal inch into standard 1/16th inch fractions (e.g. 1 5/16", 7/8", 1/2").
     */
    fun formatFractionalInches(inches: Float): String {
        val totalSixteenths = (inches * 16f).roundToInt()
        val whole = totalSixteenths / 16
        var rem = totalSixteenths % 16

        if (rem == 0) {
            return "${whole}\""
        }

        var denom = 16
        while (rem % 2 == 0 && denom > 2) {
            rem /= 2
            denom /= 2
        }

        return if (whole > 0) {
            "$whole $rem/$denom\""
        } else {
            "$rem/$denom\""
        }
    }
}
