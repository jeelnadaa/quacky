package app.quacky.feature.random.coinflip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.MonetizationOn
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
import androidx.compose.ui.graphics.graphicsLayer
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
fun CoinFlipScreen(
    viewModel: CoinFlipViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptics = rememberQuackyHaptics()

    var coinCountInput by remember { mutableStateOf("${state.coinCount}") }
    var showResultsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.coinCount) {
        coinCountInput = "${state.coinCount}"
    }

    // Modal dialog showing all results
    if (showResultsDialog) {
        val currentHeads = state.currentResults.count { it }
        val currentTails = state.currentResults.count { !it }
        val total = state.currentResults.size

        AlertDialog(
            onDismissRequest = { showResultsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Flip Results ($total coins)",
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
                    if (total > 0) {
                        val hPct = (currentHeads.toFloat() / total) * 100f
                        val tPct = (currentTails.toFloat() / total) * 100f
                        Text(
                            text = "${state.headsLabel}: $currentHeads (${String.format(Locale.ROOT, "%.1f", hPct)}%)  ·  ${state.tailsLabel}: $currentTails (${String.format(Locale.ROOT, "%.1f", tPct)}%)",
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
                        itemsIndexed(state.currentResults) { index, isHeads ->
                            val label = if (isHeads) state.headsLabel else state.tailsLabel
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(QuackyBackground, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Flip #${index + 1}",
                                    fontFamily = SatoshiFontFamily,
                                    color = QuackyTextTertiary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = label,
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = QuackyTextPrimary,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                QuackyButton(
                    onClick = {
                        val text = state.currentResults.mapIndexed { idx, h ->
                            "Flip #${idx + 1}: ${if (h) state.headsLabel else state.tailsLabel}"
                        }.joinToString("\n")
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Quacky Coin Flips", text))
                        Toast.makeText(context, "Copied coin flip results", Toast.LENGTH_SHORT).show()
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
        tool = ToolRegistry.COIN_FLIP,
        onBack = onBack,
        onHelpClick = onOpenHowToUse,
        onResetClick = viewModel::resetSession
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Coins Display Card
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
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            items(state.currentResults) { isHeads ->
                                val rotation by animateFloatAsState(
                                    targetValue = if (state.isFlipping) 360f else 0f,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "coin_rotation"
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = QuackyBackground,
                                    border = BorderStroke(2.dp, QuackyAccent),
                                    modifier = Modifier
                                        .size(72.dp)
                                        .graphicsLayer { rotationY = rotation }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isHeads) state.headsLabel else state.tailsLabel,
                                            fontFamily = SatoshiFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = QuackyTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Flip Action Button & Show Results
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuackyButton(
                        onClick = {
                            haptics.heavy()
                            viewModel.flip()
                        },
                        style = QuackyButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (state.isFlipping) "Flipping..." else "Flip Coin", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                        Text(text = "Show Results (${state.currentResults.size})", fontSize = 13.sp)
                    }
                }
            }

            // Number of Coins (Custom Tries)
            item {
                SectionLabel(text = "Number of Coins / Flips (${state.coinCount})")
                OutlinedTextField(
                    value = coinCountInput,
                    onValueChange = {
                        coinCountInput = it
                        it.toIntOrNull()?.let { c -> viewModel.setCoinCount(c) }
                    },
                    label = { Text("Coin count (1 – 200)") },
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
                    items(listOf(1, 2, 3, 5, 10, 20, 50, 100)) { count ->
                        QuackyChip(
                            text = "$count",
                            selected = state.coinCount == count,
                            onClick = { viewModel.setCoinCount(count) }
                        )
                    }
                }
            }

            // Label Presets (Heads/Tails vs Yes/No)
            item {
                SectionLabel(text = "Labels")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuackyChip(
                        text = "Heads / Tails",
                        selected = state.headsLabel == "Heads",
                        onClick = { viewModel.setCustomLabels("Heads", "Tails") }
                    )
                    QuackyChip(
                        text = "Yes / No",
                        selected = state.headsLabel == "Yes",
                        onClick = { viewModel.setCustomLabels("Yes", "No") }
                    )
                }
            }

            // Session Stats Card
            item {
                val total = state.headsCount + state.tailsCount
                val hPct = if (total > 0) (state.headsCount.toFloat() / total) * 100f else 50f
                val tPct = if (total > 0) (state.tailsCount.toFloat() / total) * 100f else 50f

                SectionLabel(text = "Session Stats")
                Surface(
                    shape = RoundedCornerShape(CardCornerRadius),
                    color = QuackySurface,
                    border = BorderStroke(1.dp, QuackyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total flips: $total", color = QuackyTextSecondary, fontSize = 13.sp)
                            Text(text = "Reset stats", color = QuackyTextTertiary, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${state.headsLabel}: ${state.headsCount} (${String.format(Locale.ROOT, "%.0f", hPct)}%)", color = QuackyTextPrimary, fontSize = 14.sp)
                            Text(text = "${state.tailsLabel}: ${state.tailsCount} (${String.format(Locale.ROOT, "%.0f", tPct)}%)", color = QuackyTextPrimary, fontSize = 14.sp)
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
