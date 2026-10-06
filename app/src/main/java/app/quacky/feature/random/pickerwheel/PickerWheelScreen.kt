package app.quacky.feature.random.pickerwheel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.core.components.ToolScaffold
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.component.SectionLabel
import app.quacky.core.designsystem.theme.CardCornerRadius
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackySurfaceElevated
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerWheelScreen(
    viewModel: PickerWheelViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    var newOptionText by remember { mutableStateOf("") }

    val animatedAngle by animateFloatAsState(
        targetValue = state.currentAngle,
        animationSpec = tween(durationMillis = 1800),
        label = "wheel_spin"
    )

    ToolScaffold(
        tool = ToolRegistry.PICKER_WHEEL,
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
            // Wheel Canvas Card
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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(220.dp)
                        ) {
                            // Monochromatic Canvas Wheel
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val radius = size.minDimension / 2 - 10f
                                val center = Offset(size.width / 2, size.height / 2)
                                val sweep = 360f / state.options.size

                                val segmentColors = listOf(
                                    Color(0xFF1E1E1E),
                                    Color(0xFF282828),
                                    Color(0xFF323232),
                                    Color(0xFF3C3C3C)
                                )

                                for (i in state.options.indices) {
                                    val start = animatedAngle + i * sweep
                                    val color = segmentColors[i % segmentColors.size]
                                    drawArc(
                                        color = color,
                                        startAngle = start,
                                        sweepAngle = sweep,
                                        useCenter = true,
                                        topLeft = Offset(center.x - radius, center.y - radius),
                                        size = Size(radius * 2, radius * 2)
                                    )
                                    drawArc(
                                        color = QuackyOutline,
                                        startAngle = start,
                                        sweepAngle = sweep,
                                        useCenter = true,
                                        topLeft = Offset(center.x - radius, center.y - radius),
                                        size = Size(radius * 2, radius * 2),
                                        style = Stroke(1.5f)
                                    )
                                }

                                // Outer border
                                drawCircle(QuackyAccent, radius = radius, center = center, style = Stroke(2.5f))
                                // Center pin
                                drawCircle(QuackyAccent, radius = 12f, center = center)
                            }

                            // Pointer Arrow on top
                            Icon(
                                imageVector = Icons.Rounded.PieChart,
                                contentDescription = null,
                                tint = QuackyAccent,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopCenter)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Winner readout (if available)
                        if (state.winner != null) {
                            Text(
                                text = "Winner: ${state.winner?.label}",
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = QuackyAccent
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            QuackyButton(
                                onClick = viewModel::eliminateWinnerAndSpinAgain,
                                style = QuackyButtonStyle.Secondary
                            ) {
                                Text(text = "Eliminate & Spin Again", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Spin Action Button
            item {
                QuackyButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.spin()
                    },
                    style = QuackyButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (state.isSpinning) "Spinning..." else "Spin Wheel", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Options Management
            item {
                SectionLabel(text = "Options (${state.options.size})")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newOptionText,
                        onValueChange = { newOptionText = it },
                        placeholder = { Text(text = "Add new option", color = QuackyTextTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QuackyAccent,
                            unfocusedBorderColor = QuackyOutline,
                            focusedTextColor = QuackyTextPrimary,
                            unfocusedTextColor = QuackyTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.addOption(newOptionText)
                            newOptionText = ""
                        }
                    ) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add", tint = QuackyAccent)
                    }
                }
            }

            // Options List
            items(state.options, key = { it.id }) { option ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = QuackySurface,
                    border = BorderStroke(1.dp, QuackyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            fontFamily = SatoshiFontFamily,
                            fontSize = 14.sp,
                            color = QuackyTextPrimary
                        )
                        IconButton(
                            onClick = { viewModel.removeOption(option.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = null, tint = QuackyTextTertiary, modifier = Modifier.size(16.dp))
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
