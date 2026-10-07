package app.quacky.feature.random.randomnumber

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.core.components.ToolScaffold
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.component.QuackyChip
import app.quacky.core.designsystem.component.SectionLabel
import app.quacky.core.designsystem.theme.CardCornerRadius
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.haptics.rememberQuackyHaptics
import app.quacky.core.registry.ToolRegistry
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomNumberScreen(
    viewModel: RandomNumberViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptics = rememberQuackyHaptics()

    var minInput by remember { mutableStateOf("${state.min}") }
    var maxInput by remember { mutableStateOf("${state.max}") }
    var countInput by remember { mutableStateOf("${state.count}") }
    var showResultsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.min, state.max, state.count) {
        minInput = "${state.min}"
        maxInput = "${state.max}"
        countInput = "${state.count}"
    }

    // Modal dialog showing all results
    if (showResultsDialog) {
        AlertDialog(
            onDismissRequest = { showResultsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Results (${state.generatedNumbers.size})",
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = QuackyTextPrimary,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = { showResultsDialog = false }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = QuackyTextSecondary
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    if (state.generatedNumbers.isNotEmpty()) {
                        val sum = state.generatedNumbers.sum()
                        val avg = state.generatedNumbers.average()
                        val min = state.generatedNumbers.minOrNull() ?: 0L
                        val max = state.generatedNumbers.maxOrNull() ?: 0L
                        Text(
                            text = "Sum: $sum  ·  Avg: ${String.format(Locale.ROOT, "%.1f", avg)}  ·  Min: $min  ·  Max: $max",
                            fontFamily = SatoshiFontFamily,
                            fontSize = 12.sp,
                            color = QuackyTextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(state.generatedNumbers) { index, num ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(QuackyBackground, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    fontFamily = SatoshiFontFamily,
                                    color = QuackyTextTertiary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "$num",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = QuackyTextPrimary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                QuackyButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Quacky Numbers", state.generatedNumbers.joinToString(", ")))
                        Toast.makeText(context, "Copied all numbers", Toast.LENGTH_SHORT).show()
                    },
                    style = QuackyButtonStyle.Primary
                ) {
                    Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy All")
                }
            },
            dismissButton = {
                QuackyButton(
                    onClick = { showResultsDialog = false },
                    style = QuackyButtonStyle.Secondary
                ) {
                    Text("Done")
                }
            },
            containerColor = QuackySurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    ToolScaffold(
        tool = ToolRegistry.RANDOM_NUMBER,
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
            // Results Display Card
            item {
                Surface(
                    shape = RoundedCornerShape(CardCornerRadius),
                    color = QuackySurface,
                    border = BorderStroke(1.dp, QuackyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.seed.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = QuackyBackground,
                                border = BorderStroke(1.dp, QuackyOutline),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "Seeded #${state.sequenceIndex}",
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 11.sp,
                                    color = QuackyTextSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            items(state.generatedNumbers) { num ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = QuackyBackground,
                                    border = BorderStroke(1.dp, QuackyOutline),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$num",
                                            fontFamily = SatoshiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = QuackyTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Generate & Show Results Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuackyButton(
                        onClick = {
                            haptics.heavy()
                            viewModel.generate(nextSequence = false)
                        },
                        style = QuackyButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.Numbers, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Generate", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    QuackyButton(
                        onClick = {
                            haptics.click()
                            showResultsDialog = true
                        },
                        style = QuackyButtonStyle.Secondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.FormatListNumbered, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Show Results (${state.generatedNumbers.size})", fontSize = 13.sp)
                    }
                }
            }

            // Custom Range Selection
            item {
                SectionLabel(text = "Range (Min – Max)")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = minInput,
                        onValueChange = {
                            minInput = it
                            it.toLongOrNull()?.let { v -> viewModel.setMin(v) }
                        },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QuackyAccent,
                            unfocusedBorderColor = QuackyOutline,
                            focusedTextColor = QuackyTextPrimary,
                            unfocusedTextColor = QuackyTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = maxInput,
                        onValueChange = {
                            maxInput = it
                            it.toLongOrNull()?.let { v -> viewModel.setMax(v) }
                        },
                        label = { Text("Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QuackyAccent,
                            unfocusedBorderColor = QuackyOutline,
                            focusedTextColor = QuackyTextPrimary,
                            unfocusedTextColor = QuackyTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuackyChip(text = "1 – 100", selected = state.min == 1L && state.max == 100L, onClick = { viewModel.setRange(1, 100) })
                    QuackyChip(text = "1 – 10", selected = state.min == 1L && state.max == 10L, onClick = { viewModel.setRange(1, 10) })
                    QuackyChip(text = "1 – 1000", selected = state.min == 1L && state.max == 1000L, onClick = { viewModel.setRange(1, 1000) })
                    QuackyChip(text = "0 – 1", selected = state.min == 0L && state.max == 1L, onClick = { viewModel.setRange(0, 1) })
                }
            }

            // Custom Number of Tries / Count
            item {
                SectionLabel(text = "Number of Tries / Numbers (${state.count})")
                OutlinedTextField(
                    value = countInput,
                    onValueChange = {
                        countInput = it
                        it.toIntOrNull()?.let { c -> viewModel.setCount(c) }
                    },
                    label = { Text("Count (1 – 1000)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QuackyAccent,
                        unfocusedBorderColor = QuackyOutline,
                        focusedTextColor = QuackyTextPrimary,
                        unfocusedTextColor = QuackyTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 5, 10, 50, 100).forEach { c ->
                        QuackyChip(text = "$c", selected = state.count == c, onClick = { viewModel.setCount(c) })
                    }
                }
            }

            // Options: Allow Duplicates & Sort
            item {
                SectionLabel(text = "Options")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuackyChip(
                        text = if (state.allowDuplicates) "Duplicates: Allowed" else "Duplicates: Off",
                        selected = state.allowDuplicates,
                        onClick = { viewModel.setAllowDuplicates(!state.allowDuplicates) }
                    )
                    QuackyChip(
                        text = if (state.isSortResults) "Sorted" else "Unsorted",
                        selected = state.isSortResults,
                        onClick = { viewModel.setSortResults(!state.isSortResults) }
                    )
                }
            }

            // Seeded input (reproducible mode)
            item {
                SectionLabel(text = "Seed (Optional, for repeatable draw)")
                OutlinedTextField(
                    value = state.seed,
                    onValueChange = viewModel::setSeed,
                    placeholder = { Text(text = "e.g. Raffle-2026", color = QuackyTextTertiary) },
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

            // Actions: Copy all & Next in sequence
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuackyButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Quacky Numbers", state.generatedNumbers.joinToString(", ")))
                            Toast.makeText(context, "Copied numbers", Toast.LENGTH_SHORT).show()
                        },
                        style = QuackyButtonStyle.Secondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Copy All", fontSize = 13.sp)
                    }

                    if (state.seed.isNotBlank()) {
                        QuackyButton(
                            onClick = {
                                haptics.click()
                                viewModel.generate(nextSequence = true)
                            },
                            style = QuackyButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Next in Seq", fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
