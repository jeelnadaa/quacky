package app.quacky.core.registry

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.ui.graphics.vector.ImageVector
import app.quacky.R

enum class ToolCategory(
    val id: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    SCAN_GENERATE(
        id = "scan_generate",
        titleRes = R.string.cat_scan_generate,
        icon = Icons.Rounded.QrCodeScanner
    ),
    MEASURE(
        id = "measure",
        titleRes = R.string.cat_measure,
        icon = Icons.Rounded.Straighten
    ),
    COLOR_IMAGE(
        id = "color_image",
        titleRes = R.string.cat_color_image,
        icon = Icons.Rounded.Palette
    ),
    TEXT_DATE(
        id = "text_date",
        titleRes = R.string.cat_text_date,
        icon = Icons.Rounded.CalendarMonth
    ),
    RANDOM(
        id = "random",
        titleRes = R.string.cat_random,
        icon = Icons.Rounded.AutoFixHigh
    )
}
