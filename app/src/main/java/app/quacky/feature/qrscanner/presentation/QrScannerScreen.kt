package app.quacky.feature.qrscanner.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.camera.CameraPreview
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackySurfaceElevated
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.permission.PermissionGate
import app.quacky.feature.qrscanner.domain.BarcodeParser
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    viewModel: QrScannerViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanGalleryImage(uri)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    PermissionGate(
        permission = android.Manifest.permission.CAMERA,
        title = "Camera needed for scanning",
        rationale = stringResource(R.string.camera_permission_rationale),
        onBack = onBack
    ) {
        val analyzer = remember {
            val scanner = BarcodeScanning.getClient()
            ImageAnalysis.Analyzer { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            val first = barcodes.firstOrNull()
                            if (first != null && !first.rawValue.isNullOrBlank()) {
                                val formatName = first.format.toString()
                                viewModel.onBarcodeDetected(first.rawValue!!, formatName)
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Stream
            CameraPreview(
                torchEnabled = uiState.isTorchOn,
                zoomRatio = uiState.zoomRatio,
                onZoomRatioChanged = { viewModel.setZoomRatio(it) },
                imageAnalyzer = analyzer
            )

            // Scanning Viewfinder Overlay
            ScannerViewfinderOverlay()

            // Header Top Bar
            Surface(
                color = QuackySurface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
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
                        text = stringResource(R.string.tool_qr_scanner_name),
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

            // Bottom Floating Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Torch Toggle (only if device has flash)
                if (uiState.hasFlash) {
                    IconButton(
                        onClick = { viewModel.toggleTorch() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(if (uiState.isTorchOn) QuackyAccent else QuackySurfaceElevated, CircleShape)
                            .border(1.dp, QuackyOutline, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (uiState.isTorchOn) Icons.Rounded.FlashOff else Icons.Rounded.FlashOn,
                            contentDescription = "Torch",
                            tint = if (uiState.isTorchOn) Color.Black else QuackyTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Gallery Import
                IconButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(QuackySurfaceElevated, CircleShape)
                        .border(1.dp, QuackyOutline, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = "Scan from gallery",
                        tint = QuackyTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // History
                IconButton(
                    onClick = { viewModel.openHistorySheet() },
                    modifier = Modifier
                        .size(52.dp)
                        .background(QuackySurfaceElevated, CircleShape)
                        .border(1.dp, QuackyOutline, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = "Scan history",
                        tint = QuackyTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )

            // Scanned Result Bottom Sheet
            uiState.activeResult?.let { result ->
                ScanResultSheet(
                    barcode = result,
                    sheetState = resultSheetState,
                    canHandleIntent = { viewModel.canHandleIntent(it) },
                    onDismiss = { viewModel.clearActiveResult() }
                )
            }

            // History Bottom Sheet
            if (uiState.showHistorySheet) {
                ScannerHistorySheet(
                    historyList = uiState.historyList,
                    sheetState = historySheetState,
                    onSelectEntry = { entry ->
                        viewModel.closeHistorySheet()
                        val parsed = BarcodeParser.parse(entry.title, entry.type)
                        viewModel.onBarcodeDetected(entry.title, entry.type)
                    },
                    onDeleteEntry = { viewModel.deleteHistoryEntry(it) },
                    onToggleFavorite = { viewModel.toggleHistoryFavorite(it) },
                    onDismiss = { viewModel.closeHistorySheet() }
                )
            }
        }
    }
}

@Composable
private fun ScannerViewfinderOverlay() {
    val transition = rememberInfiniteTransition(label = "scan_line")
    val sweepProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val reticleSize = minOf(size.width * 0.72f, 280.dp.toPx())
        val half = reticleSize / 2

        val left = cx - half
        val top = cy - half
        val right = cx + half
        val bottom = cy + half

        // Dim outer area
        drawRect(Color.Black.copy(alpha = 0.45f))

        // Cut out clear center rect
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(reticleSize, reticleSize),
            cornerRadius = CornerRadius(16f, 16f),
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
        )

        // Corner Brackets
        val cornerLen = 32.dp.toPx()
        val stroke = 3.5.dp.toPx()

        // Top-Left
        drawLine(QuackyAccent, Offset(left, top), Offset(left + cornerLen, top), stroke)
        drawLine(QuackyAccent, Offset(left, top), Offset(left, top + cornerLen), stroke)

        // Top-Right
        drawLine(QuackyAccent, Offset(right, top), Offset(right - cornerLen, top), stroke)
        drawLine(QuackyAccent, Offset(right, top), Offset(right, top + cornerLen), stroke)

        // Bottom-Left
        drawLine(QuackyAccent, Offset(left, bottom), Offset(left + cornerLen, bottom), stroke)
        drawLine(QuackyAccent, Offset(left, bottom), Offset(left, bottom - cornerLen), stroke)

        // Bottom-Right
        drawLine(QuackyAccent, Offset(right, bottom), Offset(right - cornerLen, bottom), stroke)
        drawLine(QuackyAccent, Offset(right, bottom), Offset(right, bottom - cornerLen), stroke)

        // Animated Sweeping Line
        val lineY = top + (bottom - top) * sweepProgress
        drawLine(
            color = QuackyAccent.copy(alpha = 0.85f),
            start = Offset(left + 8f, lineY),
            end = Offset(right - 8f, lineY),
            strokeWidth = 2.dp.toPx()
        )
    }
}
