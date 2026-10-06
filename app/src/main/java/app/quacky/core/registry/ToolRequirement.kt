package app.quacky.core.registry

import androidx.annotation.StringRes
import app.quacky.R

enum class ToolRequirement(@StringRes val missingMessageRes: Int) {
    BACK_CAMERA(R.string.req_missing_back_camera),
    ARCORE(R.string.req_missing_arcore_unsupported),
    VALID_DISPLAY_METRICS(R.string.req_missing_display_metrics),
    FLASH(R.string.req_missing_flash),
    ACCELEROMETER(R.string.req_missing_accelerometer),
    VIBRATOR(R.string.req_missing_vibrator)
}
