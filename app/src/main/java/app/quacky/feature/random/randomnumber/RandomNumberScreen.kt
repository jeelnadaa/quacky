package app.quacky.feature.random.randomnumber

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import app.quacky.core.registry.ToolRegistry

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

    ToolScaffold(
        tool = ToolRegistry.RANDOM_NUMBER,
        onBack = onBack,
        onHelpClick = onOpenHowToUse
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
                                            fontSize = 22.sp,
                                            color = QuackyTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Generate Action Button
            item {
                QuackyButton(
                    onClick = { viewModel.generate(nextSequence = false) },
                    style = QuackyButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.Numbers, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Generate", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Range Presets
            item {
                SectionLabel(text = "Range")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuackyChip(text = "1 – 100", selected = state.min == 1L && state.max == 100L, onClick = { viewModel.setRange(1, 100) })
                    QuackyChip(text = "1 – 10", selected = state.min == 1L && state.max == 10L, onClick = { viewModel.setRange(1, 10) })
                    QuackyChip(text = "1 – 1000", selected = state.min == 1L && state.max == 1000L, onClick = { viewModel.setRange(1, 1000) })
                }
            }

            // How many numbers
            item {
                SectionLabel(text = "How many numbers (${state.count})")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 5, 10).forEach { c ->
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
                            onClick = { viewModel.generate(nextSequence = true) },
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
