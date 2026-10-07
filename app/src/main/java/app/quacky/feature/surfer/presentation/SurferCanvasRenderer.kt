package app.quacky.feature.surfer.presentation

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.feature.surfer.model.Collectible
import app.quacky.feature.surfer.model.CollectibleType
import app.quacky.feature.surfer.model.Obstacle
import app.quacky.feature.surfer.model.ObstacleType
import app.quacky.feature.surfer.model.SurferGameState
import app.quacky.feature.surfer.model.SurferParticle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object SurferCanvasRenderer {

    fun drawScene(
        scope: DrawScope,
        state: SurferGameState
    ) {
        val width = scope.size.width
        val height = scope.size.height

        val horizonY = height * 0.20f
        val groundBottomY = height * 0.94f

        val trackWidthHorizon = width * 0.28f
        val trackWidthBottom = width * 0.90f

        // 1. Atmospheric Tunnel Ceiling & Sky
        drawTunnelBackground(scope, width, height, horizonY)

        // 2. Track Base, Rails & Moving Perspective Ties
        drawTrack(scope, width, horizonY, groundBottomY, trackWidthHorizon, trackWidthBottom, state.trackScrollOffset)

        // 3. Collectibles & Obstacles sorted Back-to-Front (Large Z to Small Z)
        val renderables = mutableListOf<RenderItem>()
        state.obstacles.forEach { renderables.add(RenderItem.Obs(it)) }
        state.collectibles.forEach { renderables.add(RenderItem.Col(it)) }
        renderables.sortByDescending { it.z }

        for (item in renderables) {
            val z = item.z
            if (z < -0.15f || z > 1.4f) continue

            val depth = (1.0f - z).coerceIn(0.0f, 1.4f)
            // Quadratic perspective projection curve: foreshortens naturally toward horizon
            val y = horizonY + (depth * depth.coerceAtLeast(0.01f).toDouble().let { kotlin.math.sqrt(it).toFloat() }) * (groundBottomY - horizonY)
            val currentTrackW = trackWidthHorizon + depth * (trackWidthBottom - trackWidthHorizon)
            val laneWidth = currentTrackW / 3f
            val itemScale = (0.25f + 0.75f * depth).coerceIn(0.20f, 1.35f)

            when (item) {
                is RenderItem.Obs -> {
                    val x = (width / 2f) + (item.obstacle.lane.xOffsetFactor * laneWidth)
                    draw3DObstacle(scope, item.obstacle, x, y, laneWidth, itemScale, horizonY)
                }
                is RenderItem.Col -> {
                    val x = (width / 2f) + (item.collectible.lane.xOffsetFactor * laneWidth)
                    val elevationOffset = if (item.collectible.isElevated) laneWidth * 0.55f else 0f
                    draw3DCollectible(scope, item.collectible, x, y - elevationOffset, laneWidth, itemScale, state.runCycleProgress)
                }
            }
        }

        // 4. Heroic 3D Animated Duck Character at foreground Z = 0
        drawHeroDuck(scope, state, width, horizonY, groundBottomY, trackWidthHorizon, trackWidthBottom)

        // 5. Dynamic Sparks, Feathers & Dust Particles
        drawParticles(scope, state.particles, width, groundBottomY, trackWidthBottom)
    }

    private sealed class RenderItem(val z: Float) {
        class Obs(val obstacle: Obstacle) : RenderItem(obstacle.z)
        class Col(val collectible: Collectible) : RenderItem(collectible.z)
    }

    private fun drawTunnelBackground(
        scope: DrawScope,
        width: Float,
        height: Float,
        horizonY: Float
    ) {
        // Dark horizon gradient
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    QuackyBackground,
                    Color(0xFF0D0D11),
                    Color(0xFF14141A)
                ),
                startY = 0f,
                endY = horizonY
            ),
            size = Size(width, horizonY)
        )

        // Tunnel ceiling arch beams
        for (i in 1..3) {
            val archH = horizonY * (0.3f * i)
            val archPath = Path().apply {
                moveTo(0f, horizonY * 0.95f)
                quadraticTo(width / 2f, archH * 0.5f, width, horizonY * 0.95f)
            }
            scope.drawPath(
                path = archPath,
                color = QuackyOutline.copy(alpha = 0.5f),
                style = Stroke(width = 2.5f)
            )
        }

        // Overhead distant railway signal lamps
        scope.drawCircle(
            color = Color(0xFFFF3D00),
            radius = 3.5f,
            center = Offset(width / 2f - 30f, horizonY * 0.60f)
        )
        scope.drawCircle(
            color = Color(0xFF00E676),
            radius = 3.5f,
            center = Offset(width / 2f + 30f, horizonY * 0.60f)
        )
    }

    private fun drawTrack(
        scope: DrawScope,
        width: Float,
        horizonY: Float,
        groundBottomY: Float,
        trackWidthHorizon: Float,
        trackWidthBottom: Float,
        trackScrollOffset: Float
    ) {
        val centerX = width / 2f
        val hHalf = trackWidthHorizon / 2f
        val bHalf = trackWidthBottom / 2f

        // Roadbed polygon
        val roadbed = Path().apply {
            moveTo(centerX - hHalf, horizonY)
            lineTo(centerX + hHalf, horizonY)
            lineTo(centerX + bHalf, groundBottomY)
            lineTo(centerX - bHalf, groundBottomY)
            close()
        }
        scope.drawPath(
            path = roadbed,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF121215), Color(0xFF1A1A20), Color(0xFF16161C)),
                startY = horizonY,
                endY = groundBottomY
            )
        )

        // Moving Railroad Ties (Sleepers) with non-linear perspective spacing
        val numTies = 15
        for (i in 0 until numTies) {
            val progress = ((i + trackScrollOffset) / numTies.toFloat()) % 1f
            val depth = progress * progress
            val tieY = horizonY + depth * (groundBottomY - horizonY)
            val currentTrackW = trackWidthHorizon + depth * (trackWidthBottom - trackWidthHorizon)
            val tieThickness = (2.0f + 6.0f * depth).coerceAtLeast(2.0f)

            // Wooden/concrete sleeper bar
            scope.drawLine(
                color = Color(0xFF282830),
                start = Offset(centerX - currentTrackW / 2f - 4f, tieY),
                end = Offset(centerX + currentTrackW / 2f + 4f, tieY),
                strokeWidth = tieThickness,
                cap = StrokeCap.Round
            )
        }

        // 4 Steel Rails dividing the 3 lanes
        for (i in 0..3) {
            val hRailX = (centerX - hHalf) + (trackWidthHorizon / 3f) * i
            val bRailX = (centerX - bHalf) + (trackWidthBottom / 3f) * i

            // Steel rail bottom shadow
            scope.drawLine(
                color = Color(0xFF0F0F12),
                start = Offset(hRailX + 2f, horizonY),
                end = Offset(bRailX + 2f, groundBottomY),
                strokeWidth = if (i == 0 || i == 3) 4.5f else 3.5f
            )

            // Main glowing metallic rail
            scope.drawLine(
                color = if (i == 0 || i == 3) Color(0xFF5A5A66) else Color(0xFF484854),
                start = Offset(hRailX, horizonY),
                end = Offset(bRailX, groundBottomY),
                strokeWidth = if (i == 0 || i == 3) 3.5f else 2.5f
            )
        }
    }

    private fun draw3DObstacle(
        scope: DrawScope,
        obstacle: Obstacle,
        cx: Float,
        cy: Float,
        laneWidth: Float,
        scaleFactor: Float,
        horizonY: Float
    ) {
        when (obstacle.type) {
            ObstacleType.TALL_TRAIN -> {
                // VOLUMETRIC 3D SUBWAY TRAIN
                val trainW = laneWidth * 0.90f
                val trainH = trainW * 1.55f
                val depth3D = trainW * 0.55f // 3D extrusion toward horizon
                val frontTopY = cy - trainH
                val backTopY = frontTopY - depth3D * 0.35f

                // 1. Train Under-chassis Ground Shadow
                scope.drawOval(
                    color = Color(0x88000000),
                    topLeft = Offset(cx - trainW / 2f - 6f, cy - 10f * scaleFactor),
                    size = Size(trainW + 12f, 20f * scaleFactor)
                )

                // 2. 3D Roof Top Quad (slanted back towards horizon)
                val roofPath = Path().apply {
                    moveTo(cx - trainW / 2f, frontTopY)
                    lineTo(cx + trainW / 2f, frontTopY)
                    lineTo(cx + trainW * 0.42f, backTopY)
                    lineTo(cx - trainW * 0.42f, backTopY)
                    close()
                }
                scope.drawPath(
                    path = roofPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E242B), Color(0xFF2C343D)),
                        startY = backTopY,
                        endY = frontTopY
                    )
                )

                // 3D Roof AC Unit
                val acW = trainW * 0.55f
                val acH = depth3D * 0.22f
                scope.drawRoundRect(
                    color = Color(0xFF181C22),
                    topLeft = Offset(cx - acW / 2f, backTopY + 4f),
                    size = Size(acW, acH),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // 3. 3D Perspective Side Wall (visible based on lane position)
                val isLeftLane = obstacle.lane.index == 0
                val isRightLane = obstacle.lane.index == 2

                if (isLeftLane) {
                    // Right side wall visible
                    val sidePath = Path().apply {
                        moveTo(cx + trainW / 2f, frontTopY)
                        lineTo(cx + trainW * 0.42f, backTopY)
                        lineTo(cx + trainW * 0.42f, cy - 8f)
                        lineTo(cx + trainW / 2f, cy)
                        close()
                    }
                    scope.drawPath(
                        path = sidePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF252C33), Color(0xFF191E24)),
                            startX = cx + trainW * 0.42f,
                            endX = cx + trainW / 2f
                        )
                    )
                } else if (isRightLane) {
                    // Left side wall visible
                    val sidePath = Path().apply {
                        moveTo(cx - trainW / 2f, frontTopY)
                        lineTo(cx - trainW * 0.42f, backTopY)
                        lineTo(cx - trainW * 0.42f, cy - 8f)
                        lineTo(cx - trainW / 2f, cy)
                        close()
                    }
                    scope.drawPath(
                        path = sidePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF191E24), Color(0xFF252C33)),
                            startX = cx - trainW / 2f,
                            endX = cx - trainW * 0.42f
                        )
                    )
                }

                // 4. Main Front Cab Face
                scope.drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF38434F), Color(0xFF2A323B), Color(0xFF1D2329)),
                        startY = frontTopY,
                        endY = cy
                    ),
                    topLeft = Offset(cx - trainW / 2f, frontTopY),
                    size = Size(trainW, trainH),
                    cornerRadius = CornerRadius(8f * scaleFactor, 8f * scaleFactor)
                )
                // Front cab rim stroke
                scope.drawRoundRect(
                    color = Color(0xFF4F5D6C),
                    topLeft = Offset(cx - trainW / 2f, frontTopY),
                    size = Size(trainW, trainH),
                    cornerRadius = CornerRadius(8f * scaleFactor, 8f * scaleFactor),
                    style = Stroke(width = 2f * scaleFactor)
                )

                // 5. Large Windshield
                val winW = trainW * 0.78f
                val winH = trainH * 0.32f
                val winTopY = frontTopY + trainH * 0.12f
                scope.drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1565C0), Color(0xFF0D47A1)),
                        startY = winTopY,
                        endY = winTopY + winH
                    ),
                    topLeft = Offset(cx - winW / 2f, winTopY),
                    size = Size(winW, winH),
                    cornerRadius = CornerRadius(6f * scaleFactor, 6f * scaleFactor)
                )
                // Windshield wiper divider
                scope.drawLine(
                    color = Color(0xFF0A2454),
                    start = Offset(cx, winTopY),
                    end = Offset(cx, winTopY + winH),
                    strokeWidth = 2f * scaleFactor
                )

                // 6. Yellow/Black Hazard Band across front
                val bandY = frontTopY + trainH * 0.52f
                val bandH = trainH * 0.11f
                scope.drawRect(
                    color = Color(0xFFFFD600),
                    topLeft = Offset(cx - trainW / 2f, bandY),
                    size = Size(trainW, bandH)
                )

                // 7. Glowing Headlights (Large Dual Beams)
                val lightRadius = 9f * scaleFactor
                val leftLightCenter = Offset(cx - trainW * 0.30f, frontTopY + trainH * 0.74f)
                val rightLightCenter = Offset(cx + trainW * 0.30f, frontTopY + trainH * 0.74f)

                // Outer warm halo
                scope.drawCircle(color = Color(0x55FFF9C4), radius = lightRadius * 1.8f, center = leftLightCenter)
                scope.drawCircle(color = Color(0x55FFF9C4), radius = lightRadius * 1.8f, center = rightLightCenter)
                // Chrome ring
                scope.drawCircle(color = Color(0xFF9E9E9E), radius = lightRadius + 1f, center = leftLightCenter)
                scope.drawCircle(color = Color(0xFF9E9E9E), radius = lightRadius + 1f, center = rightLightCenter)
                // White hot core
                scope.drawCircle(color = Color(0xFFFFFFEE), radius = lightRadius, center = leftLightCenter)
                scope.drawCircle(color = Color(0xFFFFFFEE), radius = lightRadius, center = rightLightCenter)

                // 8. Bottom Heavy Steel Cowcatcher / Bumper
                val bumperY = cy - trainH * 0.12f
                scope.drawRoundRect(
                    color = Color(0xFF15181C),
                    topLeft = Offset(cx - trainW * 0.44f, bumperY),
                    size = Size(trainW * 0.88f, trainH * 0.10f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
            ObstacleType.LOW_BARRIER -> {
                // VOLUMETRIC 3D CONSTRUCTION HURDLE BARRICADE
                val barW = laneWidth * 0.88f
                val barH = laneWidth * 0.48f
                val topY = cy - barH

                // 1. A-Frame Legs with Depth
                val legW = 8f * scaleFactor
                // Left leg
                scope.drawLine(Color(0xFF424242), Offset(cx - barW * 0.45f, cy), Offset(cx - barW * 0.38f, topY - 6f), legW)
                scope.drawLine(Color(0xFF303030), Offset(cx - barW * 0.35f, cy), Offset(cx - barW * 0.38f, topY - 6f), legW * 0.8f)
                // Right leg
                scope.drawLine(Color(0xFF424242), Offset(cx + barW * 0.45f, cy), Offset(cx + barW * 0.38f, topY - 6f), legW)
                scope.drawLine(Color(0xFF303030), Offset(cx + barW * 0.35f, cy), Offset(cx + barW * 0.38f, topY - 6f), legW * 0.8f)

                // 2. Dual Horizontal Cross-Planks
                val plankH = barH * 0.34f
                val plank1Y = topY
                val plank2Y = topY + plankH * 1.15f

                // Top plank 3D edge (depth thickness)
                scope.drawRect(
                    color = Color(0xFFCC8F00),
                    topLeft = Offset(cx - barW / 2f, plank1Y - 4f * scaleFactor),
                    size = Size(barW, 4f * scaleFactor)
                )

                // Planks
                listOf(plank1Y, plank2Y).forEach { py ->
                    scope.drawRoundRect(
                        color = Color(0xFFFFB300),
                        topLeft = Offset(cx - barW / 2f, py),
                        size = Size(barW, plankH),
                        cornerRadius = CornerRadius(3f * scaleFactor, 3f * scaleFactor)
                    )

                    // Black diagonal hazard stripes
                    val numStripes = 5
                    val stripeWidth = barW / numStripes
                    for (s in 0 until numStripes) {
                        val sx = cx - barW / 2f + s * stripeWidth
                        val stripePath = Path().apply {
                            moveTo(sx, py + plankH)
                            lineTo(sx + stripeWidth * 0.45f, py + plankH)
                            lineTo(sx + stripeWidth * 0.85f, py)
                            lineTo(sx + stripeWidth * 0.40f, py)
                            close()
                        }
                        scope.drawPath(path = stripePath, color = Color(0xFF212121))
                    }
                }

                // 3. Pulsing Amber Hazard Warning Beacon on top
                val beaconCenter = Offset(cx, topY - 14f * scaleFactor)
                val beaconRadius = 8f * scaleFactor
                // Glowing outer halo
                scope.drawCircle(color = Color(0x66FFA000), radius = beaconRadius * 2.2f, center = beaconCenter)
                // Amber light
                scope.drawCircle(color = Color(0xFFFFD54F), radius = beaconRadius, center = beaconCenter)
            }
            ObstacleType.HIGH_BARRIER -> {
                // 3D INDUSTRIAL OVERHEAD CLEARANCE GANTRY / STEAM PIPE
                val gantryW = laneWidth * 0.94f
                val towerW = 12f * scaleFactor
                val beamH = 26f * scaleFactor
                val clearanceH = laneWidth * 0.65f // High clearance gap duck slides under
                val beamTopY = cy - clearanceH - beamH

                // 1. Dual Vertical Lattice Support Towers (Left and Right)
                val leftX = cx - gantryW / 2f
                val rightX = cx + gantryW / 2f - towerW

                listOf(leftX, rightX).forEach { tx ->
                    // Steel tower pillar
                    scope.drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF616161), Color(0xFF37474F)),
                            startX = tx,
                            endX = tx + towerW
                        ),
                        topLeft = Offset(tx, beamTopY - 10f),
                        size = Size(towerW, clearanceH + beamH + 10f)
                    )
                }

                // 2. Heavy Horizontal Caution Truss Beam
                scope.drawRoundRect(
                    color = Color(0xFFD32F2F),
                    topLeft = Offset(leftX, beamTopY),
                    size = Size(gantryW, beamH),
                    cornerRadius = CornerRadius(4f * scaleFactor, 4f * scaleFactor)
                )

                // 3. Overhead Clearance "DUCK ⬇" Caution Badge
                val signW = gantryW * 0.58f
                val signH = beamH * 1.35f
                val signY = beamTopY + beamH * 0.2f
                scope.drawRoundRect(
                    color = Color(0xFFFFEB3B),
                    topLeft = Offset(cx - signW / 2f, signY),
                    size = Size(signW, signH),
                    cornerRadius = CornerRadius(4f * scaleFactor, 4f * scaleFactor)
                )
                scope.drawRoundRect(
                    color = Color(0xFF212121),
                    topLeft = Offset(cx - signW / 2f, signY),
                    size = Size(signW, signH),
                    cornerRadius = CornerRadius(4f * scaleFactor, 4f * scaleFactor),
                    style = Stroke(width = 2f * scaleFactor)
                )

                // Down Chevron symbol (indicates slide down!)
                val chevW = 14f * scaleFactor
                val chevPath = Path().apply {
                    moveTo(cx - chevW, signY + signH * 0.35f)
                    lineTo(cx, signY + signH * 0.75f)
                    lineTo(cx + chevW, signY + signH * 0.35f)
                }
                scope.drawPath(path = chevPath, color = Color(0xFF212121), style = Stroke(width = 4f * scaleFactor, cap = StrokeCap.Round))

                // 4. Dual Overhead Floodlights
                scope.drawCircle(color = Color(0xFFFFF9C4), radius = 5f * scaleFactor, center = Offset(cx - gantryW * 0.35f, beamTopY + beamH))
                scope.drawCircle(color = Color(0xFFFFF9C4), radius = 5f * scaleFactor, center = Offset(cx + gantryW * 0.35f, beamTopY + beamH))
            }
        }
    }

    private fun draw3DCollectible(
        scope: DrawScope,
        col: Collectible,
        cx: Float,
        cy: Float,
        laneWidth: Float,
        scaleFactor: Float,
        animProgress: Float
    ) {
        val bob = sin((animProgress * 2 * PI + col.id).toDouble()).toFloat() * 6f * scaleFactor
        val actualY = cy + bob

        when (col.type) {
            CollectibleType.COIN -> {
                // 3D ROTATING GOLD COIN
                val coinBaseR = laneWidth * 0.22f
                val spinPhase = (animProgress * 5f + col.id) % 1f
                val cosSpin = abs(cos(spinPhase * 2 * PI.toFloat())).coerceAtLeast(0.18f)

                // Outer Coin Glow
                scope.drawCircle(color = Color(0x44FFD700), radius = coinBaseR * 1.5f, center = Offset(cx, actualY))

                // 3D Spinning Oval Face
                scope.drawOval(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFFB300), Color(0xFFFFE082), Color(0xFFFFB300)),
                        startX = cx - coinBaseR * cosSpin,
                        endX = cx + coinBaseR * cosSpin
                    ),
                    topLeft = Offset(cx - coinBaseR * cosSpin, actualY - coinBaseR),
                    size = Size(coinBaseR * 2 * cosSpin, coinBaseR * 2)
                )

                // Embossed Inner Rim
                scope.drawOval(
                    color = Color(0xFFFF8F00),
                    topLeft = Offset(cx - (coinBaseR * 0.72f) * cosSpin, actualY - coinBaseR * 0.72f),
                    size = Size((coinBaseR * 1.44f) * cosSpin, coinBaseR * 1.44f),
                    style = Stroke(width = 2.5f * scaleFactor)
                )

                // Center Sparkle
                scope.drawCircle(
                    color = Color(0xFFFFFDE7),
                    radius = (coinBaseR * 0.28f) * cosSpin,
                    center = Offset(cx, actualY)
                )
            }
            CollectibleType.MAGNET -> {
                // Floating Hologram Magnet
                val r = laneWidth * 0.26f
                scope.drawCircle(color = Color(0x3300E5FF), radius = r * 1.5f, center = Offset(cx, actualY))
                scope.drawCircle(color = Color(0xFF00E5FF), radius = r, center = Offset(cx, actualY))
                scope.drawCircle(color = QuackyBackground, radius = r * 0.55f, center = Offset(cx, actualY))
                scope.drawCircle(color = Color(0xFFFFFFFF), radius = r * 0.25f, center = Offset(cx, actualY))
            }
            CollectibleType.DASH_BOOST -> {
                // Floating Hologram Quack Dash Rocket
                val r = laneWidth * 0.26f
                scope.drawCircle(color = Color(0x44FF6D00), radius = r * 1.6f, center = Offset(cx, actualY))
                scope.drawCircle(color = Color(0xFFFF6D00), radius = r, center = Offset(cx, actualY))
                scope.drawCircle(color = Color(0xFFFFF3E0), radius = r * 0.45f, center = Offset(cx, actualY))
            }
            CollectibleType.SHIELD -> {
                // Floating Emerald Shield Orb
                val r = laneWidth * 0.26f
                scope.drawCircle(color = Color(0x4400E676), radius = r * 1.6f, center = Offset(cx, actualY))
                scope.drawCircle(color = Color(0xFF00E676), radius = r, center = Offset(cx, actualY))
                scope.drawCircle(color = Color(0xFFE8F5E9), radius = r * 0.42f, center = Offset(cx, actualY))
            }
            CollectibleType.MULTIPLIER_2X -> {
                // Floating 2X Star Badge
                val r = laneWidth * 0.26f
                scope.drawCircle(color = Color(0x44E040FB), radius = r * 1.6f, center = Offset(cx, actualY))
                scope.drawCircle(color = Color(0xFFE040FB), radius = r, center = Offset(cx, actualY))
                scope.drawCircle(color = Color(0xFFF3E5F5), radius = r * 0.42f, center = Offset(cx, actualY))
            }
        }
    }

    private fun drawHeroDuck(
        scope: DrawScope,
        state: SurferGameState,
        width: Float,
        horizonY: Float,
        groundBottomY: Float,
        trackWidthHorizon: Float,
        trackWidthBottom: Float
    ) {
        val centerX = width / 2f
        val currentTrackW = trackWidthBottom
        val laneWidth = currentTrackW / 3f

        val playerX = centerX + (state.lanePositionFloat * laneWidth)
        val groundY = groundBottomY - 12f

        // Jump Arc (Parabolic trajectory)
        val jumpHeight = if (state.isJumping) {
            sin((state.jumpProgress * PI).toDouble()).toFloat() * (laneWidth * 0.95f)
        } else {
            0f
        }

        // Heroic Mascot Dimensions
        val duckW = laneWidth * 0.65f
        val duckH = duckW * 0.88f
        val duckCenterY = groundY - duckH * 0.55f - jumpHeight

        // 1. Ground Shadow (stays on track when jumping, shrinks with altitude)
        val shadowW = (duckW * 0.95f - jumpHeight * 0.25f).coerceAtLeast(duckW * 0.35f)
        val shadowH = (duckW * 0.28f - jumpHeight * 0.08f).coerceAtLeast(duckW * 0.10f)
        scope.drawOval(
            color = Color(0x88000000),
            topLeft = Offset(playerX - shadowW / 2f, groundY - shadowH / 2f),
            size = Size(shadowW, shadowH)
        )

        // 2. Active Run Cycle & Waddling Animation
        val stepAngle = state.runCycleProgress * 2 * PI.toFloat()
        val runBob = if (!state.isJumping && !state.isSliding) {
            abs(sin(stepAngle)) * 8f
        } else 0f

        // Duck forward lean increases as speed accelerates!
        val forwardTilt = if (!state.isJumping && !state.isSliding) {
            10f + (state.speed / 0.38f) * 12f
        } else if (state.isJumping) {
            -10f // Nose up in the air
        } else {
            35f // Flat on belly during slide
        }

        val animatedDuckY = duckCenterY + runBob

        // 3. Super Quack Dash Sonic Trail
        if (state.activePowerUp == CollectibleType.DASH_BOOST) {
            scope.drawCircle(
                color = Color(0x66FF6D00),
                radius = duckW * 0.90f,
                center = Offset(playerX, animatedDuckY)
            )
            // Exhaust rocket flames
            scope.drawLine(
                color = Color(0xFFFF9100),
                start = Offset(playerX, animatedDuckY + duckH * 0.35f),
                end = Offset(playerX, groundY + 16f),
                strokeWidth = 14f,
                cap = StrokeCap.Round
            )
        }

        // 4. Shield Energy Bubble
        if (state.hasShield) {
            scope.drawCircle(
                color = Color(0x3300E676),
                radius = duckW * 0.82f,
                center = Offset(playerX, animatedDuckY)
            )
            scope.drawCircle(
                color = Color(0xFF00E676),
                radius = duckW * 0.82f,
                center = Offset(playerX, animatedDuckY),
                style = Stroke(width = 3.5f)
            )
        }

        // 5. Draw Running Webbed Feet (Only when on ground or jumping)
        if (!state.isSliding) {
            val footR = duckW * 0.14f
            val legStroke = 4f
            val footY = groundY - 4f

            // Left Leg Cycle
            val leftLegPhase = sin(stepAngle)
            val leftFootX = playerX - duckW * 0.22f + leftLegPhase * (duckW * 0.25f)
            val leftFootY = if (state.isJumping) duckCenterY + duckH * 0.40f else footY - (leftLegPhase.coerceAtLeast(0f) * 14f)

            // Right Leg Cycle (opposite phase)
            val rightLegPhase = sin(stepAngle + PI.toFloat())
            val rightFootX = playerX + duckW * 0.12f + rightLegPhase * (duckW * 0.25f)
            val rightFootY = if (state.isJumping) duckCenterY + duckH * 0.40f else footY - (rightLegPhase.coerceAtLeast(0f) * 14f)

            // Draw Orange Webbed Feet
            listOf(Pair(leftFootX, leftFootY), Pair(rightFootX, rightFootY)).forEach { (fx, fy) ->
                // Leg bone
                scope.drawLine(
                    color = Color(0xFFFF9800),
                    start = Offset(fx, animatedDuckY + duckH * 0.30f),
                    end = Offset(fx, fy),
                    strokeWidth = legStroke,
                    cap = StrokeCap.Round
                )
                // Webbed Foot Paddle
                scope.drawOval(
                    color = Color(0xFFFF9800),
                    topLeft = Offset(fx - footR, fy - footR * 0.5f),
                    size = Size(footR * 2f, footR)
                )
            }
        }

        // 6. Draw Heroic Vector Duck Body with Forward Lean
        val scaleX = if (state.isSliding) 1.35f else 1.0f
        val scaleY = if (state.isSliding) 0.45f else 1.0f

        scope.translate(playerX, animatedDuckY) {
            scope.rotate(degrees = if (state.isSliding) 0f else (sin(stepAngle) * 5f + forwardTilt * 0.35f)) {
                scope.scale(scaleX = scaleX, scaleY = scaleY) {
                    val s = duckW / 512f
                    scope.scale(scale = s, pivot = Offset(256f, 256f)) {
                        scope.translate(-256f, -256f) {
                            // Geometric Quacky vector body
                            val bodyPath = Path().apply {
                                moveTo(120f, 280f)
                                cubicTo(100f, 245f, 140f, 220f, 180f, 235f)
                                cubicTo(215f, 248f, 245f, 220f, 275f, 180f)
                                cubicTo(290f, 160f, 305f, 130f, 340f, 130f)
                                cubicTo(385f, 130f, 415f, 165f, 415f, 205f)
                                cubicTo(415f, 210f, 414f, 216f, 412f, 222f)
                                lineTo(470f, 236f)
                                cubicTo(478f, 238f, 480f, 248f, 474f, 254f)
                                lineTo(412f, 282f)
                                cubicTo(395f, 330f, 345f, 365f, 290f, 370f)
                                cubicTo(210f, 375f, 135f, 340f, 120f, 280f)
                                close()
                            }

                            // Body color: Gold during Dash, Crisp White normally
                            scope.drawPath(
                                path = bodyPath,
                                color = if (state.activePowerUp == CollectibleType.DASH_BOOST) Color(0xFFFFD54F) else QuackyTextPrimary
                            )

                            // Beak Seam
                            scope.drawLine(
                                color = QuackyBackground,
                                start = Offset(412f, 248f),
                                end = Offset(460f, 245f),
                                strokeWidth = 5f,
                                cap = StrokeCap.Round
                            )

                            // Expressive Duck Eye
                            scope.drawOval(
                                color = QuackyBackground,
                                topLeft = Offset(360f - 14f, 190f - 14f),
                                size = Size(28f, 28f)
                            )
                            scope.drawCircle(
                                color = if (state.activePowerUp == CollectibleType.DASH_BOOST) Color(0xFFFFD54F) else QuackyTextPrimary,
                                radius = 5.5f,
                                center = Offset(364f, 186f)
                            )

                            // Wing Flapping Overlay
                            val wingAngle = sin(stepAngle * 2) * 22f
                            scope.rotate(degrees = wingAngle, pivot = Offset(210f, 280f)) {
                                val wingPath = Path().apply {
                                    moveTo(190f, 275f)
                                    cubicTo(160f, 305f, 220f, 335f, 270f, 300f)
                                    cubicTo(290f, 285f, 260f, 260f, 210f, 265f)
                                    close()
                                }
                                scope.drawPath(
                                    path = wingPath,
                                    color = if (state.activePowerUp == CollectibleType.DASH_BOOST) Color(0xFFFFCA28) else Color(0xFFE0E0E0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun drawParticles(
        scope: DrawScope,
        particles: List<SurferParticle>,
        width: Float,
        groundBottomY: Float,
        trackWidthBottom: Float
    ) {
        val centerX = width / 2f
        val laneWidth = trackWidthBottom / 3f
        val groundY = groundBottomY - 14f

        for (p in particles) {
            val px = centerX + (p.x * laneWidth) + (p.vx * 20f)
            val py = groundY - 28f + (p.y * 36f)
            scope.drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(px, py)
            )
        }
    }
}
