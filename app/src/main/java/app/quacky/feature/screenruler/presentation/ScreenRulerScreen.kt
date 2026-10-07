package app.quacky.feature.screenruler.presentation

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.component.QuackyChip
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackySurfaceElevated
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.feature.screenruler.domain.RulerMath
import app.quacky.feature.screenruler.domain.RulerUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ScreenRulerScreen(
    viewModel: ScreenRulerViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val snackbarHostState = remember { SnackbarHostState() }
    val textMeasurer = rememberTextMeasurer()

    // Keep screen awake while ruler is open
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveSuccess()
        }
    }

    val metrics = context.resources.displayMetrics
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val baseDpi = if (isLandscape) metrics.xdpi else metrics.ydpi
    val pxPerMm = RulerMath.pixelsPerMm(baseDpi, uiState.calibrationFactor)
    val pxPerInch = RulerMath.pixelsPerInch(baseDpi, uiState.calibrationFactor)

    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }

    val activeSpan = if (isLandscape) canvasWidth else canvasHeight
    val marker1Px = uiState.marker1Fraction * activeSpan
    val marker2Px = uiState.marker2Fraction * activeSpan
    val deltaPx = abs(marker1Px - marker2Px)
    val distanceMm = if (pxPerMm > 0f) deltaPx / pxPerMm else 0f
    val formattedDistance = RulerMath.formatDistance(distanceMm, uiState.selectedUnit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QuackyBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Ruler Top Header
            RulerTopHeader(
                uiState = uiState,
                onBack = onBack,
                onOpenHowToUse = onOpenHowToUse,
                onToggleFlip = { viewModel.toggleFlipped() },
                onToggleGrid = { viewModel.toggleGrid() },
                onOpenCalibration = { viewModel.openCalibration() },
                onShowInfoTip = { viewModel.setShowInfoTip(true) }
            )

            // Main Ruler Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .onSizeChanged { size ->
                        canvasWidth = size.width.toFloat()
                        canvasHeight = size.height.toFloat()
                    }
            ) {
                if (canvasWidth > 0 && canvasHeight > 0) {
                    // Ruler markings & grid Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        drawRulerCanvas(
                            isLandscape = isLandscape,
                            isFlipped = uiState.isFlipped,
                            showGrid = uiState.showGrid,
                            pxPerMm = pxPerMm,
                            pxPerInch = pxPerInch,
                            textMeasurer = textMeasurer
                        )
                    }

                    // Draggable Markers
                    if (!uiState.isLocked) {
                        RulerMarkersLayer(
                            isLandscape = isLandscape,
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            marker1Fraction = uiState.marker1Fraction,
                            marker2Fraction = uiState.marker2Fraction,
                            onMarker1Moved = { viewModel.updateMarker1Fraction(it) },
                            onMarker2Moved = { viewModel.updateMarker2Fraction(it) }
                        )
                    }
                }

                // If locked, show scale honesty gate
                if (uiState.isLocked) {
                    RulerLockedOverlay(onCalibrate = { viewModel.openCalibration() })
                }
            }

            // Bottom Distance Readout Floating Card
            if (!uiState.isLocked) {
                RulerReadoutBar(
                    formattedDistance = formattedDistance,
                    selectedUnit = uiState.selectedUnit,
                    onSelectUnit = { viewModel.setSelectedUnit(it) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Measurement", formattedDistance))
                        Toast.makeText(context, "Copied: $formattedDistance", Toast.LENGTH_SHORT).show()
                    },
                    onSave = { viewModel.openSaveMeasurementDialog() }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // Calibration Dialog
        if (uiState.showCalibrationDialog) {
            RulerCalibrationDialog(
                initialFactor = uiState.calibrationFactor,
                isCalibrated = uiState.isCalibrated,
                onSave = { viewModel.saveCalibration() },
                onReset = { viewModel.resetCalibration() },
                onDismiss = { viewModel.closeCalibration() }
            )
        }

        // Save Measurement Dialog
        if (uiState.showSaveMeasurementDialog) {
            SaveMeasurementDialog(
                formattedDistance = formattedDistance,
                onConfirm = { label ->
                    viewModel.saveMeasurement(label, distanceMm, formattedDistance)
                },
                onDismiss = { viewModel.closeSaveMeasurementDialog() }
            )
        }

        // Info Tip Popover Dialog
        if (uiState.showInfoTipDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setShowInfoTip(false) },
                title = {
                    Text(
                        stringResource(R.string.ruler_calibration_title),
                        color = QuackyTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.ruler_calibration_badge_tip),
                        color = QuackyTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setShowInfoTip(false) }) {
                        Text(stringResource(R.string.action_got_it), color = QuackyAccent)
                    }
                },
                containerColor = QuackySurface,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun RulerTopHeader(
    uiState: ScreenRulerUiState,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    onToggleFlip: () -> Unit,
    onToggleGrid: () -> Unit,
    onOpenCalibration: () -> Unit,
    onShowInfoTip: () -> Unit
) {
    Surface(
        color = QuackySurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = QuackyTextPrimary
                    )
                }

                Text(
                    text = stringResource(R.string.tool_screen_ruler_name),
                    color = QuackyTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Flip 0 Button
                IconButton(onClick = onToggleFlip) {
                    Icon(
                        imageVector = Icons.Rounded.Flip,
                        contentDescription = stringResource(R.string.ruler_flip_toggle),
                        tint = if (uiState.isFlipped) QuackyAccent else QuackyTextSecondary
                    )
                }

                // Grid Toggle Button
                IconButton(onClick = onToggleGrid) {
                    Icon(
                        imageVector = Icons.Rounded.GridOn,
                        contentDescription = stringResource(R.string.ruler_grid_toggle),
                        tint = if (uiState.showGrid) QuackyAccent else QuackyTextSecondary
                    )
                }

                // Help Button
                IconButton(onClick = onOpenHowToUse) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = stringResource(R.string.action_how_to_use),
                        tint = QuackyTextSecondary
                    )
                }
            }

            // Calibration Status Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (uiState.isCalibrated) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenCalibration() }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = QuackyAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.ruler_calibrated),
                            color = QuackyAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = QuackyTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.ruler_not_calibrated),
                            color = QuackyTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onShowInfoTip,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Calibration info",
                                tint = QuackyTextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    QuackyButton(
                        onClick = onOpenCalibration,
                        style = QuackyButtonStyle.Secondary,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ruler_calibrate),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRulerCanvas(
    isLandscape: Boolean,
    isFlipped: Boolean,
    showGrid: Boolean,
    pxPerMm: Float,
    pxPerInch: Float,
    textMeasurer: TextMeasurer
) {
    val width = size.width
    val height = size.height

    if (pxPerMm <= 0f || pxPerInch <= 0f) return

    val rulerLength = if (isLandscape) width else height
    val crossLength = if (isLandscape) height else width

    // 1. Draw mm / cm grid lines if active
    if (showGrid) {
        val gridStepMm = 10f // 10 mm grid lines
        val gridStepPx = gridStepMm * pxPerMm
        var pos = 0f
        while (pos <= rulerLength) {
            val drawPos = if (isFlipped) rulerLength - pos else pos
            if (isLandscape) {
                drawLine(
                    color = QuackyOutline.copy(alpha = 0.4f),
                    start = Offset(drawPos, 0f),
                    end = Offset(drawPos, crossLength),
                    strokeWidth = 1f
                )
            } else {
                drawLine(
                    color = QuackyOutline.copy(alpha = 0.4f),
                    start = Offset(0f, drawPos),
                    end = Offset(crossLength, drawPos),
                    strokeWidth = 1f
                )
            }
            pos += gridStepPx
        }
    }

    // 2. Draw Metric Edge (Left in Portrait, Top in Landscape)
    var mm = 0
    val totalMm = (rulerLength / pxPerMm).toInt()
    while (mm <= totalMm) {
        val posPx = mm * pxPerMm
        val drawPos = if (isFlipped) rulerLength - posPx else posPx

        val tickLen = when {
            mm % 10 == 0 -> 70f // 1 cm
            mm % 5 == 0 -> 45f  // 5 mm
            else -> 25f         // 1 mm
        }

        val stroke = if (mm % 10 == 0) 2.5f else 1.5f
        val color = if (mm % 10 == 0) QuackyTextPrimary else QuackyTextSecondary

        if (isLandscape) {
            drawLine(color, Offset(drawPos, 0f), Offset(drawPos, tickLen), stroke)
            if (mm % 10 == 0 && mm > 0) {
                val cmVal = "${mm / 10}"
                val textLayout = textMeasurer.measure(
                    text = cmVal,
                    style = TextStyle(color = QuackyTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayout, topLeft = Offset(drawPos - textLayout.size.width / 2, tickLen + 4f))
            }
        } else {
            drawLine(color, Offset(0f, drawPos), Offset(tickLen, drawPos), stroke)
            if (mm % 10 == 0 && mm > 0) {
                val cmVal = "${mm / 10}"
                val textLayout = textMeasurer.measure(
                    text = cmVal,
                    style = TextStyle(color = QuackyTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayout, topLeft = Offset(tickLen + 6f, drawPos - textLayout.size.height / 2))
            }
        }
        mm++
    }

    // 3. Draw Imperial Edge (Right in Portrait, Bottom in Landscape)
    // 1/16th subdivisions
    val totalSixteenths = ((rulerLength / pxPerInch) * 16).toInt()
    for (s in 0..totalSixteenths) {
        val posPx = (s.toFloat() / 16f) * pxPerInch
        val drawPos = if (isFlipped) rulerLength - posPx else posPx

        val tickLen = when {
            s % 16 == 0 -> 70f // 1 inch
            s % 8 == 0 -> 50f  // 1/2 inch
            s % 4 == 0 -> 38f  // 1/4 inch
            s % 2 == 0 -> 28f  // 1/8 inch
            else -> 18f        // 1/16 inch
        }

        val stroke = if (s % 16 == 0) 2.5f else 1.5f
        val color = if (s % 16 == 0) QuackyTextPrimary else QuackyTextSecondary

        if (isLandscape) {
            val yStart = height
            val yEnd = height - tickLen
            drawLine(color, Offset(drawPos, yStart), Offset(drawPos, yEnd), stroke)
            if (s % 16 == 0 && s > 0) {
                val inchVal = "${s / 16}"
                val textLayout = textMeasurer.measure(
                    text = inchVal,
                    style = TextStyle(color = QuackyTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayout, topLeft = Offset(drawPos - textLayout.size.width / 2, yEnd - textLayout.size.height - 4f))
            }
        } else {
            val xStart = width
            val xEnd = width - tickLen
            drawLine(color, Offset(xStart, drawPos), Offset(xEnd, drawPos), stroke)
            if (s % 16 == 0 && s > 0) {
                val inchVal = "${s / 16}"
                val textLayout = textMeasurer.measure(
                    text = inchVal,
                    style = TextStyle(color = QuackyTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayout, topLeft = Offset(xEnd - textLayout.size.width - 6f, drawPos - textLayout.size.height / 2))
            }
        }
    }
}

@Composable
private fun RulerMarkersLayer(
    isLandscape: Boolean,
    canvasWidth: Float,
    canvasHeight: Float,
    marker1Fraction: Float,
    marker2Fraction: Float,
    onMarker1Moved: (Float) -> Unit,
    onMarker2Moved: (Float) -> Unit
) {
    val activeSpan = if (isLandscape) canvasWidth else canvasHeight
    val crossSpan = if (isLandscape) canvasHeight else canvasWidth
    if (activeSpan <= 0f) return

    val m1Px = marker1Fraction * activeSpan
    val m2Px = marker2Fraction * activeSpan

    val currentM1 by rememberUpdatedState(m1Px)
    val currentM2 by rememberUpdatedState(m2Px)
    var activeDraggingMarker by remember { mutableStateOf<Int?>(null) }

    val density = LocalDensity.current
    val handleSizeDp = 48.dp
    val handleRadiusPx = with(density) { (handleSizeDp / 2).toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isLandscape, activeSpan) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val touchCoord = if (isLandscape) startOffset.x else startOffset.y
                        val dist1 = kotlin.math.abs(touchCoord - currentM1)
                        val dist2 = kotlin.math.abs(touchCoord - currentM2)
                        activeDraggingMarker = if (dist1 <= dist2) 1 else 2
                    },
                    onDragEnd = { activeDraggingMarker = null },
                    onDragCancel = { activeDraggingMarker = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val delta = if (isLandscape) dragAmount.x else dragAmount.y
                        when (activeDraggingMarker) {
                            1 -> {
                                val newPx = (currentM1 + delta).coerceIn(0f, activeSpan)
                                onMarker1Moved(newPx / activeSpan)
                            }
                            2 -> {
                                val newPx = (currentM2 + delta).coerceIn(0f, activeSpan)
                                onMarker2Moved(newPx / activeSpan)
                            }
                        }
                    }
                )
            }
    ) {
        // Draw both marker lines on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isLandscape) {
                // Marker 1 Line
                drawLine(
                    color = QuackyAccent,
                    start = Offset(m1Px, 0f),
                    end = Offset(m1Px, crossSpan),
                    strokeWidth = 3f
                )
                // Marker 2 Line
                drawLine(
                    color = QuackyAccent,
                    start = Offset(m2Px, 0f),
                    end = Offset(m2Px, crossSpan),
                    strokeWidth = 3f
                )
            } else {
                // Marker 1 Line
                drawLine(
                    color = QuackyAccent,
                    start = Offset(0f, m1Px),
                    end = Offset(crossSpan, m1Px),
                    strokeWidth = 3f
                )
                // Marker 2 Line
                drawLine(
                    color = QuackyAccent,
                    start = Offset(0f, m2Px),
                    end = Offset(crossSpan, m2Px),
                    strokeWidth = 3f
                )
            }
        }

        // Draggable Handle Pills (staggered across crossSpan so they never collide)
        val h1X = if (isLandscape) (m1Px - handleRadiusPx).roundToInt() else ((crossSpan * 0.35f) - handleRadiusPx).roundToInt()
        val h1Y = if (isLandscape) ((crossSpan * 0.35f) - handleRadiusPx).roundToInt() else (m1Px - handleRadiusPx).roundToInt()

        Box(
            modifier = Modifier
                .offset { IntOffset(h1X, h1Y) }
                .size(handleSizeDp)
                .background(QuackyAccent, CircleShape)
                .border(2.dp, QuackyOutline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "1",
                color = QuackyBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val h2X = if (isLandscape) (m2Px - handleRadiusPx).roundToInt() else ((crossSpan * 0.65f) - handleRadiusPx).roundToInt()
        val h2Y = if (isLandscape) ((crossSpan * 0.65f) - handleRadiusPx).roundToInt() else (m2Px - handleRadiusPx).roundToInt()

        Box(
            modifier = Modifier
                .offset { IntOffset(h2X, h2Y) }
                .size(handleSizeDp)
                .background(QuackyAccent, CircleShape)
                .border(2.dp, QuackyOutline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "2",
                color = QuackyBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RulerReadoutBar(
    formattedDistance: String,
    selectedUnit: RulerUnit,
    onSelectUnit: (RulerUnit) -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        color = QuackySurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Tabular Numerals Distance Readout
            Text(
                text = formattedDistance,
                color = QuackyTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Unit Selector & Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Unit Switcher Chips
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RulerUnit.entries.forEach { unit ->
                        QuackyChip(
                            text = unit.symbol,
                            selected = selectedUnit == unit,
                            onClick = { onSelectUnit(unit) }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Copy and Save Measurement Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .size(40.dp)
                            .background(QuackySurfaceElevated, CircleShape)
                            .border(1.dp, QuackyOutline, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.action_copy),
                            tint = QuackyTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onSave,
                        modifier = Modifier
                            .size(40.dp)
                            .background(QuackyAccent, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = stringResource(R.string.ruler_save_measurement),
                            tint = QuackyBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RulerLockedOverlay(onCalibrate: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = QuackySurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = QuackyAccent,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Ruler Locked",
                    color = QuackyTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.ruler_locked_warning),
                    color = QuackyTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                QuackyButton(
                    onClick = onCalibrate,
                    style = QuackyButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.ruler_calibrate))
                }
            }
        }
    }
}

@Composable
private fun SaveMeasurementDialog(
    formattedDistance: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.ruler_save_measurement),
                color = QuackyTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = formattedDistance,
                    color = QuackyAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text(stringResource(R.string.ruler_label_hint)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QuackyAccent,
                        unfocusedBorderColor = QuackyOutline,
                        focusedTextColor = QuackyTextPrimary,
                        unfocusedTextColor = QuackyTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(label) }) {
                Text(stringResource(R.string.action_save), color = QuackyAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), color = QuackyTextSecondary)
            }
        },
        containerColor = QuackySurface,
        shape = RoundedCornerShape(14.dp)
    )
}
