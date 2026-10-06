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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import app.quacky.feature.arruler.domain.ArTrackingStatus
import app.quacky.feature.arruler.domain.ArUnit

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
    var saveLabel by remember { mutableStateOf("") }

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
            IconButton(
                onClick = { viewModel.undoLastPoint() },
                enabled = state.points.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = stringResource(R.string.ar_action_undo),
                    tint = if (state.points.isNotEmpty()) QuackyTextPrimary else QuackyTextTertiary
                )
            }
            IconButton(
                onClick = { viewModel.clearAll() },
                enabled = state.points.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.ar_action_clear),
                    tint = if (state.points.isNotEmpty()) QuackyTextPrimary else QuackyTextTertiary
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

            // Measurement Overlay Canvas (Segments & Points)
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

                // Draw Reticle Ring
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

                // Draw Segments
                state.segments.forEach { seg ->
                    val p1 = seg.from.screenPoint
                    val p2 = seg.to.screenPoint
                    if (p1 != null && p2 != null) {
                        drawLine(
                            color = Color.White,
                            start = Offset(p1.x, p1.y),
                            end = Offset(p2.x, p2.y),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Draw Placed Points
                state.points.forEach { point ->
                    val screenPt = point.screenPoint
                    if (screenPt != null) {
                        val ptOffset = Offset(screenPt.x, screenPt.y)
                        // Inner solid dot
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = ptOffset
                        )
                        // Outer ring
                        drawCircle(
                            color = Color.White.copy(alpha = 0.6f),
                            radius = 14.dp.toPx(),
                            center = ptOffset,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        if (point.isFloorAnchor) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.35f),
                                radius = 22.dp.toPx(),
                                center = ptOffset,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Floating Distance Labels
            state.segments.forEach { seg ->
                if (seg.midScreenX > 0 && seg.midScreenY > 0 && seg.formattedLength.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (seg.midScreenX / LocalContext.current.resources.displayMetrics.density).dp - 32.dp,
                                y = (seg.midScreenY / LocalContext.current.resources.displayMetrics.density).dp - 14.dp
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
                // Tracking or Mode Prompt Hint
                val promptText = when (state.mode) {
                    ArRulerMode.DISTANCE -> {
                        when (state.points.size) {
                            0 -> stringResource(R.string.ar_hint_tap_start)
                            1 -> stringResource(R.string.ar_hint_tap_end)
                            else -> stringResource(R.string.ar_hint_surface_found)
                        }
                    }
                    ArRulerMode.PATH -> {
                        if (state.points.isEmpty()) stringResource(R.string.ar_hint_tap_start)
                        else stringResource(R.string.ar_hint_tap_next)
                    }
                    ArRulerMode.HEIGHT -> {
                        when (state.points.size) {
                            0 -> stringResource(R.string.ar_hint_tap_floor)
                            1 -> stringResource(R.string.ar_hint_tap_top)
                            else -> stringResource(R.string.ar_hint_surface_found)
                        }
                    }
                    ArRulerMode.ANGLE -> {
                        when (state.points.size) {
                            0 -> stringResource(R.string.ar_hint_tap_angle_p1)
                            1 -> stringResource(R.string.ar_hint_tap_angle_vertex)
                            2 -> stringResource(R.string.ar_hint_tap_angle_p2)
                            else -> stringResource(R.string.ar_hint_surface_found)
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
                        } else {
                            promptText
                        },
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

            // Bottom Measurement & Controls Card
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Mode Chips Row
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
                            val modeTitle = when (mode) {
                                ArRulerMode.DISTANCE -> stringResource(R.string.ar_mode_distance)
                                ArRulerMode.PATH -> stringResource(R.string.ar_mode_path)
                                ArRulerMode.HEIGHT -> stringResource(R.string.ar_mode_height)
                                ArRulerMode.ANGLE -> stringResource(R.string.ar_mode_angle)
                            }
                            Text(
                                text = modeTitle,
                                fontFamily = SatoshiFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (isSelected) QuackyBackground else QuackyTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Unit Selector Row
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

                // Readout and Action Panel Card
                QuackyCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayValue = if (state.mode == ArRulerMode.ANGLE) {
                        state.formattedAngle ?: "—"
                    } else {
                        if (state.totalValueMeters > 0f) state.formattedTotal else "0.0 ${state.unit.label}"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = when (state.mode) {
                                ArRulerMode.DISTANCE -> stringResource(R.string.ar_mode_distance)
                                ArRulerMode.PATH -> stringResource(R.string.ar_mode_path)
                                ArRulerMode.HEIGHT -> stringResource(R.string.ar_mode_height)
                                ArRulerMode.ANGLE -> stringResource(R.string.ar_mode_angle)
                            },
                            fontFamily = SatoshiFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = QuackyTextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayValue,
                            fontFamily = SatoshiFontFamily,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuackyTextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Copy Button
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(displayValue))
                                Toast.makeText(context, context.getString(R.string.ar_copied), Toast.LENGTH_SHORT).show()
                            },
                            enabled = displayValue != "—" && displayValue != "0.0 ${state.unit.label}",
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

                        // Capture Screenshot Button
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
                                .background(QuackyAccent, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = stringResource(R.string.ar_action_capture),
                                tint = QuackyBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Bottom Actions: Send to Area & Volume or Save
                if (state.totalValueMeters > 0f && state.mode != ArRulerMode.ANGLE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuackyButton(
                            onClick = { viewModel.openSaveDialog() },
                            style = QuackyButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.ar_save_measurement),
                                fontSize = 13.sp
                            )
                        }

                        QuackyButton(
                            onClick = { viewModel.prepareSendToAreaVolume() },
                            style = QuackyButtonStyle.Primary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.ar_action_send_to_area_volume),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Save Label Dialog
    if (state.isSaveDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.closeSaveDialog() },
            containerColor = QuackySurfaceElevated,
            title = {
                Text(
                    text = stringResource(R.string.ar_save_measurement),
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = QuackyTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a label for this measurement:",
                        fontFamily = SatoshiFontFamily,
                        fontSize = 14.sp,
                        color = QuackyTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveLabel.ifBlank { state.defaultLabel },
                        onValueChange = { saveLabel = it },
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
                QuackyButton(
                    onClick = {
                        viewModel.saveMeasurement(saveLabel.ifBlank { state.defaultLabel })
                    },
                    style = QuackyButtonStyle.Primary
                ) {
                    Text(text = stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                QuackyButton(
                    onClick = { viewModel.closeSaveDialog() },
                    style = QuackyButtonStyle.Secondary
                ) {
                    Text(text = stringResource(R.string.action_close))
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
                        text = "AR Measurements History",
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
                            text = "No saved measurements yet.",
                            fontFamily = SatoshiFontFamily,
                            fontSize = 14.sp,
                            color = QuackyTextTertiary
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        state.activeHistoryEntries.forEach { entry ->
                            QuackyCard(
                                modifier = Modifier.fillMaxWidth()
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
                                            fontSize = 14.sp,
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
