package app.quacky.feature.compass.presentation

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.components.ToolScaffold
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
import app.quacky.feature.compass.domain.MagneticFieldStatus
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CompassScreen(
    viewModel: CompassViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val haptics = rememberQuackyHaptics()

    LaunchedEffect(Unit) {
        viewModel.onCardinalCrossed.collect {
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
        tool = ToolRegistry.COMPASS,
        onBack = onBack,
        isPinned = isPinned,
        onTogglePin = onTogglePin,
        onHelpClick = onOpenHowToUse
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(QuackyBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Controls Bar: True North toggle & Heading Lock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // North Mode Pill
                Row(
                    modifier = Modifier
                        .background(QuackySurfaceElevated, RoundedCornerShape(20.dp))
                        .border(1.dp, QuackyOutline, RoundedCornerShape(20.dp))
                        .clickable {
                            haptics.click()
                            viewModel.toggleTrueNorth()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (state.isTrueNorth) Color(0xFF29B6F6) else Color(0xFFFF5252),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isTrueNorth) stringResource(R.string.compass_true_north) else stringResource(R.string.compass_magnetic_north),
                        fontFamily = SatoshiFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = QuackyTextPrimary
                    )
                }

                // Lock Heading Button
                Row(
                    modifier = Modifier
                        .background(
                            if (state.lockedHeading != null) Color(0xFF2E2600) else QuackySurfaceElevated,
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (state.lockedHeading != null) Color(0xFFFFB300) else QuackyOutline,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            haptics.click()
                            viewModel.toggleHeadingLock()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (state.lockedHeading != null) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = "Lock Heading",
                        tint = if (state.lockedHeading != null) Color(0xFFFFB300) else QuackyTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.lockedHeading != null) {
                            "LOCK ${state.lockedHeading?.roundToInt()}°"
                        } else {
                            stringResource(R.string.compass_lock_heading)
                        },
                        fontFamily = SatoshiFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.lockedHeading != null) Color(0xFFFFB300) else QuackyTextSecondary
                    )
                }
            }

            // Calibration Banner (if sensor accuracy is degraded)
            AnimatedVisibility(
                visible = state.needsCalibration,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color(0xFF332000), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Calibration Needed",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.compass_figure_8_prompt),
                        fontFamily = SatoshiFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFFFFE082)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Rotating Compass Canvas
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCompassRose(
                        azimuth = state.azimuth,
                        lockedHeading = state.lockedHeading,
                        pitch = state.pitch,
                        roll = state.roll
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Digital Readout
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${state.azimuth.roundToInt()}°",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuackyTextPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = state.cardinal,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Tilt / Course Deviation Subtitle
            if (state.lockedHeading != null && state.courseDeviation != null) {
                val dev = state.courseDeviation!!
                val devText = when {
                    abs(dev) < 1.5f -> "ON COURSE"
                    dev > 0 -> "TURN RIGHT ${abs(dev).roundToInt()}°"
                    else -> "TURN LEFT ${abs(dev).roundToInt()}°"
                }
                Text(
                    text = devText,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (abs(dev) < 1.5f) Color(0xFF00E676) else Color(0xFFFFB300)
                )
            } else {
                Text(
                    text = "Pitch: ${state.pitch}°  ·  Roll: ${state.roll}°  ${if (state.isFlat) "· Level" else ""}",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    color = if (state.isFlat) Color(0xFF81C784) else QuackyTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Magnetic Flux & Metal / EMF Detector Card
            MagneticDetectorCard(state = state)

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MagneticDetectorCard(state: CompassUiState) {
    val statusColor = when (state.fieldStatus) {
        MagneticFieldStatus.SHIELDED -> Color(0xFF9E9E9E)
        MagneticFieldStatus.NORMAL_AMBIENT -> Color(0xFF00E676)
        MagneticFieldStatus.ELEVATED -> Color(0xFFFFB300)
        MagneticFieldStatus.STRONG_EMF -> Color(0xFFFF5252)
    }

    val statusText = when (state.fieldStatus) {
        MagneticFieldStatus.SHIELDED -> stringResource(R.string.compass_field_shielded)
        MagneticFieldStatus.NORMAL_AMBIENT -> stringResource(R.string.compass_field_normal)
        MagneticFieldStatus.ELEVATED -> stringResource(R.string.compass_field_elevated)
        MagneticFieldStatus.STRONG_EMF -> stringResource(R.string.compass_field_strong)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(QuackySurface, RoundedCornerShape(16.dp))
            .border(1.dp, QuackyOutline, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.compass_metal_detector),
                fontFamily = SatoshiFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = QuackyTextSecondary
            )

            // Status Pill
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusText,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Total Magnitude
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${state.magneticMagnitude}",
                fontFamily = SatoshiFontFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = QuackyTextPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "μT (microtesla)",
                fontFamily = SatoshiFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = QuackyTextTertiary,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Real-time Sparkline Waveform Canvas
        if (state.magneticHistory.size > 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(QuackySurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val points = state.magneticHistory
                    val minVal = (points.minOrNull() ?: 20f).coerceAtLeast(10f)
                    val maxVal = (points.maxOrNull() ?: 70f).coerceAtLeast(minVal + 10f)
                    val range = maxVal - minVal

                    val stepX = size.width / (points.size - 1)
                    val path = Path()
                    val fillPath = Path()

                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val normalized = ((value - minVal) / range).coerceIn(0f, 1f)
                        val y = size.height - (normalized * size.height)

                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, size.height)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }

                    fillPath.lineTo(size.width, size.height)
                    fillPath.close()

                    // Draw gradient fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(statusColor.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )

                    // Draw line
                    drawPath(
                        path = path,
                        color = statusColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3-Axis breakdown (Bx, By, Bz)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AxisPill(label = "Bx", value = state.magX)
            AxisPill(label = "By", value = state.magY)
            AxisPill(label = "Bz", value = state.magZ)
        }
    }
}

@Composable
private fun AxisPill(label: String, value: Float) {
    Row(
        modifier = Modifier
            .background(QuackySurfaceElevated, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            fontFamily = SatoshiFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = QuackyTextTertiary
        )
        Text(
            text = "$value μT",
            fontFamily = SatoshiFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = QuackyTextPrimary
        )
    }
}

/**
 * Draws the rotating compass dial, tick marks, cardinal typography, needles, and leveling bubble.
 */
private fun DrawScope.drawCompassRose(
    azimuth: Float,
    lockedHeading: Float?,
    pitch: Float,
    roll: Float
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = min(size.width, size.height) / 2f - 14.dp.toPx()

    // Outer ring
    drawCircle(
        color = Color(0xFF1E1E1E),
        radius = radius,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )

    // Inner subtle ring
    drawCircle(
        color = Color(0xFF141414),
        radius = radius * 0.72f,
        center = center
    )

    // Rotating dial
    rotate(degrees = -azimuth, pivot = center) {
        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 12.sp.toPx()
            color = android.graphics.Color.WHITE
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val northPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 14.sp.toPx()
            color = android.graphics.Color.parseColor("#FF5252")
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val mutedPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 10.sp.toPx()
            color = android.graphics.Color.parseColor("#888888")
        }

        // Ticks and Cardinals every 5 and 30 degrees
        for (deg in 0 until 360 step 5) {
            val rad = Math.toRadians((deg - 90).toDouble())
            val cosVal = cos(rad).toFloat()
            val sinVal = sin(rad).toFloat()

            val isCardinal = deg % 90 == 0
            val isMajor = deg % 30 == 0

            val tickLength = when {
                isCardinal -> 14.dp.toPx()
                isMajor -> 10.dp.toPx()
                else -> 5.dp.toPx()
            }

            val strokeWidth = when {
                isCardinal -> 2.5.dp.toPx()
                isMajor -> 1.5.dp.toPx()
                else -> 1.dp.toPx()
            }

            val tickColor = when {
                deg == 0 -> Color(0xFFFF5252)
                isCardinal -> Color(0xFFE0E0E0)
                isMajor -> Color(0xFF888888)
                else -> Color(0xFF444444)
            }

            val startPoint = Offset(
                center.x + (radius - tickLength) * cosVal,
                center.y + (radius - tickLength) * sinVal
            )
            val endPoint = Offset(
                center.x + radius * cosVal,
                center.y + radius * sinVal
            )

            drawLine(
                color = tickColor,
                start = startPoint,
                end = endPoint,
                strokeWidth = strokeWidth
            )

            // Text labels for cardinal and major angles
            if (isMajor) {
                val labelDist = radius - tickLength - 12.dp.toPx()
                val textX = center.x + labelDist * cosVal
                val textY = center.y + labelDist * sinVal + 4.dp.toPx()

                when (deg) {
                    0 -> drawContext.canvas.nativeCanvas.drawText("N", textX, textY, northPaint)
                    90 -> drawContext.canvas.nativeCanvas.drawText("E", textX, textY, textPaint)
                    180 -> drawContext.canvas.nativeCanvas.drawText("S", textX, textY, textPaint)
                    270 -> drawContext.canvas.nativeCanvas.drawText("W", textX, textY, textPaint)
                    else -> drawContext.canvas.nativeCanvas.drawText("$deg°", textX, textY, mutedPaint)
                }
            }
        }

        // Locked heading marker line on the rotating dial
        if (lockedHeading != null) {
            val lockedRad = Math.toRadians((lockedHeading - 90).toDouble())
            val lockCos = cos(lockedRad).toFloat()
            val lockSin = sin(lockedRad).toFloat()
            drawLine(
                color = Color(0xFFFFB300),
                start = Offset(center.x + (radius * 0.72f) * lockCos, center.y + (radius * 0.72f) * lockSin),
                end = Offset(center.x + radius * lockCos, center.y + radius * lockSin),
                strokeWidth = 3.dp.toPx()
            )
        }

        // North & South Diamond Needles
        val needleLength = radius * 0.62f
        val needleHalfWidth = 7.dp.toPx()

        // Red North Needle
        val northNeedle = Path().apply {
            moveTo(center.x, center.y - needleLength)
            lineTo(center.x + needleHalfWidth, center.y)
            lineTo(center.x - needleHalfWidth, center.y)
            close()
        }
        drawPath(path = northNeedle, color = Color(0xFFFF5252))

        // Silver/Dark South Needle
        val southNeedle = Path().apply {
            moveTo(center.x, center.y + needleLength)
            lineTo(center.x + needleHalfWidth, center.y)
            lineTo(center.x - needleHalfWidth, center.y)
            close()
        }
        drawPath(path = southNeedle, color = Color(0xFF616161))
    }

    // Fixed Top Direction Indicator (12 o'clock chevron)
    val indicatorPath = Path().apply {
        moveTo(center.x, center.y - radius - 10.dp.toPx())
        lineTo(center.x - 6.dp.toPx(), center.y - radius)
        lineTo(center.x + 6.dp.toPx(), center.y - radius)
        close()
    }
    drawPath(path = indicatorPath, color = Color(0xFFFF5252))

    // Center Level crosshairs and bubble (pitch/roll feedback)
    val bubbleMaxOffset = 18.dp.toPx()
    val bubbleOffsetX = (roll.coerceIn(-15f, 15f) / 15f) * bubbleMaxOffset
    val bubbleOffsetY = (-pitch.coerceIn(-15f, 15f) / 15f) * bubbleMaxOffset

    drawCircle(
        color = Color(0xFF2A2A2A),
        radius = 8.dp.toPx(),
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    drawCircle(
        color = if (abs(pitch) < 5f && abs(roll) < 5f) Color(0xFF00E676) else Color(0xFFFFB300),
        radius = 4.dp.toPx(),
        center = Offset(center.x + bubbleOffsetX, center.y + bubbleOffsetY)
    )
}
