package app.quacky.feature.documentscanner.presentation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import app.quacky.feature.documentscanner.domain.DocumentProcessor
import app.quacky.feature.documentscanner.domain.LiveEdgeDetector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import app.quacky.R
import app.quacky.core.camera.CameraPreview
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
import app.quacky.feature.documentscanner.domain.CornerPoint
import app.quacky.feature.documentscanner.domain.DocFilterType
import app.quacky.feature.documentscanner.domain.DocumentQuad
import java.io.InputStream
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(
    viewModel: DocumentScannerViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    state.snackbarMessage?.let { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        viewModel.clearSnackbarMessage()
    }

    // Photo picker from gallery
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val bitmaps = uris.mapNotNull { uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmaps.isNotEmpty()) {
                viewModel.onPhotosImported(bitmaps)
            }
        }
    }

    ToolScaffold(
        tool = ToolRegistry.DOCUMENT_SCANNER,
        onBack = onBack,
        isPinned = isPinned,
        onTogglePin = onTogglePin,
        onHelpClick = onOpenHowToUse,
        additionalActions = {
            if (state.step == DocScanStep.REVIEW && state.pages.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.exportPdf(context) },
                    enabled = !state.isGeneratingPdf
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "Export PDF",
                        tint = QuackyAccent
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(QuackyBackground)
        ) {
            when (state.step) {
                DocScanStep.CAPTURE -> {
                    CameraCaptureView(
                        flashEnabled = state.flashEnabled,
                        autoCropEnabled = state.isAutoCropEnabled,
                        liveDetectedQuad = state.liveDetectedQuad,
                        pagesCount = state.pages.size,
                        onPhotoCaptured = { bitmap, quad -> viewModel.onPhotoCaptured(bitmap, quad) },
                        onPickGallery = { galleryPicker.launch("image/*") },
                        onToggleFlash = { viewModel.toggleFlash() },
                        onToggleAutoCrop = { viewModel.toggleAutoCrop() },
                        onEdgeDetected = { viewModel.updateLiveDetectedQuad(it) },
                        onDone = {
                            if (state.pages.isNotEmpty()) {
                                viewModel.goToReview()
                            }
                        }
                    )
                }

                DocScanStep.CROP -> {
                    val curPage = state.currentPage
                    if (curPage != null) {
                        QuadCropView(
                            bitmap = curPage.originalBitmap,
                            quad = state.activeCropQuad,
                            isProcessing = state.isProcessing,
                            onCornerDragged = { corner, pt ->
                                viewModel.updateCropCorner(corner, pt)
                            },
                            onResetFull = { viewModel.resetCropToFull() },
                            onResetInset = { viewModel.resetCropToInset() },
                            onApply = { viewModel.applyCrop() },
                            onCancel = { viewModel.goToCapture() }
                        )
                    }
                }

                DocScanStep.REVIEW -> {
                    ReviewPagesView(
                        state = state,
                        onSelectPage = { viewModel.selectPage(it) },
                        onAddPage = { viewModel.goToCapture() },
                        onSetFilter = { viewModel.setFilter(it) },
                        onRotate = { viewModel.rotateCurrentPage() },
                        onRecrop = { viewModel.retakeOrRecropCurrentPage() },
                        onDelete = { viewModel.deleteCurrentPage() },
                        onExportPdf = { viewModel.exportPdf(context) }
                    )
                }
            }

            if (state.isGeneratingPdf || state.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = QuackyAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (state.isGeneratingPdf) "Compiling PDF..." else "Enhancing document...",
                            fontFamily = SatoshiFontFamily,
                            color = QuackyTextPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // PDF Export Success Dialog
    state.generatedPdfFile?.let { pdfFile ->
        AlertDialog(
            onDismissRequest = { viewModel.clearGeneratedPdf() },
            containerColor = QuackySurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = QuackyAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PDF Generated",
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = QuackyTextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = pdfFile.name,
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = QuackyTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.pages.size} pages · ${pdfFile.length() / 1024} KB",
                        fontFamily = SatoshiFontFamily,
                        fontSize = 13.sp,
                        color = QuackyTextSecondary
                    )
                }
            },
            confirmButton = {
                QuackyButton(
                    onClick = {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            pdfFile
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Document PDF"))
                    },
                    style = QuackyButtonStyle.Primary
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF")
                    }
                }
            },
            dismissButton = {
                QuackyButton(
                    onClick = { viewModel.clearGeneratedPdf() },
                    style = QuackyButtonStyle.Secondary
                ) {
                    Text(stringResource(R.string.action_done))
                }
            }
        )
    }
}

@Composable
private fun CameraCaptureView(
    flashEnabled: Boolean,
    autoCropEnabled: Boolean,
    liveDetectedQuad: DocumentQuad?,
    pagesCount: Int,
    onPhotoCaptured: (Bitmap, DocumentQuad?) -> Unit,
    onPickGallery: () -> Unit,
    onToggleFlash: () -> Unit,
    onToggleAutoCrop: () -> Unit,
    onEdgeDetected: (DocumentQuad?) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    val edgeDetector = remember(autoCropEnabled) {
        if (autoCropEnabled) {
            LiveEdgeDetector { detected ->
                onEdgeDetected(detected)
            }
        } else null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            torchEnabled = flashEnabled,
            imageCapture = imageCapture,
            imageAnalyzer = edgeDetector
        )

        // Live Auto-Crop Quadrilateral Overlay
        if (autoCropEnabled && liveDetectedQuad != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val q = liveDetectedQuad
                val pTL = Offset(q.topLeft.x * size.width, q.topLeft.y * size.height)
                val pTR = Offset(q.topRight.x * size.width, q.topRight.y * size.height)
                val pBR = Offset(q.bottomRight.x * size.width, q.bottomRight.y * size.height)
                val pBL = Offset(q.bottomLeft.x * size.width, q.bottomLeft.y * size.height)

                val quadPath = Path().apply {
                    moveTo(pTL.x, pTL.y)
                    lineTo(pTR.x, pTR.y)
                    lineTo(pBR.x, pBR.y)
                    lineTo(pBL.x, pBL.y)
                    close()
                }

                // Shaded tint inside detected document
                drawPath(path = quadPath, color = Color(0x2A00E5FF))
                // Boundary stroke
                drawPath(path = quadPath, color = Color(0xFF00E5FF), style = Stroke(width = 2.5.dp.toPx()))

                // 4 corner pins
                listOf(pTL, pTR, pBR, pBL).forEach { pt ->
                    drawCircle(color = Color(0xFF00E5FF), radius = 6.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = pt)
                }
            }
        }

        // Top Flash, Auto-Crop Toggle and Status controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleFlash,
                modifier = Modifier
                    .size(40.dp)
                    .background(QuackySurface.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(
                    imageVector = if (flashEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    contentDescription = "Flash",
                    tint = if (flashEnabled) QuackyAccent else QuackyTextPrimary
                )
            }

            // Auto Crop On/Off Toggle Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (autoCropEnabled) QuackyAccent else QuackySurface.copy(alpha = 0.85f))
                    .border(1.dp, if (autoCropEnabled) QuackyAccent else QuackyOutline, RoundedCornerShape(20.dp))
                    .clickable { onToggleAutoCrop() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Crop,
                        contentDescription = null,
                        tint = if (autoCropEnabled) QuackyBackground else QuackyTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (autoCropEnabled) "Auto Crop: ON" else "Auto Crop: OFF",
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (autoCropEnabled) QuackyBackground else QuackyTextPrimary
                    )
                }
            }

            if (pagesCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(QuackyAccent)
                        .clickable { onDone() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Next ($pagesCount)",
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = QuackyBackground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Next",
                            tint = QuackyBackground,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
        }

        // Bottom Shutter & Gallery Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Picker
            IconButton(
                onClick = onPickGallery,
                modifier = Modifier
                    .size(52.dp)
                    .background(QuackySurface.copy(alpha = 0.85f), CircleShape)
                    .border(1.dp, QuackyOutline, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = "Gallery",
                    tint = QuackyTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Big Shutter Button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(QuackySurface.copy(alpha = 0.9f))
                    .border(3.dp, QuackyAccent, CircleShape)
                    .clickable {
                        val capture = imageCapture ?: return@clickable
                        capture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val rotation = image.imageInfo.rotationDegrees
                                    val rawBitmap = image.toBitmap()
                                    image.close()
                                    val bitmap = if (rotation != 0) {
                                        DocumentProcessor.rotateBitmap(rawBitmap, rotation)
                                    } else {
                                        rawBitmap
                                    }
                                    val quadToUse = if (autoCropEnabled && liveDetectedQuad != null) {
                                        liveDetectedQuad
                                    } else {
                                        DocumentQuad(
                                            topLeft = CornerPoint(0f, 0f),
                                            topRight = CornerPoint(1f, 0f),
                                            bottomRight = CornerPoint(1f, 1f),
                                            bottomLeft = CornerPoint(0f, 1f)
                                        )
                                    }
                                    onPhotoCaptured(bitmap, quadToUse)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(QuackyAccent)
                )
            }

            if (pagesCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(QuackySurface.copy(alpha = 0.85f))
                        .border(1.dp, QuackyOutline, RoundedCornerShape(14.dp))
                        .clickable { onDone() }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Review",
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = QuackyAccent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Review",
                            tint = QuackyAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(52.dp))
            }
        }
    }
}

@Composable
private fun QuadCropView(
    bitmap: Bitmap,
    quad: app.quacky.feature.documentscanner.domain.DocumentQuad,
    isProcessing: Boolean,
    onCornerDragged: (String, CornerPoint) -> Unit,
    onResetFull: () -> Unit,
    onResetInset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    var activeDraggingCorner by remember { mutableStateOf<String?>(null) }
    val currentQuad by rememberUpdatedState(quad)
    val currentOnCornerDragged by rememberUpdatedState(onCornerDragged)

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = QuackyTextPrimary)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Full",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    color = QuackyTextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(QuackySurface)
                        .border(1.dp, QuackyOutline, RoundedCornerShape(8.dp))
                        .clickable { onResetFull() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                Text(
                    text = "Auto Quad",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    color = QuackyAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(QuackySurface)
                        .border(1.dp, QuackyAccent, RoundedCornerShape(8.dp))
                        .clickable { onResetInset() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            IconButton(onClick = onApply, enabled = !isProcessing) {
                Icon(Icons.Rounded.Check, contentDescription = "Apply", tint = QuackyAccent)
            }
        }

        // Quad Interactive Canvas
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val canvasW = constraints.maxWidth.toFloat()
            val canvasH = constraints.maxHeight.toFloat()

            // Calculate scaled bitmap display rect
            val scale = minOf(canvasW / bitmap.width.toFloat(), canvasH / bitmap.height.toFloat())
            val dispW = bitmap.width * scale
            val dispH = bitmap.height * scale
            val offsetX = (canvasW - dispW) / 2f
            val offsetY = (canvasH - dispH) / 2f

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Document to crop",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(dispW, dispH, offsetX, offsetY) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val q = currentQuad
                                val ptTL = Offset(offsetX + q.topLeft.x * dispW, offsetY + q.topLeft.y * dispH)
                                val ptTR = Offset(offsetX + q.topRight.x * dispW, offsetY + q.topRight.y * dispH)
                                val ptBR = Offset(offsetX + q.bottomRight.x * dispW, offsetY + q.bottomRight.y * dispH)
                                val ptBL = Offset(offsetX + q.bottomLeft.x * dispW, offsetY + q.bottomLeft.y * dispH)

                                val dTL = hypot(startOffset.x - ptTL.x, startOffset.y - ptTL.y)
                                val dTR = hypot(startOffset.x - ptTR.x, startOffset.y - ptTR.y)
                                val dBR = hypot(startOffset.x - ptBR.x, startOffset.y - ptBR.y)
                                val dBL = hypot(startOffset.x - ptBL.x, startOffset.y - ptBL.y)

                                val minDist = minOf(dTL, dTR, dBR, dBL)
                                activeDraggingCorner = if (minDist < 180f) {
                                    when (minDist) {
                                        dTL -> "TL"
                                        dTR -> "TR"
                                        dBR -> "BR"
                                        else -> "BL"
                                    }
                                } else null
                            },
                            onDragEnd = { activeDraggingCorner = null },
                            onDragCancel = { activeDraggingCorner = null },
                            onDrag = { change, _ ->
                                change.consume()
                                val corner = activeDraggingCorner ?: return@detectDragGestures
                                val newNormX = ((change.position.x - offsetX) / dispW).coerceIn(0f, 1f)
                                val newNormY = ((change.position.y - offsetY) / dispH).coerceIn(0f, 1f)
                                currentOnCornerDragged(corner, CornerPoint(newNormX, newNormY))
                            }
                        )
                    }
            ) {
                val q = quad
                val pTL = Offset(offsetX + q.topLeft.x * dispW, offsetY + q.topLeft.y * dispH)
                val pTR = Offset(offsetX + q.topRight.x * dispW, offsetY + q.topRight.y * dispH)
                val pBR = Offset(offsetX + q.bottomRight.x * dispW, offsetY + q.bottomRight.y * dispH)
                val pBL = Offset(offsetX + q.bottomLeft.x * dispW, offsetY + q.bottomLeft.y * dispH)

                // Connecting quad boundary path
                val quadPath = Path().apply {
                    moveTo(pTL.x, pTL.y)
                    lineTo(pTR.x, pTR.y)
                    lineTo(pBR.x, pBR.y)
                    lineTo(pBL.x, pBL.y)
                    close()
                }

                drawPath(path = quadPath, color = Color.White, style = Stroke(width = 2.5.dp.toPx()))

                // Draw corner handles
                listOf(pTL, pTR, pBR, pBL).forEach { pt ->
                    drawCircle(color = Color.White, radius = 9.dp.toPx(), center = pt)
                    drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = 22.dp.toPx(), center = pt, style = Stroke(width = 2.5.dp.toPx()))
                }
            }
        }

        // Bottom instruction bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(QuackySurface)
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Drag the 4 corner circles to fit the document edges",
                fontFamily = SatoshiFontFamily,
                fontSize = 13.sp,
                color = QuackyTextSecondary
            )
        }
    }
}

@Composable
private fun ReviewPagesView(
    state: DocScannerUiState,
    onSelectPage: (Int) -> Unit,
    onAddPage: () -> Unit,
    onSetFilter: (DocFilterType) -> Unit,
    onRotate: () -> Unit,
    onRecrop: () -> Unit,
    onDelete: () -> Unit,
    onExportPdf: () -> Unit
) {
    val curPage = state.currentPage

    Column(modifier = Modifier.fillMaxSize()) {
        // Main Page Image Preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val bmp = curPage?.warpedBitmap ?: curPage?.originalBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Scanned Page",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Page Controls & Editing Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(QuackySurface)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Horizontal Page Strip Carousel
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add page button
                item {
                    Box(
                        modifier = Modifier
                            .size(54.dp, 72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(QuackySurfaceElevated)
                            .border(1.dp, QuackyOutline, RoundedCornerShape(8.dp))
                            .clickable { onAddPage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add page", tint = QuackyTextPrimary, modifier = Modifier.size(20.dp))
                            Text("Add", fontFamily = SatoshiFontFamily, fontSize = 11.sp, color = QuackyTextSecondary)
                        }
                    }
                }

                // Pages items
                itemsIndexed(state.pages) { idx, page ->
                    val isSelected = (idx == state.activePageIndex)
                    val pBmp = page.warpedBitmap ?: page.originalBitmap
                    Box(
                        modifier = Modifier
                            .size(54.dp, 72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, if (isSelected) QuackyAccent else QuackyOutline, RoundedCornerShape(8.dp))
                            .clickable { onSelectPage(idx) }
                    ) {
                        Image(
                            bitmap = pBmp.asImageBitmap(),
                            contentDescription = "Page ${idx + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P${idx + 1}",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Selector Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(DocFilterType.entries) { _, filter ->
                    val isSelected = (curPage?.filterType == filter)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) QuackyAccent else QuackySurfaceElevated)
                            .border(1.dp, if (isSelected) QuackyAccent else QuackyOutline, RoundedCornerShape(8.dp))
                            .clickable { onSetFilter(filter) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter.label,
                            fontFamily = SatoshiFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (isSelected) QuackyBackground else QuackyTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Rotate, Re-crop, Delete, Export PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onRotate, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = "Rotate", tint = QuackyTextPrimary)
                    }
                    IconButton(onClick = onRecrop, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.Crop, contentDescription = "Crop", tint = QuackyTextPrimary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = QuackyTextTertiary)
                    }
                }

                QuackyButton(
                    onClick = onExportPdf,
                    style = QuackyButtonStyle.Primary
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create PDF")
                    }
                }
            }
        }
    }
}
