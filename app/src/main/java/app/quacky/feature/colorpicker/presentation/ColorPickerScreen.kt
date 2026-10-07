package app.quacky.feature.colorpicker.presentation

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.camera.CameraPreview
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
import app.quacky.core.permission.PermissionGate
import app.quacky.feature.colorpicker.domain.SampleSize
import kotlin.math.roundToInt

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerScreen(
    viewModel: ColorPickerViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val colorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val paletteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadGalleryImageUri(uri)
        }
    }

    LaunchedEffect(uiState.infoMessage, uiState.errorMessage) {
        val msg = uiState.infoMessage ?: uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }
    var touchScreenOffset by remember { mutableStateOf(Offset.Zero) }
    var latestCameraBitmap by remember { mutableStateOf<Bitmap?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QuackyBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Surface(
                color = QuackySurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
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
                        text = stringResource(R.string.tool_color_picker_name),
                        color = QuackyTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onOpenHowToUse) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = stringResource(R.string.action_how_to_use),
                            tint = QuackyTextSecondary
                        )
                    }
                }
            }

            // Source Selector Tabs: Camera | Gallery
            TabRow(
                selectedTabIndex = uiState.source.ordinal,
                containerColor = QuackySurface,
                contentColor = QuackyTextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.source.ordinal]),
                        color = QuackyAccent
                    )
                }
            ) {
                Tab(
                    selected = uiState.source == ColorSource.CAMERA,
                    onClick = { viewModel.setSource(ColorSource.CAMERA) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera")
                        }
                    }
                )
                Tab(
                    selected = uiState.source == ColorSource.GALLERY,
                    onClick = {
                        viewModel.setSource(ColorSource.GALLERY)
                        if (uiState.galleryBitmap == null) {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery")
                        }
                    }
                )
            }

            // Main Interactive Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .onSizeChanged { size ->
                        canvasWidth = size.width.toFloat()
                        canvasHeight = size.height.toFloat()
                    }
            ) {
                val activeBitmap = when (uiState.source) {
                    ColorSource.CAMERA -> if (uiState.isFrozen) uiState.frozenBitmap else latestCameraBitmap
                    ColorSource.GALLERY -> uiState.galleryBitmap
                }

                if (uiState.source == ColorSource.CAMERA && !uiState.isFrozen) {
                    PermissionGate(
                        permission = android.Manifest.permission.CAMERA,
                        title = "Camera needed for live color picking",
                        rationale = stringResource(R.string.camera_permission_rationale)
                    ) {
                        val analyzer = remember {
                            ImageAnalysis.Analyzer { imageProxy ->
                                val bitmap = imageProxy.toBitmap()
                                latestCameraBitmap = bitmap
                                viewModel.sampleFromBitmap(bitmap)
                                imageProxy.close()
                            }
                        }

                        CameraPreview(
                            torchEnabled = false,
                            imageAnalyzer = analyzer,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else if (activeBitmap != null) {
                    Image(
                        bitmap = activeBitmap.asImageBitmap(),
                        contentDescription = "Active frame",
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (uiState.source == ColorSource.GALLERY) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        QuackyButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            style = QuackyButtonStyle.Primary
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Image")
                        }
                    }
                }

                // Interactive Reticle Layer
                if (canvasWidth > 0 && canvasHeight > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    touchScreenOffset = offset
                                    viewModel.updatePinPosition(offset.x / canvasWidth, offset.y / canvasHeight)
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        touchScreenOffset = offset
                                        viewModel.setDragging(true)
                                        viewModel.updatePinPosition(offset.x / canvasWidth, offset.y / canvasHeight)
                                    },
                                    onDragEnd = { viewModel.setDragging(false) },
                                    onDragCancel = { viewModel.setDragging(false) },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        touchScreenOffset += dragAmount
                                        viewModel.updatePinPosition(
                                            touchScreenOffset.x / canvasWidth,
                                            touchScreenOffset.y / canvasHeight
                                        )
                                    }
                                )
                            }
                    ) {
                        // Render all pins
                        uiState.pins.forEachIndexed { index, pin ->
                            val isSelected = index == uiState.activePinIndex
                            val pinX = pin.xNorm * canvasWidth
                            val pinY = pin.yNorm * canvasHeight

                            ReticleCrosshair(
                                xPx = pinX,
                                yPx = pinY,
                                pinNumber = pin.id,
                                isSelected = isSelected,
                                sampleSize = uiState.sampleSize
                            )
                        }

                        // Render 8x Loupe while dragging
                        if (uiState.isDraggingReticle && activeBitmap != null) {
                            val activePin = uiState.activePin
                            if (activePin != null) {
                                val centerPxX = (activePin.xNorm * activeBitmap.width).roundToInt()
                                val centerPxY = (activePin.yNorm * activeBitmap.height).roundToInt()
                                LoupeMagnifier(
                                    bitmap = activeBitmap,
                                    centerPxX = centerPxX,
                                    centerPxY = centerPxY,
                                    currentColorHex = activePin.hex,
                                    screenOffsetX = touchScreenOffset.x,
                                    screenOffsetY = touchScreenOffset.y
                                )
                            }
                        }
                    }
                }

                // 1px Arrow Nudge Overlay Pad (Floating at top-right)
                if (activeBitmap != null) {
                    NudgeControlsPad(
                        onNudge = { dx, dy ->
                            viewModel.nudgeActivePin(dx, dy, activeBitmap.width, activeBitmap.height)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                }

                // Active Pin Coordinates readout chip (top-left)
                uiState.activePin?.let { pin ->
                    if (activeBitmap != null) {
                        val pxX = (pin.xNorm * activeBitmap.width).roundToInt()
                        val pxY = (pin.yNorm * activeBitmap.height).roundToInt()
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(QuackySurface.copy(alpha = 0.85f))
                                .border(1.dp, QuackyOutline, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "X: $pxX  Y: $pxY  (${activeBitmap.width}×${activeBitmap.height})",
                                color = QuackyTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Multi-Pin Bottom Strip (Up to 8 pins)
            MultiPinStrip(
                pins = uiState.pins,
                activePinIndex = uiState.activePinIndex,
                onSelectPin = { viewModel.selectPin(it) },
                onAddPin = { viewModel.addPin() },
                onDeletePin = { viewModel.removePin(it) }
            )

            // Bottom Actions & Inspection Bar
            BottomControlsBar(
                uiState = uiState,
                onToggleFreeze = { viewModel.toggleFreeze(latestCameraBitmap) },
                onSelectSampleSize = { viewModel.setSampleSize(it) },
                onExtractPalette = { viewModel.extractPaletteFromActiveImage() },
                onOpenPalettes = { viewModel.openPaletteSheet() },
                onInspectColor = { viewModel.openColorSheet() },
                onSaveColor = { viewModel.saveColorToHistory() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        )

        // Color Inspection Bottom Sheet
        if (uiState.showColorSheet) {
            val activePin = uiState.activePin
            val activeFormats = uiState.activeColorFormats
            if (activePin != null && activeFormats != null) {
                ColorReadoutSheet(
                    pin = activePin,
                    formats = activeFormats,
                    contrastColor = uiState.contrastComparisonColor,
                    sheetState = colorSheetState,
                    onSaveToHistory = { viewModel.saveColorToHistory() },
                    onSelectComparisonColor = { viewModel.setContrastComparisonColor(it) },
                    onDismiss = { viewModel.closeColorSheet() }
                )
            }
        }

        // Palette Bottom Sheet
        if (uiState.showPaletteSheet) {
            PaletteSheet(
                pins = uiState.pins,
                savedPalettes = uiState.savedPalettes,
                sheetState = paletteSheetState,
                onSaveCurrentPalette = { viewModel.savePalette(it) },
                onDeletePalette = { viewModel.deletePalette(it) },
                onDismiss = { viewModel.closePaletteSheet() }
            )
        }
    }
}

@Composable
private fun ReticleCrosshair(
    xPx: Float,
    yPx: Float,
    pinNumber: Int,
    isSelected: Boolean,
    sampleSize: SampleSize
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val crossLen = 22.dp.toPx()
        val gap = 5.dp.toPx()
        val stroke = if (isSelected) 2.5f else 1.5f
        val color = if (isSelected) QuackyAccent else QuackyTextSecondary

        // Horizontal lines
        drawLine(color, Offset(xPx - crossLen, yPx), Offset(xPx - gap, yPx), stroke)
        drawLine(color, Offset(xPx + gap, yPx), Offset(xPx + crossLen, yPx), stroke)

        // Vertical lines
        drawLine(color, Offset(xPx, yPx - crossLen), Offset(xPx, yPx - gap), stroke)
        drawLine(color, Offset(xPx, yPx + gap), Offset(xPx, yPx + crossLen), stroke)

        // Center hollow square
        drawRect(
            color = color,
            topLeft = Offset(xPx - gap, yPx - gap),
            size = Size(gap * 2, gap * 2),
            style = Stroke(1.5f)
        )
    }

    // Numbered Badge above reticle
    Box(
        modifier = Modifier
            .offset {
                IntOffset((xPx - 10 * 2.7f).roundToInt(), (yPx - 34 * 2.7f).roundToInt())
            }
            .size(20.dp)
            .clip(CircleShape)
            .background(if (isSelected) QuackyAccent else QuackySurfaceElevated)
            .border(1.dp, QuackyOutline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$pinNumber",
            color = if (isSelected) Color.Black else QuackyTextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NudgeControlsPad(onNudge: (Int, Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = QuackySurface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = { onNudge(0, -1) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Nudge Up 1px", tint = QuackyTextPrimary, modifier = Modifier.size(16.dp))
            }
            Row {
                IconButton(onClick = { onNudge(-1, 0) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Nudge Left 1px", tint = QuackyTextPrimary, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = { onNudge(1, 0) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Nudge Right 1px", tint = QuackyTextPrimary, modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = { onNudge(0, 1) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Nudge Down 1px", tint = QuackyTextPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MultiPinStrip(
    pins: List<ColorPin>,
    activePinIndex: Int,
    onSelectPin: (Int) -> Unit,
    onAddPin: () -> Unit,
    onDeletePin: (Int) -> Unit
) {
    Surface(
        color = QuackySurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pins.forEachIndexed { index, pin ->
                    val isSelected = index == activePinIndex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(pin.color))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) QuackyAccent else QuackyOutline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .pointerInput(pin.id) {
                                detectTapGestures(
                                    onTap = { onSelectPin(index) },
                                    onLongPress = { onDeletePin(index) }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${pin.id}",
                            color = if (androidx.core.graphics.ColorUtils.calculateLuminance(pin.color) > 0.5) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (pins.size < 8) {
                IconButton(
                    onClick = onAddPin,
                    modifier = Modifier
                        .size(36.dp)
                        .background(QuackySurfaceElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, QuackyOutline, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Pin", tint = QuackyTextPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun BottomControlsBar(
    uiState: ColorPickerUiState,
    onToggleFreeze: () -> Unit,
    onSelectSampleSize: (SampleSize) -> Unit,
    onExtractPalette: () -> Unit,
    onOpenPalettes: () -> Unit,
    onInspectColor: () -> Unit,
    onSaveColor: () -> Unit
) {
    Surface(
        color = QuackySurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Sampling size chips & Freeze button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SampleSize.entries.forEach { size ->
                        QuackyChip(
                            text = size.label,
                            selected = uiState.sampleSize == size,
                            onClick = { onSelectSampleSize(size) }
                        )
                    }
                }

                if (uiState.source == ColorSource.CAMERA) {
                    QuackyButton(
                        onClick = onToggleFreeze,
                        style = if (uiState.isFrozen) QuackyButtonStyle.Primary else QuackyButtonStyle.Secondary,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.AcUnit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (uiState.isFrozen) "Unfreeze" else "Freeze", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inspection banner & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                uiState.activePin?.let { pin ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onInspectColor() }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(pin.color))
                                .border(1.dp, QuackyOutline, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(pin.hex, color = QuackyTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(pin.colorName, color = QuackyTextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onExtractPalette,
                        modifier = Modifier
                            .size(36.dp)
                            .background(QuackySurfaceElevated, CircleShape)
                            .border(1.dp, QuackyOutline, CircleShape)
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = "Extract Palette", tint = QuackyTextPrimary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onOpenPalettes,
                        modifier = Modifier
                            .size(36.dp)
                            .background(QuackySurfaceElevated, CircleShape)
                            .border(1.dp, QuackyOutline, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = "Palettes", tint = QuackyTextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
