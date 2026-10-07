package app.quacky.feature.compressor.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.automirrored.rounded.Note
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.components.ToolScaffold
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackySurfaceElevated
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.registry.ToolRegistry
import app.quacky.feature.compressor.domain.CompressionMode
import app.quacky.feature.compressor.domain.CompressorPreset
import app.quacky.feature.compressor.domain.OutputFormat

private val SafeGreen = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressorScreen(
    viewModel: CompressorViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.loadFiles(uris)
        }
    }

    ToolScaffold(
        tool = ToolRegistry.COMPRESSOR,
        onBack = onBack,
        isPinned = isPinned,
        onTogglePin = onTogglePin,
        onHelpClick = onOpenHowToUse,
        onResetClick = viewModel::clearFiles
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(QuackyBackground)
        ) {
            if (state.files.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Compress,
                        contentDescription = null,
                        tint = QuackyTextTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No files selected",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = QuackyTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Choose JPG, PNG, WebP images, or a PDF to compress with full privacy.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = QuackyTextSecondary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("image/*", "application/pdf")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.compressor_pick_files), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val activeFile = state.activeFile

                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.files.size > 1) {
                                "File ${state.activeIndex + 1} of ${state.files.size}"
                            } else {
                                "1 file selected"
                            },
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = QuackyTextSecondary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { filePickerLauncher.launch(arrayOf("image/*", "application/pdf")) }) {
                                Icon(Icons.Rounded.UploadFile, contentDescription = "Add Files", tint = QuackyTextPrimary)
                            }
                            IconButton(onClick = { viewModel.removeFile(state.activeIndex) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove File", tint = QuackyTextTertiary)
                            }
                        }
                    }

                    // Multi-file row (if > 1)
                    if (state.files.size > 1) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(state.files) { idx, f ->
                                val isSelected = idx == state.activeIndex
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) QuackySurfaceElevated else QuackySurface)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.White else QuackyOutline,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.selectActiveIndex(idx) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (f.isPdf) Icons.Rounded.PictureAsPdf else Icons.AutoMirrored.Rounded.Note,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else QuackyTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = f.fileName,
                                            color = if (isSelected) Color.White else QuackyTextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove file",
                                            tint = if (isSelected) Color.White else QuackyTextTertiary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.removeFile(idx) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Content form
                    if (activeFile != null) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (!activeFile.isPdf) {
                                // Live Split preview for images
                                item {
                                    val activeResult = state.activeResult
                                    SplitComparisonView(
                                        originalUri = activeFile.uri,
                                        compressedUri = activeResult?.outputUri,
                                        originalSizeText = activeFile.formattedOriginalSize,
                                        compressedSizeText = activeResult?.formattedCompressedSize ?: "Not processed"
                                    )
                                }

                                // Compression mode: Quality slider vs Target size
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = QuackySurface),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                FilterChip(
                                                    selected = state.imageConfig.mode == CompressionMode.QUALITY_SLIDER,
                                                    onClick = { viewModel.setCompressionMode(CompressionMode.QUALITY_SLIDER) },
                                                    label = { Text(stringResource(R.string.compressor_mode_quality), fontSize = 12.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = Color.White,
                                                        selectedLabelColor = Color.Black
                                                    )
                                                )
                                                FilterChip(
                                                    selected = state.imageConfig.mode == CompressionMode.TARGET_SIZE,
                                                    onClick = { viewModel.setCompressionMode(CompressionMode.TARGET_SIZE) },
                                                    label = { Text(stringResource(R.string.compressor_mode_target), fontSize = 12.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = Color.White,
                                                        selectedLabelColor = Color.Black
                                                    )
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            if (state.imageConfig.mode == CompressionMode.QUALITY_SLIDER) {
                                                // Presets
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    CompressorPreset.entries.forEach { p ->
                                                        FilterChip(
                                                            selected = state.imageConfig.preset == p,
                                                            onClick = { viewModel.setPreset(p) },
                                                            label = { Text(p.label, fontSize = 11.sp) },
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                selectedContainerColor = Color.White,
                                                                selectedLabelColor = Color.Black
                                                            )
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                val origSize = activeFile.originalSizeBytes
                                                val estBytes = if (origSize > 0) {
                                                    val factor = (state.imageConfig.quality / 100f).coerceIn(0.05f, 1.0f)
                                                    val est = (origSize * (0.15f + 0.85f * factor)).toLong()
                                                    est.coerceIn(512L, origSize)
                                                } else null

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Quality: ${state.imageConfig.quality}%",
                                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = QuackyTextPrimary
                                                    )
                                                    if (estBytes != null) {
                                                        Text(
                                                            text = "Est. output: ~${app.quacky.feature.compressor.domain.CompressionResult.formatBytes(estBytes)}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                                Slider(
                                                    value = state.imageConfig.quality.toFloat(),
                                                    onValueChange = { viewModel.setQuality(it.toInt()) },
                                                    valueRange = 5f..100f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color.White,
                                                        activeTrackColor = Color.White,
                                                        inactiveTrackColor = QuackyOutline
                                                    )
                                                )
                                            } else {
                                                // Target file size input
                                                Text(
                                                    text = "Target file size (KB):",
                                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = QuackyTextPrimary
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                OutlinedTextField(
                                                    value = state.imageConfig.targetSizeKb.toString(),
                                                    onValueChange = { str ->
                                                        str.toLongOrNull()?.let { viewModel.setTargetSizeKb(it) }
                                                    },
                                                    placeholder = { Text("e.g. 200", color = QuackyTextTertiary) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color.White,
                                                        unfocusedBorderColor = QuackyOutline,
                                                        focusedTextColor = QuackyTextPrimary,
                                                        unfocusedTextColor = QuackyTextPrimary
                                                    ),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Options card
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = QuackySurface),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(stringResource(R.string.compressor_strip_meta), color = QuackyTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                    Text("Removes EXIF, camera info, and GPS coordinates", color = QuackyTextSecondary, fontSize = 11.sp)
                                                }
                                                app.quacky.core.components.QuackySwitch(
                                                    checked = state.imageConfig.stripMetadata,
                                                    onCheckedChange = { viewModel.toggleStripMetadata(it) }
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(stringResource(R.string.compressor_keep_dims), color = QuackyTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                    Text("Prevent dimension downscaling during compression", color = QuackyTextSecondary, fontSize = 11.sp)
                                                }
                                                app.quacky.core.components.QuackySwitch(
                                                    checked = state.imageConfig.keepDimensions,
                                                    onCheckedChange = { viewModel.toggleKeepDimensions(it) }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // PDF Section
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = QuackySurface),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Rounded.PictureAsPdf,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = activeFile.fileName,
                                                color = QuackyTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "Original size: ${activeFile.formattedOriginalSize}",
                                                color = QuackyTextSecondary,
                                                fontSize = 12.sp
                                            )

                                            Spacer(modifier = Modifier.height(14.dp))

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(QuackyBackground)
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.compressor_pdf_text_note),
                                                    color = QuackyTextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Resolution DPI selector
                                            Text(
                                                text = stringResource(R.string.compressor_pdf_dpi),
                                                color = QuackyTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.align(Alignment.Start)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf(72, 100, 150, 200).forEach { dpi ->
                                                    FilterChip(
                                                        selected = state.pdfConfig.dpi == dpi,
                                                        onClick = { viewModel.setPdfDpi(dpi) },
                                                        label = { Text("$dpi DPI", fontSize = 12.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = Color.White,
                                                            selectedLabelColor = Color.Black
                                                        )
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Text(
                                                text = "Page Quality: ${state.pdfConfig.pageQuality}%",
                                                color = QuackyTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.align(Alignment.Start)
                                            )
                                            Slider(
                                                value = state.pdfConfig.pageQuality.toFloat(),
                                                onValueChange = { viewModel.setPdfPageQuality(it.toInt()) },
                                                valueRange = 10f..100f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color.White,
                                                    activeTrackColor = Color.White,
                                                    inactiveTrackColor = QuackyOutline
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }

                    // Sticky Bottom Compress Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = QuackySurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            if (state.isProcessing) {
                                Text(
                                    text = state.progressMessage,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                    color = QuackyTextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color.White,
                                    trackColor = QuackyOutline
                                )
                            } else {
                                Button(
                                    onClick = { viewModel.compressFiles(compressAll = state.files.size > 1) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Compress, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (state.files.size > 1) {
                                            stringResource(R.string.compressor_action_batch, state.files.size)
                                        } else {
                                            stringResource(R.string.compressor_action_compress)
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Completion Banner
            state.completionSummary?.let { summary ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = QuackySurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Saved to Pictures/Quacky",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = QuackyTextPrimary
                            )
                            Text(
                                text = summary,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = QuackyTextSecondary
                            )
                        }
                        state.results.firstOrNull()?.let { result ->
                            IconButton(onClick = { viewModel.shareResult(context, result) }) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share", tint = QuackyTextPrimary)
                            }
                        }
                        IconButton(onClick = { viewModel.dismissSummary() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = QuackyTextTertiary)
                        }
                    }
                }
            }
        }
    }
}
