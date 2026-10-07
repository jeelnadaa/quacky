package app.quacky.feature.arruler.engine

enum class ReferenceItem(val label: String, val lengthMeters: Double) {
    BANK_CARD("Bank Card (85.6 mm)", MeasureTuning.CARD_LENGTH_METERS),
    A4_LONG("A4 Long Edge (297 mm)", MeasureTuning.A4_LONG_EDGE_METERS),
    A4_SHORT("A4 Short Edge (210 mm)", MeasureTuning.A4_SHORT_EDGE_METERS),
    US_LETTER_LONG("US Letter Long Edge (279.4 mm)", MeasureTuning.US_LETTER_LONG_METERS),
    US_LETTER_SHORT("US Letter Short Edge (215.9 mm)", MeasureTuning.US_LETTER_SHORT_METERS),
    CUSTOM("Custom Length", 0.0)
}

data class ScaleCheckResult(
    val success: Boolean,
    val scaleFactor: Double,
    val message: String
)

class ScaleChecker {

    var currentScaleFactor: Double = 1.0
        private set

    val isCalibrated: Boolean
        get() = currentScaleFactor != 1.0

    fun reset() {
        currentScaleFactor = 1.0
    }

    fun calibrate(
        trueLengthMeters: Double,
        measuredLengthMeters: Double
    ): ScaleCheckResult {
        if (measuredLengthMeters <= 0.001 || trueLengthMeters <= 0.001) {
            return ScaleCheckResult(
                success = false,
                scaleFactor = 1.0,
                message = "Invalid measurement length."
            )
        }

        val k = trueLengthMeters / measuredLengthMeters
        return if (k in MeasureTuning.SCALE_FACTOR_MIN..MeasureTuning.SCALE_FACTOR_MAX) {
            currentScaleFactor = k
            ScaleCheckResult(
                success = true,
                scaleFactor = k,
                message = String.format("Scale checked ✓ ×%.3f", k)
            )
        } else {
            ScaleCheckResult(
                success = false,
                scaleFactor = 1.0,
                message = "Measurement was too far off. Move closer and try again."
            )
        }
    }

    fun applyScale(lengthMeters: Double): Double {
        return lengthMeters * currentScaleFactor
    }
}
