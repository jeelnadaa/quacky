package app.quacky.feature.random.dice

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
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
fun DiceScreen(
    viewModel: DiceViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    ToolScaffold(
        tool = ToolRegistry.DICE,
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

            // Roll Action Button
            item {
                QuackyButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.roll()
                    },
                    style = QuackyButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (state.isRolling) "Rolling..." else "Roll Dice", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Number of Dice (1-10)
            item {
                SectionLabel(text = "Number of Dice (${state.numberOfDice})")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..10).toList()) { count ->
                        QuackyChip(
                            text = "$count",
                            selected = state.numberOfDice == count,
                            onClick = { viewModel.setNumberOfDice(count) }
                        )
                    }
                }
            }

            // Sides Selector (d4, d6, d8, d10, d12, d20, d100)
            item {
                SectionLabel(text = "Sides (${state.sides})")
                val commonSides = listOf(4, 6, 8, 10, 12, 20, 100)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(commonSides) { sides ->
                        QuackyChip(
                            text = "d$sides",
                            selected = state.sides == sides,
                            onClick = { viewModel.setSides(sides) }
                        )
                    }
                }
            }

            // Modifier Stepper
            item {
                SectionLabel(text = "Modifier (${if (state.modifier > 0) "+${state.modifier}" else "${state.modifier}"})")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-5, -2, -1, 0, 1, 2, 5).forEach { mod ->
                        QuackyChip(
                            text = if (mod > 0) "+$mod" else "$mod",
                            selected = state.modifier == mod,
                            onClick = { viewModel.setModifier(mod) }
                        )
                    }
                }
            }

            // Recent Rolls History
            if (state.history.isNotEmpty()) {
                item {
                    SectionLabel(text = "Roll History")
                }
                items(state.history.take(6)) { record ->
                    Surface(
                        shape = RoundedCornerShape(CardCornerRadius),
                        color = QuackySurface,
                        border = BorderStroke(1.dp, QuackyOutline),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = record.expression,
                                fontFamily = SatoshiFontFamily,
                                fontSize = 14.sp,
                                color = QuackyTextSecondary
                            )
                            Text(
                                text = "${record.total} (${record.individualValues.joinToString(", ")})",
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = QuackyTextPrimary
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
