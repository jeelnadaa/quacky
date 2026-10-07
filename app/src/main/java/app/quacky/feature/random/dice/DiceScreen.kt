package app.quacky.feature.random.dice

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
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FormatListNumbered
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
fun DiceScreen(
    viewModel: DiceViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptics = rememberQuackyHaptics()

    var diceCountInput by remember { mutableStateOf("${state.numberOfDice}") }
    var showResultsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.numberOfDice) {
        diceCountInput = "${state.numberOfDice}"
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
                        text = "Dice Roll Results (${state.currentValues.size})",
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
                    if (state.currentValues.isNotEmpty()) {
                        val sum = state.currentTotal
                        val avg = state.currentValues.average()
                        val min = state.currentValues.minOrNull() ?: 0
                        val max = state.currentValues.maxOrNull() ?: 0
                        Text(
                            text = "Total: $sum  ·  Avg: ${String.format(Locale.ROOT, "%.1f", avg)}  ·  Min: $min  ·  Max: $max",
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
                        itemsIndexed(state.currentValues) { index, num ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(QuackyBackground, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Die #${index + 1}",
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
                        clipboard.setPrimaryClip(ClipData.newPlainText("Quacky Dice Roll", state.currentValues.joinToString(", ")))
                        Toast.makeText(context, "Copied dice results", Toast.LENGTH_SHORT).show()
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
        tool = ToolRegistry.DICE,
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
            // Dice Result Display Card
            item {
                Surface(
                    shape = RoundedCornerShape(CardCornerRadius),
                    color = QuackySurface,
                    border = BorderStroke(1.dp, QuackyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Display Dice Numerals
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            items(state.currentValues) { v ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = QuackyBackground,
                                    border = BorderStroke(1.dp, QuackyOutline),
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$v",
                                            fontFamily = SatoshiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                            color = QuackyTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Total sum display
                        Text(
                            text = "Total: ${state.currentTotal}",
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = QuackyAccent
                        )
                        if (state.modifier != 0) {
                            Text(
                                text = "Modifier: ${if (state.modifier > 0) "+${state.modifier}" else "${state.modifier}"}",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 12.sp,
                                color = QuackyTextSecondary
                            )
                        }
                    }
                }
            }

            // Roll Action Button & Show Results
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuackyButton(
                        onClick = {
                            haptics.heavy()
                            viewModel.roll()
                        },
                        style = QuackyButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (state.isRolling) "Rolling..." else "Roll Dice", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                        Text(text = "Show Results (${state.currentValues.size})", fontSize = 13.sp)
                    }
                }
            }

            // Number of Dice (Custom Tries)
            item {
                SectionLabel(text = "Number of Dice (${state.numberOfDice})")
                OutlinedTextField(
                    value = diceCountInput,
                    onValueChange = {
                        diceCountInput = it
                        it.toIntOrNull()?.let { c -> viewModel.setNumberOfDice(c) }
                    },
                    label = { Text("Dice count (1 – 200)") },
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

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(1, 2, 3, 4, 5, 10, 20, 50)) { count ->
                        QuackyChip(
                            text = "$count",
                            selected = state.numberOfDice == count,
                            onClick = { viewModel.setNumberOfDice(count) }
                        )
                    }
                }
            }

            // Sides of Dice
            item {
                SectionLabel(text = "Dice Type / Sides (${state.sides})")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sidesList = listOf(4, 6, 8, 10, 12, 20, 100)
                    items(sidesList) { s ->
                        QuackyChip(
                            text = "d$s",
                            selected = state.sides == s,
                            onClick = { viewModel.setSides(s) }
                        )
                    }
                }
            }

            // Modifier Selector
            item {
                SectionLabel(text = "Modifier (${if (state.modifier >= 0) "+${state.modifier}" else "${state.modifier}"})")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-5, -2, -1, 0, 1, 2, 5).forEach { mod ->
                        QuackyChip(
                            text = if (mod >= 0) "+$mod" else "$mod",
                            selected = state.modifier == mod,
                            onClick = { viewModel.setModifier(mod) }
                        )
                    }
                }
            }

            // D20 Advantage / Disadvantage Mode (if d20 selected)
            if (state.sides == 20) {
                item {
                    SectionLabel(text = "D20 Mode")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        D20RollMode.entries.forEach { mode ->
                            QuackyChip(
                                text = mode.name.lowercase().replaceFirstChar { it.titlecase() },
                                selected = state.d20Mode == mode,
                                onClick = { viewModel.setD20Mode(mode) }
                            )
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
