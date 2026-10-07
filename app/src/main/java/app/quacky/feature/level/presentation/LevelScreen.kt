package app.quacky.feature.level.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.components.ToolScaffold
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.component.QuackyCard
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackySurfaceElevated
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.haptics.rememberQuackyHaptics
import app.quacky.core.registry.ToolRegistry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LevelScreen(
    viewModel: LevelViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val haptics = rememberQuackyHaptics()

    LaunchedEffect(Unit) {
        viewModel.onLevelReached.collect {
            haptics.tick()
        }
    }

    DisposableEffect(Unit) {
        viewModel.startListening()
        onDispose {
            viewModel.stopListening()
        }
    }

    ToolScaffold(
        tool = ToolRegistry.SPIRIT_LEVEL,
        onBack = onBack,
        isPinned = isPinned,
        onTogglePin = onTogglePin,
        onHelpClick = onOpenHowToUse,
        onResetClick = {
            haptics.click()
            viewModel.resetZero()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(QuackyBackground)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode Header Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(QuackySurfaceElevated, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (state.isLevel) Color(0xFF00E676) else Color(0xFFFFB300), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isFlatMode) stringResource(R.string.level_surface_mode) else stringResource(R.string.level_edge_mode),
                    fontFamily = SatoshiFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = QuackyTextPrimary
                )
                if (state.isHeld) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• HELD",
                        fontFamily = SatoshiFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            // Main Level Visualization Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isFlatMode) {
                    BullseyeLevelView(
                        roll = state.roll,
                        pitch = state.pitch,
                        isLevel = state.isLevel
                    )
                } else {
                    TubularLevelView(
                        roll = state.roll,
                        pitch = state.pitch,
                        isLevel = state.isLevel
                    )
                }
            }

            // Digital Angle Readout Card
            QuackyCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.isLevel) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0x3300E676), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.level_perfect_level),
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF00E676)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AngleMetric(
                            label = stringResource(R.string.level_pitch),
                            angle = state.pitch,
                            isLevel = kotlin.math.abs(state.pitch) <= 0.4f
                        )
                        AngleMetric(
                            label = stringResource(R.string.level_roll),
                            angle = state.roll,
                            isLevel = kotlin.math.abs(state.roll) <= 0.4f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row (Hold / Zero Calibrate)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuackyButton(
                    onClick = {
                        haptics.click()
                        viewModel.toggleHold()
                    },
                    style = if (state.isHeld) QuackyButtonStyle.Primary else QuackyButtonStyle.Secondary,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (state.isHeld) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isHeld) stringResource(R.string.level_release_button) else stringResource(R.string.level_hold_button),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                QuackyButton(
                    onClick = {
                        haptics.click()
                        viewModel.calibrateZero()
                    },
                    style = QuackyButtonStyle.Secondary,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.level_zero_calibrate),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (state.zeroOffsetPitch != 0f || state.zeroOffsetRoll != 0f) {
                    QuackyButton(
                        onClick = {
                            haptics.click()
                            viewModel.resetZero()
                        },
                        style = QuackyButtonStyle.Secondary,
                        modifier = Modifier.width(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset Zero",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BullseyeLevelView(
    roll: Float,
    pitch: Float,
    isLevel: Boolean
) {
    val levelColor by animateColorAsState(
        targetValue = if (isLevel) Color(0xFF00E676) else Color(0xFFFFD54F),
        animationSpec = tween(durationMillis = 200),
        label = "levelColor"
    )

    Canvas(
        modifier = Modifier
            .size(280.dp)
            .aspectRatio(1f)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = size.width / 2f - 14f

        // 1. Dark outer bezel ring with degree ticks
        drawCircle(
            color = QuackySurface,
            radius = maxRadius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = QuackyOutline,
            radius = maxRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )

        // Degree tick marks (every 30°)
        for (i in 0 until 12) {
            val angleRad = (i * 30) * (PI / 180f)
            val outerX = cx + cos(angleRad).toFloat() * maxRadius
            val outerY = cy + sin(angleRad).toFloat() * maxRadius
            val innerX = cx + cos(angleRad).toFloat() * (maxRadius - 10f)
            val innerY = cy + sin(angleRad).toFloat() * (maxRadius - 10f)
            drawLine(
                color = QuackyTextTertiary,
                start = Offset(innerX, innerY),
                end = Offset(outerX, outerY),
                strokeWidth = 2f
            )
        }

        // 2. Concentric ring markers at 5°, 3°, 1°
        val r5 = maxRadius * 0.75f
        val r3 = maxRadius * 0.50f
        val r1 = maxRadius * 0.25f

        listOf(r5, r3, r1).forEach { r ->
            drawCircle(
                color = QuackyOutline.copy(alpha = 0.6f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )
        }

        // Center Level Target Bullseye Ring
        drawCircle(
            color = if (isLevel) Color(0xFF00E676) else QuackyTextSecondary,
            radius = r1 * 0.70f,
            center = Offset(cx, cy),
            style = Stroke(width = if (isLevel) 3f else 1.5f)
        )

        // Crosshairs
        drawLine(
            color = QuackyOutline,
            start = Offset(cx - maxRadius + 14f, cy),
            end = Offset(cx + maxRadius - 14f, cy),
            strokeWidth = 1.5f
        )
        drawLine(
            color = QuackyOutline,
            start = Offset(cx, cy - maxRadius + 14f),
            end = Offset(cx, cy + maxRadius - 14f),
            strokeWidth = 1.5f
        )

        // 3. Fluid Spirit Bubble (Moves with roll on X and pitch on Y)
        val maxAngle = 10f
        val bubbleOffsetX = (roll / maxAngle).coerceIn(-1f, 1f) * (maxRadius * 0.72f)
        val bubbleOffsetY = (-pitch / maxAngle).coerceIn(-1f, 1f) * (maxRadius * 0.72f)

        val bubbleCenter = Offset(cx + bubbleOffsetX, cy + bubbleOffsetY)
        val bubbleRadius = maxRadius * 0.16f

        // Bubble glow halo
        drawCircle(
            color = levelColor.copy(alpha = 0.35f),
            radius = bubbleRadius * 1.5f,
            center = bubbleCenter
        )

        // Bubble fill
        drawCircle(
            color = levelColor,
            radius = bubbleRadius,
            center = bubbleCenter
        )

        // Bubble inner highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = bubbleRadius * 0.35f,
            center = Offset(bubbleCenter.x - bubbleRadius * 0.25f, bubbleCenter.y - bubbleRadius * 0.25f)
        )
    }
}

@Composable
private fun TubularLevelView(
    roll: Float,
    pitch: Float,
    isLevel: Boolean
) {
    val levelColor by animateColorAsState(
        targetValue = if (isLevel) Color(0xFF00E676) else Color(0xFFFFD54F),
        animationSpec = tween(durationMillis = 200),
        label = "tubularLevelColor"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val tubeW = size.width * 0.85f
        val tubeH = 54f

        // 1. Tubular glass vial background
        drawRoundRect(
            color = QuackySurface,
            topLeft = Offset(cx - tubeW / 2f, cy - tubeH / 2f),
            size = Size(tubeW, tubeH),
            cornerRadius = CornerRadius(27f, 27f)
        )
        drawRoundRect(
            color = QuackyOutline,
            topLeft = Offset(cx - tubeW / 2f, cy - tubeH / 2f),
            size = Size(tubeW, tubeH),
            cornerRadius = CornerRadius(27f, 27f),
            style = Stroke(width = 2.5f)
        )

        // 2. Center leveling target lines
        val targetSpacing = 28f
        drawLine(
            color = if (isLevel) Color(0xFF00E676) else QuackyTextPrimary,
            start = Offset(cx - targetSpacing, cy - tubeH / 2f + 4f),
            end = Offset(cx - targetSpacing, cy + tubeH / 2f - 4f),
            strokeWidth = 2.5f
        )
        drawLine(
            color = if (isLevel) Color(0xFF00E676) else QuackyTextPrimary,
            start = Offset(cx + targetSpacing, cy - tubeH / 2f + 4f),
            end = Offset(cx + targetSpacing, cy + tubeH / 2f - 4f),
            strokeWidth = 2.5f
        )

        // 3. Fluid Bubble
        val activeAngle = if (kotlin.math.abs(roll) > kotlin.math.abs(pitch)) roll else pitch
        val maxAngle = 12f
        val bubbleOffset = (activeAngle / maxAngle).coerceIn(-1f, 1f) * (tubeW / 2f - 40f)

        val bubbleW = 46f
        val bubbleCenter = Offset(cx + bubbleOffset, cy)

        // Bubble glow
        drawRoundRect(
            color = levelColor.copy(alpha = 0.35f),
            topLeft = Offset(bubbleCenter.x - bubbleW / 2f - 4f, cy - tubeH * 0.35f - 4f),
            size = Size(bubbleW + 8f, tubeH * 0.70f + 8f),
            cornerRadius = CornerRadius(16f, 16f)
        )
        // Bubble body
        drawRoundRect(
            color = levelColor,
            topLeft = Offset(bubbleCenter.x - bubbleW / 2f, cy - tubeH * 0.35f),
            size = Size(bubbleW, tubeH * 0.70f),
            cornerRadius = CornerRadius(14f, 14f)
        )
    }
}

@Composable
private fun AngleMetric(
    label: String,
    angle: Float,
    isLevel: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = QuackyTextTertiary,
            fontFamily = SatoshiFontFamily
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = (if (angle > 0) "+$angle°" else "$angle°"),
            fontFamily = SatoshiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = if (isLevel) Color(0xFF00E676) else QuackyTextPrimary
        )
    }
}
