package app.quacky.feature.metadata.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.components.ToolScaffold
import app.quacky.core.registry.ToolRegistry
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackySurfaceElevated
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.feature.metadata.domain.PhotoMetadata
import app.quacky.feature.metadata.domain.PrivacyScore
import coil.compose.AsyncImage

private val DestructiveRed = Color(0xFFB3261E)
private val SafeGreen = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataScreen(
    viewModel: MetadataViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.loadPhotos(uris)
        }
    }

    ToolScaffold(
        tool = ToolRegistry.METADATA,
        onBack = onBack,
        isPinned = isPinned,
        onTogglePin = onTogglePin,
        onHelpClick = onOpenHowToUse
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(QuackyBackground)
        ) {
            if (state.photos.isEmpty() && !state.isLoading) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = null,
                        tint = QuackyTextTertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No photos selected",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = QuackyTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Pick one or more photos to inspect hidden metadata, view GPS locations, and create clean copies.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = QuackyTextSecondary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.metadata_pick_photos), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Content with photos
                val activePhoto = state.activePhoto

                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Multi-photo carousel / header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.photos.size > 1) {
                                "Photo ${state.activeIndex + 1} of ${state.photos.size}"
                            } else {
                                "1 photo selected"
                            },
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = QuackyTextSecondary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = stringResource(R.string.metadata_add_photos),
                                    tint = QuackyTextPrimary
                                )
                            }
                            IconButton(onClick = { viewModel.clearAllPhotos() }) {
                                Icon(
                                    Icons.Rounded.Clear,
                                    contentDescription = stringResource(R.string.metadata_clear),
                                    tint = QuackyTextTertiary
                                )
                            }
                        }
                    }

                    // Multi-photo thumbnail bar (if > 1 photo)
                    if (state.photos.size > 1) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(state.photos) { index, item ->
                                val isSelected = index == state.activeIndex
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(QuackySurface)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color.White else QuackyOutline,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.selectActiveIndex(index) }
                                ) {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = item.fileName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // GPS indicator badge
                                    if (item.location.hasGps) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(DestructiveRed)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active photo details
                    if (activePhoto != null) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                PhotoSummaryCard(photo = activePhoto)
                            }

                            // Location Section
                            item {
                                LocationSection(
                                    location = activePhoto.location,
                                    canOpenMaps = viewModel.canOpenMaps(),
                                    onOpenMaps = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activePhoto.location.mapsUri))
                                            context.startActivity(intent)
                                        } catch (_: ActivityNotFoundException) {
                                            Toast.makeText(context, "No app on this phone can open maps.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }

                            // Device Section
                            item {
                                MetadataGroupCard(
                                    title = stringResource(R.string.metadata_section_device),
                                    icon = Icons.Rounded.CameraAlt,
                                    entries = listOfNotNull(
                                        activePhoto.device.make?.let { "Make" to it },
                                        activePhoto.device.model?.let { "Model" to it },
                                        activePhoto.device.software?.let { "Software" to it },
                                        activePhoto.device.lensModel?.let { "Lens" to it }
                                    )
                                )
                            }

                            // Capture Section
                            item {
                                MetadataGroupCard(
                                    title = stringResource(R.string.metadata_section_capture),
                                    icon = Icons.Rounded.PhotoCamera,
                                    entries = listOfNotNull(
                                        activePhoto.capture.dateTime?.let { "Date / Time" to it },
                                        activePhoto.capture.exposureTime?.let { "Exposure" to it },
                                        activePhoto.capture.fNumber?.let { "Aperture" to it },
                                        activePhoto.capture.iso?.let { "ISO" to it },
                                        activePhoto.capture.focalLength?.let { "Focal Length" to it },
                                        activePhoto.capture.flash?.let { "Flash" to it },
                                        activePhoto.capture.whiteBalance?.let { "White Balance" to it }
                                    )
                                )
                            }

                            // Image Section
                            item {
                                MetadataGroupCard(
                                    title = stringResource(R.string.metadata_section_image),
                                    icon = Icons.Rounded.Image,
                                    entries = listOfNotNull(
                                        if (activePhoto.image.width != null && activePhoto.image.height != null) {
                                            "Dimensions" to "${activePhoto.image.width} × ${activePhoto.image.height}"
                                        } else null,
                                        activePhoto.image.orientation?.let { "Orientation" to it },
                                        activePhoto.image.colorSpace?.let { "Color Space" to it },
                                        "File Size" to activePhoto.image.formattedFileSize,
                                        "Format" to activePhoto.mimeType
                                    )
                                )
                            }

                            // Other Tags Section
                            item {
                                OtherTagsSection(
                                    tags = activePhoto.otherTags,
                                    query = state.tagSearchQuery,
                                    onQueryChange = { viewModel.setTagSearchQuery(it) }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }

                    // Sticky bottom clean bar
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
                            if (state.isCleaning) {
                                Text(
                                    text = state.cleaningProgress,
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
                                    onClick = { viewModel.setShowCleanSheet(true) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (state.photos.size > 1) {
                                            stringResource(R.string.metadata_clean_batch_action, state.photos.size)
                                        } else {
                                            stringResource(R.string.metadata_clean_action)
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Clean Completion Banner
            state.cleanCompletionMessage?.let { msg ->
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
                                text = stringResource(R.string.metadata_cleaned_verified),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = QuackyTextPrimary
                            )
                            Text(
                                text = msg,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = QuackyTextSecondary
                            )
                        }
                        state.lastCleanResults.firstOrNull()?.let { result ->
                            IconButton(onClick = { viewModel.shareCleanedPhoto(context, result) }) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share", tint = QuackyTextPrimary)
                            }
                        }
                        IconButton(onClick = { viewModel.clearCompletionMessage() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = QuackyTextTertiary)
                        }
                    }
                }
            }
        }

        // Clean options bottom sheet
        if (state.showCleanSheet) {
            CleanOptionsSheet(
                totalPhotos = state.photos.size,
                removalChoice = state.removalChoice,
                customOptions = state.customOptions,
                onSelectChoice = { viewModel.setRemovalChoice(it) },
                onUpdateCustom = { viewModel.updateCustomOptions(it) },
                onConfirmClean = { cleanAll -> viewModel.cleanPhotos(cleanAll) },
                onDismiss = { viewModel.setShowCleanSheet(false) }
            )
        }
    }
}

@Composable
private fun PhotoSummaryCard(photo: PhotoMetadata) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = QuackySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = photo.uri,
                contentDescription = photo.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(QuackyBackground)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = photo.fileName,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = QuackyTextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${photo.image.formattedFileSize} • ${photo.mimeType.substringAfter('/')}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = QuackyTextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                // Privacy Score Chip
                PrivacyScoreChip(score = photo.privacyScore, tagCount = photo.totalTagCount)
            }
        }
    }
}

@Composable
private fun PrivacyScoreChip(score: PrivacyScore, tagCount: Int) {
    val (label, bg, textColor, icon) = when (score) {
        PrivacyScore.CLEAN -> Quadruple(
            "Clean (No metadata)",
            QuackyBackground,
            QuackyTextSecondary,
            Icons.Rounded.Lock
        )
        PrivacyScore.CONTAINS_METADATA -> Quadruple(
            "Contains metadata ($tagCount tags)",
            QuackyBackground,
            QuackyTextPrimary,
            Icons.Rounded.Warning
        )
        PrivacyScore.CONTAINS_LOCATION -> Quadruple(
            "Contains GPS location",
            DestructiveRed.copy(alpha = 0.15f),
            DestructiveRed,
            Icons.Rounded.LocationOn
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, if (score == PrivacyScore.CONTAINS_LOCATION) DestructiveRed.copy(alpha = 0.4f) else QuackyOutline, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun LocationSection(
    location: app.quacky.feature.metadata.domain.LocationMetadata,
    canOpenMaps: Boolean,
    onOpenMaps: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = QuackySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (location.hasGps) DestructiveRed.copy(alpha = 0.35f) else QuackyOutline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = if (location.hasGps) DestructiveRed else QuackyTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.metadata_section_location),
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = QuackyTextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                if (location.hasGps) {
                    Text(
                        text = "GPS Active",
                        fontSize = 11.sp,
                        color = DestructiveRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (location.hasGps) {
                // Coordinates
                MetadataRow(label = "Coordinates", value = location.formattedCoordinates)
                location.altitude?.let {
                    MetadataRow(label = "Altitude", value = "%.1f m".format(it))
                }

                // Hardware honesty gated "Open in maps app" button
                if (canOpenMaps) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenMaps,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QuackyBackground,
                            contentColor = QuackyTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .border(1.dp, QuackyOutline, RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.metadata_open_maps), fontSize = 12.sp)
                    }
                }
            } else {
                Text(
                    text = "No GPS coordinates recorded in this photo.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = QuackyTextTertiary
                )
            }
        }
    }
}

@Composable
private fun MetadataGroupCard(
    title: String,
    icon: ImageVector,
    entries: List<Pair<String, String>>
) {
    if (entries.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = QuackySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = QuackyTextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = QuackyTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            entries.forEach { (label, value) ->
                MetadataRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = QuackyTextSecondary,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = QuackyTextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun OtherTagsSection(
    tags: List<app.quacky.feature.metadata.domain.OtherTag>,
    query: String,
    onQueryChange: (String) -> Unit
) {
    if (tags.isEmpty()) return

    val filtered = if (query.isBlank()) tags else tags.filter {
        it.tag.contains(query, ignoreCase = true) || it.value.contains(query, ignoreCase = true)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = QuackySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.metadata_section_other),
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = QuackyTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.metadata_search_tags_hint), fontSize = 12.sp, color = QuackyTextTertiary) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = QuackyTextTertiary, modifier = Modifier.size(18.dp)) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = QuackyTextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = QuackyOutline,
                    focusedTextColor = QuackyTextPrimary,
                    unfocusedTextColor = QuackyTextPrimary,
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                Text(
                    text = "No matching tags",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = QuackyTextTertiary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                filtered.forEach { tag ->
                    MetadataRow(label = tag.tag.removePrefix("Exif"), value = tag.value)
                }
            }
        }
    }
}
