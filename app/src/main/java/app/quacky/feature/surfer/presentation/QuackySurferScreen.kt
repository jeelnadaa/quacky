package app.quacky.feature.surfer.presentation

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.quacky.core.components.ToolScaffold
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyDestructive
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackySurfaceElevated
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.haptics.rememberQuackyHaptics
import app.quacky.core.registry.ToolRegistry
import app.quacky.feature.surfer.domain.SurferAudioEngine

private val BreadcrumbYellow = Color(0xFFFFD700)

@Composable
fun QuackySurferScreen(
    viewModel: QuackySurferViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = rememberQuackyHaptics()
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var gameSurfaceView by remember { mutableStateOf<GameSurfaceView?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Initialize Audio Engine
    val audioEngine = remember { SurferAudioEngine(context) }

    // Initialize 3D Engine Renderer
    val renderer3d = remember {
        Game3dRenderer(
            context = context,
            onBreadcrumbCollected = { count ->
                audioEngine.playCoin()
                viewModel.onBreadcrumbCollected(count)
            },
            onCrash = { reason, dist, coins, score ->
                audioEngine.playCrash()
                audioEngine.stopBgm()
                viewModel.onCrash(reason, dist, coins, score)
            }
        ).apply {
            onTick = { dist, coins, score ->
                viewModel.onStatsUpdate(dist, coins, score)
            }
        }
    }

    // Sync Audio Engine with Sound toggle setting
    LaunchedEffect(state.soundEnabled) {
        audioEngine.isSoundEnabled = state.soundEnabled
    }

    // Audio on Countdown ticks
    LaunchedEffect(state.countdown) {
        if (state.status == SurferUiStatus.COUNTDOWN) {
            audioEngine.playCountdownTick()
        }
    }

    // Audio on Game Status transitions
    LaunchedEffect(state.status) {
        when (state.status) {
            SurferUiStatus.RUNNING -> {
                audioEngine.playCountdownGo()
                audioEngine.startBgm()
            }
            SurferUiStatus.PAUSED -> {
                audioEngine.pauseBgm()
            }
            SurferUiStatus.CRASHED, SurferUiStatus.GAME_OVER -> {
                audioEngine.stopBgm()
            }
            else -> {}
        }
    }

    // Check Filament Engine Support
    LaunchedEffect(renderer3d) {
        if (!renderer3d.isEngineInitialized) {
            viewModel.setEngineSupported(false)
        }
    }

    // Haptics Event Collector
    LaunchedEffect(Unit) {
        viewModel.hapticEvents.collect { event ->
            when (event) {
                SurferGameHapticEvent.Tick -> haptics.tick()
                SurferGameHapticEvent.Click -> haptics.click()
                SurferGameHapticEvent.Heavy -> haptics.heavy()
            }
        }
    }

    // Lifecycle Synchronization
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    gameSurfaceView?.stopRendering()
                    renderer3d.pause()
                    audioEngine.pauseBgm()
                    viewModel.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    gameSurfaceView?.startRendering()
                    if (state.status == SurferUiStatus.RUNNING) {
                        renderer3d.resume()
                        audioEngine.resumeBgm()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            gameSurfaceView?.stopRendering()
            audioEngine.release()
            renderer3d.destroy()
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
            audioEngine.stopBgm()
            viewModel.playAgain {
                renderer3d.startReadyState()
            }
            viewModel.startCountdown {
                renderer3d.startRunning()
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(QuackyBackground)
        ) {
            if (!state.isEngineSupported) {
                // Unsupported Device Card (OpenGL ES 3.0 required)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = QuackySurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "3D Engine Unavailable",
                                style = MaterialTheme.typography.titleLarge,
                                color = QuackyTextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your phone can't run the 3D engine this game needs.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = QuackyTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Real 3D Filament Viewport (Edge-to-Edge)
                AndroidView(
                    factory = { ctx ->
                        GameSurfaceView(
                            context = ctx,
                            renderer3d = renderer3d,
                            onSwipeLeft = {
                                audioEngine.playSwipeLeft()
                                if (state.hapticsEnabled) haptics.tick()
                                renderer3d.requestMoveLeft()
                            },
                            onSwipeRight = {
                                audioEngine.playSwipeRight()
                                if (state.hapticsEnabled) haptics.tick()
                                renderer3d.requestMoveRight()
                            },
                            onSwipeUp = {
                                audioEngine.playJump()
                                if (state.hapticsEnabled) haptics.tick()
                                renderer3d.requestJump()
                            },
                            onSwipeDown = {
                                audioEngine.playSlide()
                                if (state.hapticsEnabled) haptics.tick()
                                renderer3d.requestSlide()
                            }
                        ).also { sv ->
                            gameSurfaceView = sv
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .systemGestureExclusion()
                )

                // Top Vignette Scrim for HUD Legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xCC000000),
                                    Color(0x66000000),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // HUD Bar (Score, Breadcrumbs, Distance, Pause)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Score & Distance
                    Column {
                        Text(
                            text = "${state.score}",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${state.distanceMeters.toInt()} m",
                            style = MaterialTheme.typography.labelMedium,
                            color = QuackyTextSecondary
                        )
                    }

                    // Right: Breadcrumb Pill & Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Breadcrumbs Yellow Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(QuackySurfaceElevated)
                                .border(1.dp, QuackyOutline, RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(BreadcrumbYellow, CircleShape)
                            )
                            Text(
                                text = "${state.breadcrumbs}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BreadcrumbYellow
                            )
                        }

                        // Pause Button (touch consuming)
                        if (state.status == SurferUiStatus.RUNNING) {
                            IconButton(
                                onClick = {
                                    haptics.click()
                                    renderer3d.pause()
                                    viewModel.pause()
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(QuackySurfaceElevated)
                                    .border(1.dp, QuackyOutline, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Pause,
                                    contentDescription = "Pause",
                                    tint = QuackyTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Overflow Menu Button
                        Box {
                            IconButton(
                                onClick = {
                                    haptics.click()
                                    showOverflowMenu = true
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(QuackySurfaceElevated)
                                    .border(1.dp, QuackyOutline, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Options",
                                    tint = QuackyTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                modifier = Modifier.background(QuackySurfaceElevated)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sound: ${if (state.soundEnabled) "ON" else "OFF"}", color = QuackyTextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            if (state.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeMute,
                                            contentDescription = null,
                                            tint = QuackyTextSecondary
                                        )
                                    },
                                    onClick = {
                                        viewModel.toggleSound()
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Haptics: ${if (state.hapticsEnabled) "ON" else "OFF"}", color = QuackyTextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.Vibration,
                                            contentDescription = null,
                                            tint = QuackyTextSecondary
                                        )
                                    },
                                    onClick = {
                                        viewModel.toggleHaptics()
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Swipe hints: ${if (state.showSwipeHints) "ON" else "OFF"}", color = QuackyTextPrimary) },
                                    onClick = {
                                        viewModel.toggleSwipeHints()
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reset best score", color = QuackyDestructive) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showResetDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // State: READY Screen
                // -------------------------------------------------------------
                if (state.status == SurferUiStatus.READY) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptics.click()
                                viewModel.startCountdown {
                                    renderer3d.startRunning()
                                }
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (state.showSwipeHints) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("←  ↑  ↓  →", color = QuackyTextTertiary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Swipe to move, jump & slide",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = QuackyTextSecondary
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            Button(
                                onClick = {
                                    haptics.click()
                                    viewModel.startCountdown {
                                        renderer3d.startRunning()
                                    }
                                },
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Text(
                                        text = "TAP TO RUN",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // State: COUNTDOWN (3 - 2 - 1)
                // -------------------------------------------------------------
                if (state.status == SurferUiStatus.COUNTDOWN) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val scale = remember { Animatable(1.5f) }
                        LaunchedEffect(state.countdown) {
                            scale.snapTo(1.6f)
                            scale.animateTo(
                                targetValue = 1.0f,
                                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                            )
                        }

                        Text(
                            text = "${state.countdown}",
                            fontSize = 110.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.scale(scale.value)
                        )
                    }
                }

                // -------------------------------------------------------------
                // State: PAUSED Overlay
                // -------------------------------------------------------------
                if (state.status == SurferUiStatus.PAUSED) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xCC000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = QuackySurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "PAUSED",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        haptics.click()
                                        renderer3d.resume()
                                        audioEngine.resumeBgm()
                                        viewModel.resume()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Text("Resume", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = {
                                        haptics.click()
                                        audioEngine.stopBgm()
                                        viewModel.playAgain {
                                            renderer3d.startReadyState()
                                        }
                                        viewModel.startCountdown {
                                            renderer3d.startRunning()
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Text("Restart", color = QuackyTextPrimary)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TextButton(
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Exit", color = QuackyTextSecondary)
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // State: GAME OVER ("CRASHED!" Card)
                // -------------------------------------------------------------
                AnimatedVisibility(
                    visible = state.status == SurferUiStatus.GAME_OVER,
                    enter = fadeIn(tween(300)) + scaleIn(tween(300, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(200)) + scaleOut(tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xCC000000))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = QuackySurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Title & Reason
                                Text(
                                    text = "CRASHED!",
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.crashReason.ifEmpty { "You crashed!" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = QuackyTextSecondary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Stats Grid: Score, Breadcrumbs, Distance, Best
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(QuackySurface)
                                        .border(1.dp, QuackyOutline, RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Row 1: Score & Breadcrumbs
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("SCORE", style = MaterialTheme.typography.labelSmall, color = QuackyTextTertiary)
                                            Text("${state.score}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("BREADCRUMBS", style = MaterialTheme.typography.labelSmall, color = QuackyTextTertiary)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(BreadcrumbYellow, CircleShape)
                                                )
                                                Text(
                                                    "+${state.breadcrumbs}",
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BreadcrumbYellow
                                                )
                                            }
                                        }
                                    }

                                    // Row 2: Distance & Best
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("DISTANCE", style = MaterialTheme.typography.labelSmall, color = QuackyTextTertiary)
                                            Text("${state.distanceMeters.toInt()} m", style = MaterialTheme.typography.titleMedium, color = QuackyTextPrimary)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text("BEST", style = MaterialTheme.typography.labelSmall, color = QuackyTextTertiary)
                                                if (state.isNewHighScore) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF2E7D32)
                                                    ) {
                                                        Text(
                                                            text = "NEW BEST",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text("${state.highScore}", style = MaterialTheme.typography.titleMedium, color = QuackyTextPrimary)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Play Again White Button
                                Button(
                                    onClick = {
                                        haptics.click()
                                        audioEngine.stopBgm()
                                        viewModel.playAgain {
                                            renderer3d.startReadyState()
                                        }
                                        viewModel.startCountdown {
                                            renderer3d.startRunning()
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                ) {
                                    Text("Play Again", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Exit Text Button
                                TextButton(
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Exit", color = QuackyTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reset High Score Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Best Score?", color = Color.White) },
            text = { Text("Are you sure you want to reset your high score to 0?", color = QuackyTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetHighScore()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = QuackyDestructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = QuackyTextPrimary)
                }
            },
            containerColor = QuackySurfaceElevated
        )
    }
}
