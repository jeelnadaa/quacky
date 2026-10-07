package app.quacky.core.registry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.ViewInAr
import app.quacky.R

object ToolRegistry {

    val QR_SCANNER = ToolDefinition(
        id = "qr_scanner",
        nameRes = R.string.tool_qr_scanner_name,
        descriptionRes = R.string.tool_qr_scanner_desc,
        category = ToolCategory.SCAN_GENERATE,
        icon = Icons.Rounded.QrCodeScanner,
        keywords = listOf("qr", "barcode", "scan", "scanner", "camera", "upc", "ean", "code"),
        route = "tool/qr_scanner",
        requirements = setOf(ToolRequirement.BACK_CAMERA)
    )

    val QR_GENERATOR = ToolDefinition(
        id = "qr_generator",
        nameRes = R.string.tool_qr_generator_name,
        descriptionRes = R.string.tool_qr_generator_desc,
        category = ToolCategory.SCAN_GENERATE,
        icon = Icons.Rounded.QrCode,
        keywords = listOf("qr", "barcode", "generate", "creator", "make", "wifi", "upi", "vcard"),
        route = "tool/qr_generator",
        requirements = emptySet()
    )

    val DOCUMENT_SCANNER = ToolDefinition(
        id = "doc_scanner",
        nameRes = R.string.tool_doc_scanner_name,
        descriptionRes = R.string.tool_doc_scanner_desc,
        category = ToolCategory.SCAN_GENERATE,
        icon = Icons.Rounded.PictureAsPdf,
        keywords = listOf("document", "scanner", "lens", "pdf", "crop", "enhance", "camera", "page", "scan"),
        route = "tool/doc_scanner",
        requirements = setOf(ToolRequirement.BACK_CAMERA)
    )

    val AR_RULER = ToolDefinition(
        id = "ar_ruler",
        nameRes = R.string.tool_ar_ruler_name,
        descriptionRes = R.string.tool_ar_ruler_desc,
        category = ToolCategory.MEASURE,
        icon = Icons.Rounded.ViewInAr,
        keywords = listOf("ar", "ruler", "measure", "height", "distance", "tape", "camera", "arcore"),
        route = "tool/ar_ruler",
        requirements = setOf(ToolRequirement.BACK_CAMERA, ToolRequirement.ARCORE)
    )

    val SCREEN_RULER = ToolDefinition(
        id = "screen_ruler",
        nameRes = R.string.tool_screen_ruler_name,
        descriptionRes = R.string.tool_screen_ruler_desc,
        category = ToolCategory.MEASURE,
        icon = Icons.Rounded.Straighten,
        keywords = listOf("ruler", "screen", "centimeter", "inch", "mm", "measure", "physical", "scale"),
        route = "tool/screen_ruler",
        requirements = setOf(ToolRequirement.VALID_DISPLAY_METRICS)
    )

    val AREA_VOLUME = ToolDefinition(
        id = "area_volume",
        nameRes = R.string.tool_area_volume_name,
        descriptionRes = R.string.tool_area_volume_desc,
        category = ToolCategory.MEASURE,
        icon = Icons.Rounded.Calculate,
        keywords = listOf("area", "volume", "calculator", "geometry", "paint", "tile", "concrete", "tank"),
        route = "tool/area_volume",
        requirements = emptySet()
    )

    val SPIRIT_LEVEL = ToolDefinition(
        id = "spirit_level",
        nameRes = R.string.tool_spirit_level_name,
        descriptionRes = R.string.tool_spirit_level_desc,
        category = ToolCategory.MEASURE,
        icon = Icons.Rounded.Straighten,
        keywords = listOf("level", "spirit", "bubble", "inclinometer", "angle", "pitch", "roll", "tilt", "surface"),
        route = "tool/spirit_level",
        requirements = setOf(ToolRequirement.ACCELEROMETER)
    )

    val COLOR_PICKER = ToolDefinition(
        id = "color_picker",
        nameRes = R.string.tool_color_picker_name,
        descriptionRes = R.string.tool_color_picker_desc,
        category = ToolCategory.COLOR_IMAGE,
        icon = Icons.Rounded.Palette,
        keywords = listOf("color", "picker", "hex", "rgb", "hsl", "eyedropper", "palette", "contrast"),
        route = "tool/color_picker",
        requirements = emptySet()
    )

    val METADATA = ToolDefinition(
        id = "metadata",
        nameRes = R.string.tool_metadata_name,
        descriptionRes = R.string.tool_metadata_desc,
        category = ToolCategory.COLOR_IMAGE,
        icon = Icons.Rounded.PhotoCamera,
        keywords = listOf("exif", "metadata", "privacy", "gps", "location", "remove", "clean", "strip", "photo"),
        route = "tool/metadata",
        requirements = emptySet()
    )

    val COMPRESSOR = ToolDefinition(
        id = "compressor",
        nameRes = R.string.tool_compressor_name,
        descriptionRes = R.string.tool_compressor_desc,
        category = ToolCategory.COLOR_IMAGE,
        icon = Icons.Rounded.Compress,
        keywords = listOf("compress", "image", "pdf", "shrink", "resize", "size", "kb", "mb"),
        route = "tool/compressor",
        requirements = emptySet()
    )

    val TEXT_COUNTER = ToolDefinition(
        id = "text_counter",
        nameRes = R.string.tool_text_counter_name,
        descriptionRes = R.string.tool_text_counter_desc,
        category = ToolCategory.TEXT_DATE,
        icon = Icons.Rounded.TextFields,
        keywords = listOf("text", "word", "character", "count", "counter", "frequency", "reading time", "limit"),
        route = "tool/text_counter",
        requirements = emptySet()
    )

    val DATE_CALC = ToolDefinition(
        id = "date_calc",
        nameRes = R.string.tool_date_calc_name,
        descriptionRes = R.string.tool_date_calc_desc,
        category = ToolCategory.TEXT_DATE,
        icon = Icons.Rounded.DateRange,
        keywords = listOf("date", "age", "birthday", "days between", "calculator", "countdown", "business days"),
        route = "tool/date_calc",
        requirements = emptySet()
    )

    val DICE = ToolDefinition(
        id = "dice",
        nameRes = R.string.tool_dice_name,
        descriptionRes = R.string.tool_dice_desc,
        category = ToolCategory.RANDOM,
        icon = Icons.Rounded.Casino,
        keywords = listOf("dice", "roll", "d6", "d20", "random", "boardgame", "dnd"),
        route = "tool/dice",
        requirements = emptySet()
    )

    val COIN_FLIP = ToolDefinition(
        id = "coin_flip",
        nameRes = R.string.tool_coin_flip_name,
        descriptionRes = R.string.tool_coin_flip_desc,
        category = ToolCategory.RANDOM,
        icon = Icons.Rounded.MonetizationOn,
        keywords = listOf("coin", "flip", "toss", "heads", "tails", "decision", "random"),
        route = "tool/coin_flip",
        requirements = emptySet()
    )

    val RANDOM_NUMBER = ToolDefinition(
        id = "random_number",
        nameRes = R.string.tool_random_number_name,
        descriptionRes = R.string.tool_random_number_desc,
        category = ToolCategory.RANDOM,
        icon = Icons.Rounded.Numbers,
        keywords = listOf("random", "number", "rng", "generate", "lottery", "seed", "min", "max"),
        route = "tool/random_number",
        requirements = emptySet()
    )

    val PICKER_WHEEL = ToolDefinition(
        id = "picker_wheel",
        nameRes = R.string.tool_picker_wheel_name,
        descriptionRes = R.string.tool_picker_wheel_desc,
        category = ToolCategory.RANDOM,
        icon = Icons.Rounded.PieChart,
        keywords = listOf("wheel", "picker", "spin", "decision", "choice", "random", "fortune"),
        route = "tool/picker_wheel",
        requirements = emptySet()
    )

    val TEAM_SPLITTER = ToolDefinition(
        id = "team_splitter",
        nameRes = R.string.tool_team_splitter_name,
        descriptionRes = R.string.tool_team_splitter_desc,
        category = ToolCategory.RANDOM,
        icon = Icons.Rounded.Groups,
        keywords = listOf("team", "splitter", "groups", "random", "split", "draft", "friends", "fair"),
        route = "tool/team_splitter",
        requirements = emptySet()
    )

    val QUACKY_SURFER = ToolDefinition(
        id = "quacky_surfer",
        nameRes = R.string.tool_quacky_surfer_name,
        descriptionRes = R.string.tool_quacky_surfer_desc,
        category = ToolCategory.RANDOM,
        icon = Icons.AutoMirrored.Rounded.DirectionsRun,
        keywords = listOf("subway", "surfer", "runner", "game", "duck", "dodge", "jump", "arcade", "offline"),
        route = "tool/quacky_surfer",
        requirements = emptySet()
    )

    val COMPASS = ToolDefinition(
        id = "compass",
        nameRes = R.string.tool_compass_name,
        descriptionRes = R.string.tool_compass_desc,
        category = ToolCategory.MEASURE,
        icon = Icons.Rounded.Explore,
        keywords = listOf("compass", "magnetic", "north", "heading", "azimuth", "emf", "metal", "detector", "microtesla"),
        route = "tool/compass",
        requirements = setOf(ToolRequirement.MAGNETOMETER, ToolRequirement.ACCELEROMETER)
    )

    val UNIT_CONVERTER = ToolDefinition(
        id = "unit_converter",
        nameRes = R.string.tool_unit_converter_name,
        descriptionRes = R.string.tool_unit_converter_desc,
        category = ToolCategory.MEASURE,
        icon = Icons.Rounded.Calculate,
        keywords = listOf("unit", "converter", "length", "mass", "temperature", "speed", "area", "pressure", "digital", "bytes", "storage", "fuel", "volume"),
        route = "tool/unit_converter",
        requirements = emptySet()
    )

    val allTools: List<ToolDefinition> = listOf(
        QR_SCANNER,
        QR_GENERATOR,
        DOCUMENT_SCANNER,
        AR_RULER,
        SCREEN_RULER,
        AREA_VOLUME,
        COLOR_PICKER,
        METADATA,
        COMPRESSOR,
        TEXT_COUNTER,
        DATE_CALC,
        DICE,
        COIN_FLIP,
        RANDOM_NUMBER,
        PICKER_WHEEL,
        TEAM_SPLITTER,
        QUACKY_SURFER,
        SPIRIT_LEVEL,
        COMPASS,
        UNIT_CONVERTER
    )

    fun getById(id: String): ToolDefinition? = allTools.firstOrNull { it.id == id }

    fun getByCategory(category: ToolCategory): List<ToolDefinition> =
        allTools.filter { it.category == category }
}
