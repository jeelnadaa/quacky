package app.quacky.feature.textcounter.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.components.ToolScaffold
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.component.QuackyCard
import app.quacky.core.designsystem.component.QuackyChip
import app.quacky.core.designsystem.component.SectionLabel
import app.quacky.core.designsystem.theme.CardCornerRadius
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyDestructive
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextCounterScreen(
    viewModel: TextCounterViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    ToolScaffold(
        tool = ToolRegistry.TEXT_COUNTER,
        onBack = onBack,
        onHelpClick = onOpenHowToUse,
        onResetClick = viewModel::reset
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Text Input Box
            item {
                Surface(
                    shape = RoundedCornerShape(CardCornerRadius),
                    color = QuackySurface,
                    border = BorderStroke(1.dp, QuackyOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Box(modifier = Modifier.padding(14.dp)) {
                        if (state.text.isEmpty()) {
                            Text(
                                text = "Type or paste your text here...",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 15.sp,
                                color = QuackyTextTertiary
                            )
                        }
                        BasicTextField(
                            value = state.text,
                            onValueChange = viewModel::onTextChanged,
                            textStyle = TextStyle(
                                fontFamily = SatoshiFontFamily,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = QuackyTextPrimary
                            ),
                            cursorBrush = SolidColor(QuackyTextPrimary),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Character Preset Limit
            if (state.selectedPreset != CharacterPreset.NONE) {
                item {
                    val limit = state.selectedPreset.limit
                    val currentCount = state.stats.characterCountWithSpaces
                    val isExceeded = currentCount > limit
                    val progress = (currentCount.toFloat() / limit).coerceIn(0f, 1f)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.selectedPreset.label,
                                fontFamily = SatoshiFontFamily,
                                fontSize = 13.sp,
                                color = QuackyTextSecondary
                            )
                            Text(
                                text = "$currentCount / $limit",
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isExceeded) QuackyDestructive else QuackyTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = if (isExceeded) QuackyDestructive else QuackyAccent,
                            trackColor = QuackyOutline
                        )
                    }
                }
            }

            // Presets Chips Row
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CharacterPreset.entries.filter { it != CharacterPreset.NONE }) { preset ->
                        QuackyChip(
                            text = preset.label,
                            selected = state.selectedPreset == preset,
                            onClick = { viewModel.selectPreset(preset) }
                        )
                    }
                }
            }

            // Live Stats Cards Grid
            item {
                SectionLabel(text = "Statistics")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "Characters (with spaces)",
                            value = "${state.stats.characterCountWithSpaces}",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Characters (no spaces)",
                            value = "${state.stats.characterCountWithoutSpaces}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "Words",
                            value = "${state.stats.wordCount}",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Sentences",
                            value = "${state.stats.sentenceCount}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "Reading Time",
                            value = "${state.stats.readingTimeSeconds}s",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Speaking Time",
                            value = "${state.stats.speakingTimeSeconds}s",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Top Words Frequency
            if (state.wordFrequencies.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel(text = "Word Frequency")
                        Text(
                            text = if (state.ignoreStopwords) "Stopwords hidden" else "All words",
                            fontSize = 12.sp,
                            color = QuackyTextTertiary,
                            modifier = Modifier.clickable { viewModel.toggleIgnoreStopwords() }
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(CardCornerRadius),
                        color = QuackySurface,
                        border = BorderStroke(1.dp, QuackyOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            state.wordFrequencies.take(6).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.word,
                                        fontFamily = SatoshiFontFamily,
                                        fontSize = 14.sp,
                                        color = QuackyTextPrimary
                                    )
                                    Text(
                                        text = "${item.count} (${String.format("%.1f", item.percentage)}%)",
                                        fontFamily = SatoshiFontFamily,
                                        fontSize = 13.sp,
                                        color = QuackyTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Custom Character / Word Frequency & Filters
            item {
                SectionLabel(text = "Custom Frequency & Search")
                Surface(
                    shape = RoundedCornerShape(CardCornerRadius),
                    color = QuackySurface,
                    border = BorderStroke(1.dp, QuackyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("Find character, word or pattern...", color = QuackyTextTertiary, fontSize = 14.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = QuackyTextSecondary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = QuackyTextTertiary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QuackyAccent,
                                unfocusedBorderColor = QuackyOutline,
                                focusedTextColor = QuackyTextPrimary,
                                unfocusedTextColor = QuackyTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Filter Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            QuackyChip(
                                text = if (state.ignoreCase) "Ignore Case" else "Match Case",
                                selected = state.ignoreCase,
                                onClick = viewModel::toggleIgnoreCase
                            )
                            QuackyChip(
                                text = "Whole Words",
                                selected = state.matchWholeWord,
                                onClick = viewModel::toggleMatchWholeWord
                            )
                            QuackyChip(
                                text = "Regex",
                                selected = state.isRegex,
                                onClick = viewModel::toggleRegex
                            )
                        }

                        // Occurrence result display
                        if (state.searchQuery.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(QuackyBackground, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Matches found",
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 13.sp,
                                    color = QuackyTextSecondary
                                )
                                Text(
                                    text = "${state.customMatchCount} (${String.format(java.util.Locale.ROOT, "%.2f", state.customMatchPercentage)}%)",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = QuackyAccent
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions: Clear, Copy, Paste, Save to history
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuackyButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                viewModel.onTextChanged(state.text + pasted)
                            }
                        },
                        style = QuackyButtonStyle.Secondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Paste", fontSize = 13.sp)
                    }

                    QuackyButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Quacky Text", state.text))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        style = QuackyButtonStyle.Secondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Copy", fontSize = 13.sp)
                    }

                    QuackyButton(
                        onClick = {
                            viewModel.saveSnippetToHistory()
                            Toast.makeText(context, "Saved to history", Toast.LENGTH_SHORT).show()
                        },
                        style = QuackyButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Save", fontSize = 13.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CardCornerRadius),
        color = QuackySurface,
        border = BorderStroke(1.dp, QuackyOutline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = value,
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = QuackyTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontFamily = SatoshiFontFamily,
                fontSize = 11.sp,
                color = QuackyTextSecondary,
                maxLines = 1
            )
        }
    }
}
