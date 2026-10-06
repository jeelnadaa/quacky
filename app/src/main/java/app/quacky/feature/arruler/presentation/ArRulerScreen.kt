package app.quacky.feature.arruler.presentation

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.quacky.R
import app.quacky.core.components.ToolScaffold
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.component.QuackyCard
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackySurfaceElevated
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolRegistry
import app.quacky.feature.arruler.domain.ArRulerMode
import app.quacky.feature.arruler.domain.ArSingleMeasurement
import app.quacky.feature.arruler.domain.ArTrackingStatus
import app.quacky.feature.arruler.domain.ArUnit
import app.quacky.feature.arruler.domain.MeasurementTones

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArRulerScreen(
    viewModel: ArRulerViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    onNavigateToAreaVolume: ((Double) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current

    var arSurfaceViewRef by remember { mutableStateOf<ArSurfaceView?>(null) }
    var renamingMeasurementId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var selectedForDelete by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isMultiDeleteMode by remember { mutableStateOf(false) }

    // Show toast for snackbar messages
    state.snackbarMessage?.let { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        viewModel.clearSnackbarMessage()
    }

    // Handle send to Area & Volume navigation
    state.sendToAreaVolumeValue?.let { value ->
        onNavigateToAreaVolume?.invoke(value)
        viewModel.clearSendToAreaVolume()
    }

    ToolScaffold(
        tool = ToolRegistry.AR_RULER,
        onBack = onBack,
        isPinned = isPinned,
        onTogglePin = onTogglePin,
        onHistoryClick = { viewModel.toggleHistory() },
        onHelpClick = onOpenHowToUse,
        additionalActions = {
            // Undo button
            IconButton(
                onClick = { viewModel.undoLastPoint() },
                enabled = (state.activeMeasurement?.points?.isNotEmpty() == true) || state.finishedMeasurements.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = stringResource(R.string.ar_action_undo),
                    tint = if ((state.activeMeasurement?.points?.isNotEmpty() == true) || state.finishedMeasurements.isNotEmpty()) {
                        QuackyTextPrimary
                    } else QuackyTextTertiary
                )
            }
            // List / Sheet button
            IconButton(
                onClick = { viewModel.openManageSheet() },
                enabled = state.finishedMeasurements.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                    contentDescription = "Session list",
                    tint = if (state.finishedMeasurements.isNotEmpty()) QuackyTextPrimary else QuackyTextTertiary
                )
            }
            // Clear all button
            IconButton(
                onClick = { viewModel.clearAllMeasurements() },
                enabled = state.finishedMeasurements.isNotEmpty() || state.activeMeasurement != null
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.ar_action_clear),
                    tint = if (state.finishedMeasurements.isNotEmpty() || state.activeMeasurement != null) {
                        QuackyTextPrimary
                    } else QuackyTextTertiary
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(QuackyBackground)
        ) {
            // ARCore Camera Surface
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    ArSurfaceView(
                        context = ctx,
                        onFrameUpdated = { _, _, viewMatrix, projMatrix, hasSurface, reticleHit, width, height ->
                            viewModel.onFrameUpdated(
                                viewMatrix = viewMatrix,
                                projMatrix = projMatrix,
                                hasSurface = hasSurface,
                                reticleHit = reticleHit,
                                viewportWidth = width,
                                viewportHeight = height
                            )
                        },
                        onTap = { worldPos, screenX, screenY ->
                            viewModel.onPointPlaced(worldPos, screenX, screenY)
                        }
                    ).also { surfaceView ->
                        surfaceView.initSession()
                        surfaceView.resumeSession()
                        arSurfaceViewRef = surfaceView
                    }
                },
                update = { surfaceView ->
                    arSurfaceViewRef = surfaceView
                }
            )

            DisposableEffect(Unit) {
                onDispose {
                    arSurfaceViewRef?.pauseSession()
                    arSurfaceViewRef?.destroySession()
                }
            }

            // Measurement Overlay Canvas (Segments & Points for All Measurements)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                        }
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Draw Center Reticle Ring
                val isSurfaceFound = state.trackingStatus == ArTrackingStatus.SURFACE_FOUND
                val reticleAlpha = if (isSurfaceFound) 0.95f else 0.4f
                val reticleRadius = 24.dp.toPx()

                drawCircle(
                    color = Color.White.copy(alpha = reticleAlpha),
                    radius = reticleRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )
                if (isSurfaceFound) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = 3.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }

                // 1. Draw Finished Measurements
                state.finishedMeasurements.forEach { measurement ->
                    val isSelected = (measurement.id == state.selectedMeasurementId)
                    val toneColor = if (isSelected) Color.White else MeasurementTones[measurement.colorToneIndex % MeasurementTones.size]
                    val strokeW = if (isSelected) 3.dp.toPx() else 2.dp.toPx()

                    // Draw segments
                    measurement.segments.forEach { seg ->
                        val p1 = seg.from.screenPoint
                        val p2 = seg.to.screenPoint
                        if (p1 != null && p2 != null) {
                            drawLine(
                                color = toneColor,
                                start = Offset(p1.x, p1.y),
                                end = Offset(p2.x, p2.y),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Draw points
                    measurement.points.forEach { pt ->
                        val sp = pt.screenPoint
                        if (sp != null) {
                            val center = Offset(sp.x, sp.y)
                            drawCircle(color = toneColor, radius = 5.dp.toPx(), center = center)
                            drawCircle(
                                color = toneColor.copy(alpha = if (isSelected) 0.8f else 0.4f),
                                radius = 12.dp.toPx(),
                                center = center,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }

                // 2. Draw Active Measurement (Current in-progress)
                state.activeMeasurement?.let { active ->
                    active.segments.forEach { seg ->
                        val p1 = seg.from.screenPoint
                        val p2 = seg.to.screenPoint
                        if (p1 != null && p2 != null) {
                            drawLine(
                                color = Color.White,
                                start = Offset(p1.x, p1.y),
                                end = Offset(p2.x, p2.y),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    active.points.forEach { pt ->
                        val sp = pt.screenPoint
                        if (sp != null) {
                            val center = Offset(sp.x, sp.y)
                            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = center)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.7f),
                                radius = 14.dp.toPx(),
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            if (pt.isFloorAnchor) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.35f),
                                    radius = 22.dp.toPx(),
                                    center = center,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            // Floating Clickable Chips for Finished Measurements on Screen
            val density = LocalContext.current.resources.displayMetrics.density
            state.finishedMeasurements.forEach { measurement ->
                val midPt = measurement.midPointScreen
                val isSelected = (measurement.id == state.selectedMeasurementId)
                if (midPt != null && midPt.x > 0 && midPt.y > 0) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (midPt.x / density).dp - 40.dp,
                                y = (midPt.y / density).dp - 14.dp
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) QuackyAccent else QuackySurface.copy(alpha = 0.9f))
                            .border(1.dp, if (isSelected) QuackyAccent else QuackyOutline, RoundedCornerShape(12.dp))
                            .clickable { viewModel.selectMeasurement(measurement.id) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${measurement.name} · ${measurement.displayValue}",
                            fontFamily = SatoshiFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (isSelected) QuackyBackground else QuackyTextPrimary
                        )
                    }
                }
            }

            // Floating Distance Pill for Active Measurement
            state.activeMeasurement?.segments?.forEach { seg ->
                if (seg.midScreenX > 0 && seg.midScreenY > 0 && seg.formattedLength.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (seg.midScreenX / density).dp - 32.dp,
                                y = (seg.midScreenY / density).dp - 14.dp
                            )
                            .background(QuackySurface, RoundedCornerShape(12.dp))
                            .border(1.dp, QuackyOutline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = seg.formattedLength,
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = QuackyTextPrimary
                        )
                    }
                }
            }

            // Top Status & Instruction Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val activePtsCount = state.activeMeasurement?.points?.size ?: 0
                val promptText = if (state.activeMeasurement == null) {
                    "Tap surface or press + New to measure"
                } else {
                    when (state.mode) {
                        ArRulerMode.DISTANCE -> if (activePtsCount == 0) stringResource(R.string.ar_hint_tap_start) else stringResource(R.string.ar_hint_tap_end)
                        ArRulerMode.PATH -> if (activePtsCount == 0) stringResource(R.string.ar_hint_tap_start) else "Tap next point or tap tick (✓) to finish"
                        ArRulerMode.HEIGHT -> if (activePtsCount == 0) stringResource(R.string.ar_hint_tap_floor) else stringResource(R.string.ar_hint_tap_top)
                        ArRulerMode.ANGLE -> when (activePtsCount) {
                            0 -> stringResource(R.string.ar_hint_tap_angle_p1)
                            1 -> stringResource(R.string.ar_hint_tap_angle_vertex)
                            else -> stringResource(R.string.ar_hint_tap_angle_p2)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .background(QuackySurface.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                        .border(1.dp, QuackyOutline, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (state.trackingStatus == ArTrackingStatus.SEARCHING_SURFACE) {
                            stringResource(R.string.ar_hint_move_slowly)
                        } else promptText,
                        fontFamily = SatoshiFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = QuackyTextPrimary
                    )
                }

                // One-time Accuracy Note (Dismissible)
                AnimatedVisibility(
                    visible = !state.isAccuracyNoteDismissed,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(QuackySurfaceElevated.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                            .border(1.dp, QuackyOutline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.ar_accuracy_note),
                                fontFamily = SatoshiFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = QuackyTextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_got_it),
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = QuackyAccent,
                                modifier = Modifier
                                    .clickable { viewModel.dismissAccuracyNote() }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Multi-Measurement Strip & Readout Deck
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Bottom Strip: Horizontal Scrollable List of Measurements
                if (state.finishedMeasurements.isNotEmpty() || state.activeMeasurement != null) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "+ New" button chip
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(QuackyAccent)
                                    .clickable { viewModel.startNewMeasurement() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "New measurement",
                                        tint = QuackyBackground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "New",
                                        fontFamily = SatoshiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = QuackyBackground
                                    )
                                }
                            }
                        }

                        // Finished Measurements Chips
                        items(state.finishedMeasurements) { measurement ->
                            val isSelected = (measurement.id == state.selectedMeasurementId)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) QuackySurfaceElevated else QuackySurface)
                                    .border(1.dp, if (isSelected) QuackyAccent else QuackyOutline, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectMeasurement(measurement.id) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${measurement.name} · ${measurement.displayValue}",
                                        fontFamily = SatoshiFontFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = if (isSelected) QuackyAccent else QuackyTextPrimary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Mode Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArRulerMode.entries.forEach { mode ->
                        val isSelected = (state.mode == mode)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) QuackyAccent else QuackySurface)
                                .border(1.dp, if (isSelected) QuackyAccent else QuackyOutline, RoundedCornerShape(10.dp))
                                .clickable { viewModel.setMode(mode) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontFamily = SatoshiFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (isSelected) QuackyBackground else QuackyTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Unit Selector Chips
                if (state.mode != ArRulerMode.ANGLE) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ArUnit.entries) { unit ->
                            val isSelected = (state.unit == unit)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) QuackySurfaceElevated else QuackySurface)
                                    .border(1.dp, if (isSelected) QuackyAccent else QuackyOutline, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setUnit(unit) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unit.label,
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (isSelected) QuackyAccent else QuackyTextSecondary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Selected or Active Measurement Readout Card
                val activeMeasurement = state.activeMeasurement
                val selectedMeasurement = state.finishedMeasurements.firstOrNull { it.id == state.selectedMeasurementId }
                val displayObj: ArSingleMeasurement? = activeMeasurement ?: selectedMeasurement

                QuackyCard(modifier = Modifier.fillMaxWidth()) {
                    val displayVal = displayObj?.displayValue ?: "0.0 ${state.unit.label}"
                    val displayName = displayObj?.name ?: "M1"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "$displayName (${displayObj?.mode?.name ?: state.mode.name})",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = QuackyTextSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = displayVal,
                                fontFamily = SatoshiFontFamily,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = QuackyTextPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Finish / Tick Button for active measurement
                            if (activeMeasurement != null && activeMeasurement.points.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.finishActiveMeasurement() },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(QuackyAccent, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Finish measurement",
                                        tint = QuackyBackground,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Copy Button
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(displayVal))
                                    Toast.makeText(context, context.getString(R.string.ar_copied), Toast.LENGTH_SHORT).show()
                                },
                                enabled = displayVal != "0.0 ${state.unit.label}",
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(QuackySurfaceElevated, CircleShape)
                                    .border(1.dp, QuackyOutline, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = stringResource(R.string.action_copy),
                                    tint = QuackyTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Screenshot Button
                            IconButton(
                                onClick = {
                                    val bitmap = Bitmap.createBitmap(
                                        view.width.coerceAtLeast(1),
                                        view.height.coerceAtLeast(1),
                                        Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = android.graphics.Canvas(bitmap)
                                    view.draw(canvas)
                                    viewModel.captureScreenshot(bitmap, context)
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(QuackySurfaceElevated, CircleShape)
                                    .border(1.dp, QuackyOutline, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = stringResource(R.string.ar_action_capture),
                                    tint = QuackyTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Action strip for selected finished measurement
                    if (selectedMeasurement != null && activeMeasurement == null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = QuackyOutline, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rename action
                            Text(
                                text = "Rename",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 12.sp,
                                color = QuackyTextSecondary,
                                modifier = Modifier
                                    .clickable {
                                        renamingMeasurementId = selectedMeasurement.id
                                        renameText = selectedMeasurement.name
                                    }
                                    .padding(4.dp)
                            )
                            // Edit Points action
                            Text(
                                text = "Edit points",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 12.sp,
                                color = QuackyTextSecondary,
                                modifier = Modifier
                                    .clickable { viewModel.editMeasurementPoints(selectedMeasurement.id) }
                                    .padding(4.dp)
                            )
                            // Favorite action
                            IconButton(
                                onClick = { viewModel.toggleFavorite(selectedMeasurement.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (selectedMeasurement.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (selectedMeasurement.isFavorite) Color.White else QuackyTextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            // Send to Area & Volume
                            if (selectedMeasurement.totalValueMeters > 0f) {
                                Text(
                                    text = "Send to A&V",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = QuackyAccent,
                                    modifier = Modifier
                                        .clickable { viewModel.sendMeasurementToAreaVolume(selectedMeasurement.totalValueMeters.toDouble()) }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Limit Reached Dialog (20 measurements limit)
    if (state.isSessionLimitReached) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLimitDialog() },
            containerColor = QuackySurfaceElevated,
            title = {
                Text(
                    text = "Session Limit Reached",
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = QuackyTextPrimary
                )
            },
            text = {
                Text(
                    text = "You have reached the maximum of 20 measurements in this session. Start a new session or delete existing measurements to continue.",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 14.sp,
                    color = QuackyTextSecondary
                )
            },
            confirmButton = {
                QuackyButton(
                    onClick = {
                        viewModel.clearAllMeasurements()
                        viewModel.dismissLimitDialog()
                    },
                    style = QuackyButtonStyle.Primary
                ) {
                    Text("Start New Session")
                }
            },
            dismissButton = {
                QuackyButton(
                    onClick = { viewModel.dismissLimitDialog() },
                    style = QuackyButtonStyle.Secondary
                ) {
                    Text("Manage Existing")
                }
            }
        )
    }

    // Rename Measurement Dialog
    renamingMeasurementId?.let { mId ->
        AlertDialog(
            onDismissRequest = { renamingMeasurementId = null },
            containerColor = QuackySurfaceElevated,
            title = {
                Text(
                    text = "Rename Measurement",
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = QuackyTextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QuackyAccent,
                        unfocusedBorderColor = QuackyOutline,
                        focusedTextColor = QuackyTextPrimary,
                        unfocusedTextColor = QuackyTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                QuackyButton(
                    onClick = {
                        viewModel.renameMeasurement(mId, renameText)
                        renamingMeasurementId = null
                    },
                    style = QuackyButtonStyle.Primary
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                QuackyButton(
                    onClick = { renamingMeasurementId = null },
                    style = QuackyButtonStyle.Secondary
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Expandable Full Session List Bottom Sheet
    if (state.isSessionListOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSessionList() },
            sheetState = sheetState,
            containerColor = QuackySurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${state.sessionName} (${state.finishedMeasurements.size}/20)",
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = QuackyTextPrimary
                        )
                        Text(
                            text = "Tap to select, rename, or export",
                            fontFamily = SatoshiFontFamily,
                            fontSize = 12.sp,
                            color = QuackyTextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Copy All button
                        IconButton(
                            onClick = {
                                val summary = viewModel.copyAllMeasurementsSummary()
                                clipboardManager.setText(AnnotatedString(summary))
                                Toast.makeText(context, "Copied all measurements", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy all",
                                tint = QuackyTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Multi-delete mode toggle
                        IconButton(
                            onClick = {
                                isMultiDeleteMode = !isMultiDeleteMode
                                selectedForDelete = emptySet()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Multi delete",
                                tint = if (isMultiDeleteMode) QuackyAccent else QuackyTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (isMultiDeleteMode && selectedForDelete.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    QuackyButton(
                        onClick = {
                            viewModel.deleteMultipleMeasurements(selectedForDelete)
                            selectedForDelete = emptySet()
                            isMultiDeleteMode = false
                        },
                        style = QuackyButtonStyle.Primary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Selected (${selectedForDelete.size})")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.finishedMeasurements) { measurement ->
                        QuackyCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectMeasurement(measurement.id)
                                    viewModel.closeSessionList()
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (isMultiDeleteMode) {
                                    Checkbox(
                                        checked = selectedForDelete.contains(measurement.id),
                                        onCheckedChange = { checked ->
                                            selectedForDelete = if (checked) {
                                                selectedForDelete + measurement.id
                                            } else {
                                                selectedForDelete - measurement.id
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = QuackyAccent,
                                            checkmarkColor = QuackyBackground
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = measurement.name,
                                        fontFamily = SatoshiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = QuackyTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${measurement.displayValue} · ${measurement.mode.name}",
                                        fontFamily = SatoshiFontFamily,
                                        fontSize = 13.sp,
                                        color = QuackyTextSecondary
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            renamingMeasurementId = measurement.id
                                            renameText = measurement.name
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Rename",
                                            tint = QuackyTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteMeasurement(measurement.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = QuackyTextTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Static Session Detail Viewer (When opened from Room history)
    state.viewingSessionDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = { viewModel.closeSessionDetail() },
            containerColor = QuackySurfaceElevated,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = detail.sessionName,
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = QuackyTextPrimary
                    )
                    IconButton(
                        onClick = { viewModel.closeSessionDetail() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = QuackyTextSecondary
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${detail.measurements.size} measurements in this session",
                        fontFamily = SatoshiFontFamily,
                        fontSize = 13.sp,
                        color = QuackyTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(detail.measurements) { m ->
                            QuackyCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = m.name,
                                            fontFamily = SatoshiFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = QuackyTextPrimary
                                        )
                                        Text(
                                            text = "${m.displayValue} (${m.mode.name})",
                                            fontFamily = SatoshiFontFamily,
                                            fontSize = 12.sp,
                                            color = QuackyTextSecondary
                                        )
                                    }
                                    if (m.totalValueMeters > 0f) {
                                        Text(
                                            text = "Send to A&V",
                                            fontFamily = SatoshiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = QuackyAccent,
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.sendMeasurementToAreaVolume(m.totalValueMeters.toDouble())
                                                    viewModel.closeSessionDetail()
                                                }
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                QuackyButton(
                    onClick = {
                        val sb = StringBuilder()
                        sb.appendLine("${detail.sessionName}:")
                        detail.measurements.forEach { m ->
                            sb.appendLine("• ${m.name}: ${m.displayValue}")
                        }
                        clipboardManager.setText(AnnotatedString(sb.toString()))
                        Toast.makeText(context, "Copied session summary", Toast.LENGTH_SHORT).show()
                    },
                    style = QuackyButtonStyle.Primary
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Summary")
                    }
                }
            },
            dismissButton = {
                QuackyButton(
                    onClick = { viewModel.closeSessionDetail() },
                    style = QuackyButtonStyle.Secondary
                ) {
                    Text("Close")
                }
            }
        )
    }

    // AR Ruler History Bottom Sheet
    if (state.isHistoryOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleHistory() },
            sheetState = sheetState,
            containerColor = QuackySurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AR Sessions History",
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = QuackyTextPrimary
                    )
                    if (state.activeHistoryEntries.isNotEmpty()) {
                        Text(
                            text = "Clear all",
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = QuackyTextSecondary,
                            modifier = Modifier
                                .clickable { viewModel.clearAllHistory() }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.activeHistoryEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved sessions yet.",
                            fontFamily = SatoshiFontFamily,
                            fontSize = 14.sp,
                            color = QuackyTextTertiary
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        items(state.activeHistoryEntries) { entry ->
                            QuackyCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.openSessionDetail(entry) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.title,
                                            fontFamily = SatoshiFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp,
                                            color = QuackyTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = entry.subtitle,
                                            fontFamily = SatoshiFontFamily,
                                            fontSize = 12.sp,
                                            color = QuackyTextSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteHistoryEntry(entry.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = QuackyTextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
