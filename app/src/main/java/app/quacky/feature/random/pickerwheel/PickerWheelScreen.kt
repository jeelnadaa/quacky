package app.quacky.feature.random.pickerwheel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
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
    val haptics = app.quacky.core.haptics.rememberQuackyHaptics()
    var newOptionText by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<WheelItem?>(null) }
    var editTextValue by remember { mutableStateOf("") }

    if (editingItem != null) {
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = {
                Text(
                    text = "Edit Option",
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = QuackyTextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QuackyAccent,
                        unfocusedBorderColor = QuackyOutline,
                        focusedTextColor = QuackyTextPrimary,
                        unfocusedTextColor = QuackyTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                QuackyButton(
                    onClick = {
                        editingItem?.let { item ->
                            viewModel.editOption(item.id, editTextValue)
                        }
                        editingItem = null
                    },
                    style = QuackyButtonStyle.Primary
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                QuackyButton(
                    onClick = { editingItem = null },
                    style = QuackyButtonStyle.Secondary
                ) {
                    Text("Cancel")
                }
            },
            containerColor = QuackySurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    val animatedAngle by animateFloatAsState(
        targetValue = state.currentAngle,
        animationSpec = tween(durationMillis = 1000),
        label = "wheel_spin"
    )

    ToolScaffold(
        tool = ToolRegistry.PICKER_WHEEL,
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
                            modifier = Modifier.size(230.dp)
                        ) {
                            // Monochromatic Canvas Wheel
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val radius = size.minDimension / 2 - 12f
                                val center = Offset(size.width / 2, size.height / 2)
                                val count = state.options.size.coerceAtLeast(1)
                                val sweep = 360f / count

                                val segmentColors = listOf(
                                    Color(0xFF1E1E1E),
                                    Color(0xFF282828),
                                    Color(0xFF323232),
                                    Color(0xFF3C3C3C)
                                )

                                val textPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = (11f * density).coerceIn(20f, 32f)
                                    isAntiAlias = true
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                }

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

                                    // Draw Option Label inside the wheel slice
                                    val midAngle = start + sweep / 2f
                                    drawContext.canvas.nativeCanvas.save()
                                    drawContext.canvas.nativeCanvas.rotate(midAngle, center.x, center.y)

                                    val label = state.options[i].label
                                    val maxChars = if (count > 8) 7 else 12
                                    val displayLabel = if (label.length > maxChars) label.take(maxChars - 1) + "…" else label

                                    drawContext.canvas.nativeCanvas.drawText(
                                        displayLabel,
                                        center.x + radius - 16f,
                                        center.y + 6f,
                                        textPaint
                                    )
                                    drawContext.canvas.nativeCanvas.restore()
                                }

                                // Outer border
                                drawCircle(QuackyAccent, radius = radius, center = center, style = Stroke(2.5f))
                                // Center pin
                                drawCircle(QuackyAccent, radius = 12f, center = center)
                            }

                            // Downward Pointer Needle on top (replaces white wheel icon)
                            Canvas(
                                modifier = Modifier
                                    .size(20.dp, 22.dp)
                                    .align(Alignment.TopCenter)
                            ) {
                                val needlePath = Path().apply {
                                    moveTo(0f, 0f)
                                    lineTo(size.width, 0f)
                                    lineTo(size.width / 2f, size.height)
                                    close()
                                }
                                drawPath(needlePath, color = QuackyAccent)
                            }
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
                        haptics.heavy()
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
                        modifier = Modifier
                            .clickable {
                                editingItem = option
                                editTextValue = option.label
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            fontFamily = SatoshiFontFamily,
                            fontSize = 14.sp,
                            color = QuackyTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    editingItem = option
                                    editTextValue = option.label
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Edit, contentDescription = "Edit", tint = QuackyTextTertiary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { viewModel.removeOption(option.id) },
                                modifier = Modifier.size(28.dp),
                                enabled = state.options.size > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Delete",
                                    tint = if (state.options.size > 1) QuackyTextTertiary else QuackyOutline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
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
