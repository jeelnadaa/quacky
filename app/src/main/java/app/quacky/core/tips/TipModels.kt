package app.quacky.core.tips

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class VisualType {
    SCANNER_SWEEP,
    PINCH_ZOOM,
    GALLERY_PICK,
    ACTION_SHEET,
    LOCK_URL,
    CHIP_SELECT,
    LIVE_TYPE,
    CONTRAST_CHECK,
    EXPORT_FORMATS,
    AR_PLANE,
    AR_POINTS,
    AR_MODES,
    AR_HEIGHT,
    AR_ESTIMATE,
    RULER_ALIGN,
    RULER_CALIBRATE,
    RULER_MARKERS,
    RULER_FLIP,
    AREA_TABS,
    AREA_SHAPE,
    AREA_RESULTS,
    AREA_ESTIMATE,
    COLOR_SOURCE,
    COLOR_RETICLE,
    COLOR_NUDGE,
    COLOR_ZOOM,
    COLOR_SAMPLE,
    COLOR_FREEZE,
    COLOR_PALETTE,
    COLOR_VALUES,
    METADATA_PICK,
    METADATA_INSPECT,
    METADATA_REMOVE,
    METADATA_CLEAN,
    COMPRESS_PICK,
    COMPRESS_SLIDER,
    COMPRESS_SPLIT,
    COMPRESS_SAVE,
    TEXT_TYPE,
    TEXT_OPTIONS,
    TEXT_SEARCH,
    TEXT_LIMIT,
    DATE_TABS,
    DATE_PICK,
    DATE_RESULTS,
    DATE_COUNTDOWN,
    DICE_CONFIG,
    DICE_ROLL,
    DICE_MODIFIER,
    DICE_SAVE,
    COIN_FLIP,
    COIN_LABELS,
    COIN_COUNTERS,
    RNG_RANGE,
    RNG_COUNT,
    RNG_DUPLICATES,
    RNG_SEED,
    WHEEL_OPTIONS,
    WHEEL_SPIN,
    WHEEL_ELIMINATE,
    WHEEL_SAVE,
    TEAM_NAMES,
    TEAM_SPLIT,
    TEAM_SHUFFLE,
    TEAM_CONSTRAINTS,
    TEAM_SHARE
}

data class TipStep(
    val visualType: VisualType,
    val title: String,
    val body: String,
    val a11yDescription: String
)

data class ToolGuide(
    val toolId: String,
    val steps: List<TipStep>
)
