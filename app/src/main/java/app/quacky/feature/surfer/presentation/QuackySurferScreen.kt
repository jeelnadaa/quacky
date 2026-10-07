package app.quacky.feature.surfer.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.brand.QuackyMark
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
import app.quacky.feature.surfer.domain.SurferEngine
import app.quacky.feature.surfer.model.CollectibleType
import app.quacky.feature.surfer.model.GameStatus
import kotlin.math.abs

@Composable
fun QuackySurferScreen(
    viewModel: QuackySurferViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val haptics = rememberQuackyHaptics()
    var showOnScreenControls by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SurferEngine.GameEvent.LANE_SWITCH -> haptics.tick()
                SurferEngine.GameEvent.JUMP -> haptics.click()
                SurferEngine.GameEvent.SLIDE -> haptics.click()
                SurferEngine.GameEvent.COIN_PICKUP -> haptics.tick()
                SurferEngine.GameEvent.POWERUP_PICKUP -> haptics.heavy()
                SurferEngine.GameEvent.HOVERBOARD_ACTIVATE -> haptics.heavy()
                SurferEngine.GameEvent.HOVERBOARD_BREAK -> haptics.heavy()
                SurferEngine.GameEvent.SHIELD_BREAK -> haptics.heavy()
                SurferEngine.GameEvent.OBSTACLE_SMASHED -> haptics.heavy()
                SurferEngine.GameEvent.CRASH -> haptics.heavy()
            }
        }
    }

    ToolScaffold(
        tool = ToolRegistry.QUACKY_SURFER,
        onBack = onBack,
        isPinned = isPinned,
        onTogglePin = onTogglePin,
        onHelpClick = onOpenHowToUse,
        onResetClick = {
            haptics.click()
            viewModel.restart()
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(QuackyBackground)
        ) {
            // Main 3D Canvas Game Viewport
            var dragDistanceX by remember { mutableFloatStateOf(0f) }
            var dragDistanceY by remember { mutableFloatStateOf(0f) }
            var hasSwipedInGesture by remember { mutableStateOf(false) }
            val swipeThreshold = 50f

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.status) {
                        if (state.status == GameStatus.PLAYING) {
                            detectTapGestures(
                                onDoubleTap = {
                                    haptics.heavy()
                                    viewModel.activateHoverboard()
                                }
                            )
                        }
                    }
                    .pointerInput(state.status) {
                        if (state.status == GameStatus.PLAYING) {
                            detectDragGestures(
                                onDragStart = {
                                    dragDistanceX = 0f
                                    dragDistanceY = 0f
                                    hasSwipedInGesture = false
                                },
                                onDragEnd = {
                                    hasSwipedInGesture = false
                                    dragDistanceX = 0f
                                    dragDistanceY = 0f
                                },
                                onDragCancel = {
                                    hasSwipedInGesture = false
                                    dragDistanceX = 0f
                                    dragDistanceY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    if (hasSwipedInGesture) return@detectDragGestures

                                    dragDistanceX += dragAmount.x
                                    dragDistanceY += dragAmount.y

                                    val absX = abs(dragDistanceX)
                                    val absY = abs(dragDistanceY)

                                    if (absX > swipeThreshold && absX > absY * 1.2f) {
                                        hasSwipedInGesture = true
                                        change.consume()
                                        if (dragDistanceX > 0) viewModel.switchRight() else viewModel.switchLeft()
                                    } else if (absY > swipeThreshold && absY > absX * 1.2f) {
                                        hasSwipedInGesture = true
                                        change.consume()
                                        if (dragDistanceY < 0) viewModel.jump() else viewModel.slide()
                                    }
                                }
                            )
                        }
                    }
            ) {
                SurferCanvasRenderer.drawScene(this, state)
            }

            // HUD overlay during play
            if (state.status == GameStatus.PLAYING || state.status == GameStatus.PAUSED) {
                GameHudOverlay(
                    score = state.score,
                    coins = state.coins,
                    distanceMeters = state.distanceMeters.toInt(),
                    scoreMultiplier = state.scoreMultiplier,
                    activePowerUp = state.activePowerUp,
                    powerUpFraction = state.powerUpFraction,
                    isHoverboardActive = state.isHoverboardActive,
                    hoverboardFraction = state.hoverboardFraction,
                    isPaused = state.status == GameStatus.PAUSED,
                    showControls = showOnScreenControls,
                    onToggleControls = { showOnScreenControls = !showOnScreenControls },
                    onPauseClick = {
                        haptics.click()
                        viewModel.pause()
                    }
                )
            }

            // Quick-Deploy Hoverboard Floating Action Button
            if (state.status == GameStatus.PLAYING) {
                FloatingHoverboardButton(
                    inventory = state.hoverboardsInventory,
                    isActive = state.isHoverboardActive,
                    fraction = state.hoverboardFraction,
                    onClick = {
                        haptics.heavy()
                        viewModel.activateHoverboard()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = if (showOnScreenControls) 118.dp else 24.dp)
                )
            }

            // Optional On-Screen Controls for accessible / one-hand play
            if (showOnScreenControls && state.status == GameStatus.PLAYING) {
                OnScreenTouchControls(
                    onLeft = { viewModel.switchLeft() },
                    onRight = { viewModel.switchRight() },
                    onJump = { viewModel.jump() },
                    onSlide = { viewModel.slide() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                )
            }

            // Start Screen Overlay
            if (state.status == GameStatus.READY) {
                StartGameOverlay(
                    highScore = state.highScore,
                    totalCoins = state.totalCoins,
                    onStartClick = {
                        haptics.click()
                        viewModel.startGame()
                    }
                )
            }

            // Pause Overlay
            if (state.status == GameStatus.PAUSED) {
                PauseOverlay(
                    score = state.score,
                    onResume = {
                        haptics.click()
                        viewModel.resume()
                    },
                    onRestart = {
                        haptics.click()
                        viewModel.restart()
                    }
                )
            }

            // Game Over Sheet
            if (state.status == GameStatus.GAME_OVER) {
                GameOverOverlay(
                    score = state.score,
                    coins = state.coins,
                    distanceMeters = state.distanceMeters.toInt(),
                    isNewHighScore = state.isNewHighScore,
                    highScore = state.highScore,
                    crashReason = state.lastCrashReason,
                    onPlayAgain = {
                        haptics.click()
                        viewModel.startGame()
                    }
                )
            }
        }
    }
}

@Composable
private fun GameHudOverlay(
    score: Int,
    coins: Int,
    distanceMeters: Int,
    scoreMultiplier: Int,
    activePowerUp: CollectibleType?,
    powerUpFraction: Float,
    isHoverboardActive: Boolean,
    hoverboardFraction: Float,
    isPaused: Boolean,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onPauseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score & Multiplier
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$score",
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = QuackyTextPrimary
                )
                if (scoreMultiplier > 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE040FB), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${scoreMultiplier}X",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Stats (Coins & Distance) + Action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Breadcrumbs/Coins
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(QuackySurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFFFD700), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$coins",
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = QuackyTextPrimary
                    )
                }

                // Distance
                Text(
                    text = "${distanceMeters}m",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 13.sp,
                    color = QuackyTextSecondary
                )

                // Controls toggle
                IconButton(
                    onClick = onToggleControls,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = "Toggle on-screen buttons",
                        tint = if (showControls) QuackyAccent else QuackyTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Pause button
                IconButton(
                    onClick = onPauseClick,
                    enabled = !isPaused,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = "Pause",
                        tint = QuackyTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Active Power-up Progress Bar
        if (activePowerUp != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                val powerUpName = when (activePowerUp) {
                    CollectibleType.MAGNET -> "🧲 COIN MAGNET"
                    CollectibleType.DASH_BOOST -> "⚡ QUACK DASH"
                    CollectibleType.SHIELD -> "🛡️ SHIELD ACTIVE"
                    CollectibleType.MULTIPLIER_2X -> "⭐ 2X BOOST"
                    else -> ""
                }
                Text(
                    text = powerUpName,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (activePowerUp) {
                        CollectibleType.MAGNET -> Color(0xFF00E5FF)
                        CollectibleType.DASH_BOOST -> Color(0xFFFF9100)
                        CollectibleType.SHIELD -> Color(0xFF00E676)
                        else -> Color(0xFFE040FB)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { powerUpFraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = when (activePowerUp) {
                        CollectibleType.MAGNET -> Color(0xFF00E5FF)
                        CollectibleType.DASH_BOOST -> Color(0xFFFF9100)
                        CollectibleType.SHIELD -> Color(0xFF00E676)
                        else -> Color(0xFFE040FB)
                    },
                    trackColor = QuackyOutline
                )
            }
        }

        // Active Hoverboard Countdown Bar
        if (isHoverboardActive) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "🛹 HOVERBOARD ACTIVE",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { hoverboardFraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF00E5FF),
                    trackColor = QuackyOutline
                )
            }
        }
    }
}

@Composable
private fun FloatingHoverboardButton(
    inventory: Int,
    isActive: Boolean,
    fraction: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .background(
                if (isActive) Color(0xFF00E5FF) else QuackySurfaceElevated,
                CircleShape
            )
            .border(
                1.5.dp,
                if (isActive) Color.White else QuackyOutline,
                CircleShape
            )
            .clickable(enabled = inventory > 0 || isActive) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🛹", fontSize = 18.sp)
            Text(
                text = if (isActive) "${(fraction * 20).toInt()}s" else "$inventory",
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (isActive) Color.Black else QuackyTextPrimary
            )
        }
    }
}

@Composable
private fun OnScreenTouchControls(
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onJump: () -> Unit,
    onSlide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left & Right
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconButton(
                onClick = onLeft,
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xCC1A1A1A), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Left",
                    tint = QuackyTextPrimary
                )
            }
            IconButton(
                onClick = onRight,
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xCC1A1A1A), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Right",
                    tint = QuackyTextPrimary
                )
            }
        }

        // Jump & Slide
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconButton(
                onClick = onSlide,
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xCC1A1A1A), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = "Slide",
                    tint = QuackyTextPrimary
                )
            }
            IconButton(
                onClick = onJump,
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xEEFFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = "Jump",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
private fun StartGameOverlay(
    highScore: Int,
    totalCoins: Int,
    onStartClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD90A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            QuackyMark(
                size = 72.dp,
                tint = QuackyTextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.surfer_start_title),
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = QuackyTextPrimary
            )
            Text(
                text = stringResource(R.string.surfer_start_subtitle),
                fontFamily = SatoshiFontFamily,
                fontSize = 13.sp,
                color = QuackyTextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // High Score & Coins Card
            QuackyCard(
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Best Score",
                            fontSize = 11.sp,
                            color = QuackyTextTertiary
                        )
                        Text(
                            text = "$highScore",
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = QuackyTextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Breadcrumbs",
                            fontSize = 11.sp,
                            color = QuackyTextTertiary
                        )
                        Text(
                            text = "$totalCoins",
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick How To Play Controls
            Text(
                text = stringResource(R.string.surfer_controls_tip),
                fontFamily = SatoshiFontFamily,
                fontSize = 12.sp,
                color = QuackyTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            QuackyButton(
                onClick = onStartClick,
                style = QuackyButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.surfer_tap_to_start),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    score: Int,
    onResume: () -> Unit,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD90A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        QuackyCard(
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Game Paused",
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = QuackyTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current Score: $score",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 14.sp,
                    color = QuackyTextSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                QuackyButton(
                    onClick = onResume,
                    style = QuackyButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resume Run", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                QuackyButton(
                    onClick = onRestart,
                    style = QuackyButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restart")
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    coins: Int,
    distanceMeters: Int,
    isNewHighScore: Boolean,
    highScore: Int,
    crashReason: String?,
    onPlayAgain: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        QuackyCard(
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.surfer_game_over),
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = QuackyTextPrimary
                )

                if (crashReason != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = crashReason,
                        fontSize = 12.sp,
                        color = QuackyTextTertiary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isNewHighScore) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0x33FFD700), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.surfer_new_high_score),
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFFFD700)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Stats breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Score", fontSize = 11.sp, color = QuackyTextSecondary)
                        Text(
                            text = "$score",
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = QuackyTextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Breadcrumbs", fontSize = 11.sp, color = QuackyTextSecondary)
                        Text(
                            text = "+$coins",
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFFFFD700)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Distance", fontSize = 11.sp, color = QuackyTextSecondary)
                        Text(
                            text = "${distanceMeters}m",
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = QuackyTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                QuackyButton(
                    onClick = onPlayAgain,
                    style = QuackyButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.surfer_play_again),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
