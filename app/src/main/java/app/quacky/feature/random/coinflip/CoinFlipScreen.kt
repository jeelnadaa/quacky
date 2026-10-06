package app.quacky.feature.random.coinflip

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinFlipScreen(
    viewModel: CoinFlipViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

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

            // Flip Action Button
            item {
                QuackyButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.flip()
                    },
                    style = QuackyButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.MonetizationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (state.isFlipping) "Flipping..." else "Flip Coin", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Coin Count (1-10)
            item {
                SectionLabel(text = "Number of Coins (${state.coinCount})")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..10).toList()) { count ->
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
                            Text(
                                text = "${state.headsLabel}: ${state.headsCount} (${String.format("%.1f", hPct)}%)",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 14.sp,
                                color = QuackyTextPrimary
                            )
                            Text(
                                text = "${state.tailsLabel}: ${state.tailsCount} (${String.format("%.1f", tPct)}%)",
                                fontFamily = SatoshiFontFamily,
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
