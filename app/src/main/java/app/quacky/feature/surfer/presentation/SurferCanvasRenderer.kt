package app.quacky.feature.surfer.presentation

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.feature.surfer.model.Collectible
import app.quacky.feature.surfer.model.CollectibleType
import app.quacky.feature.surfer.model.Obstacle
import app.quacky.feature.surfer.model.ObstacleType
import app.quacky.feature.surfer.model.ScorePopup
import app.quacky.feature.surfer.model.SurferGameState
import app.quacky.feature.surfer.model.SurferParticle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance, realistic 3D Subway Surfers canvas rendering engine.
 * Features:
 * - Urban twilight subway corridor with city skyscraper skyline and catenary overhead power lines
 * - 3 railway tracks with ballast gravel, wooden ties, and 6 gleaming specular steel rails
 * - Hyper-realistic 3D Bombardier commuter trains with glowing cabs, passenger windows, and headlights
 * - Construction hazard roadblocks and overhead gantry clearance barriers
 * - 3D rotating gold coins with specular sheen, and rich 3D power-up models
 * - Quacky the Mascot with athletic sneakers, backwards snapback cap, dynamic banking tilt,
 *   and high-tech cyberpunk hoverboard mode with anti-grav thrusters
 * - Floating score popups, speed lines, and dynamic track sparks
 */
object SurferCanvasRenderer {

    fun drawScene(
        scope: DrawScope,
        state: SurferGameState
    ) {
        val width = scope.size.width
        val height = scope.size.height

        val horizonY = height * 0.22f
        val groundBottomY = height * 0.94f

        val trackWidthHorizon = width * 0.26f
        val trackWidthBottom = width * 0.92f

        // Apply subtle cinematic camera banking on rapid lane changes
        scope.rotate(
            degrees = state.cameraRollDegrees * 0.45f,
            pivot = Offset(width / 2f, groundBottomY * 0.6f)
        ) {
            // 1. Realistic Urban Sunset Sky & City Backdrop
            drawSubwaySunsetBackdrop(this, width, height, horizonY)

            // 2. Realistic 3-Track Subway Corridor with 6 Gleaming Steel Rails
            drawSubwayRailwayCorridor(
                this,
                width,
                horizonY,
                groundBottomY,
                trackWidthHorizon,
                trackWidthBottom,
                state.trackScrollOffset
            )

            // 3. 3D Collectibles & Obstacles sorted Back-to-Front (Depth sorting)
            val renderables = mutableListOf<RenderItem>()
            state.obstacles.forEach { renderables.add(RenderItem.Obs(it)) }
            state.collectibles.forEach { renderables.add(RenderItem.Col(it)) }
            renderables.sortByDescending { it.z }

            for (item in renderables) {
                val z = item.z
                if (z < -0.15f || z > 1.4f) continue

                val depth = (1.0f - z).coerceIn(0.0f, 1.4f)
                val depthCurve = depth * depth.coerceAtLeast(0.01f).toDouble().let { kotlin.math.sqrt(it).toFloat() }
                val y = horizonY + depthCurve * (groundBottomY - horizonY)
                val currentTrackW = trackWidthHorizon + depth * (trackWidthBottom - trackWidthHorizon)
                val laneWidth = currentTrackW / 3f
                val itemScale = (0.24f + 0.76f * depth).coerceIn(0.20f, 1.35f)

                when (item) {
                    is RenderItem.Obs -> {
                        val x = (width / 2f) + (item.obstacle.lane.xOffsetFactor * laneWidth)
                        draw3DObstacle(this, item.obstacle, x, y, laneWidth, itemScale, horizonY)
                    }
                    is RenderItem.Col -> {
                        val x = (width / 2f) + (item.collectible.lane.xOffsetFactor * laneWidth)
                        val elevationOffset = if (item.collectible.isElevated) laneWidth * 0.65f else 0f
                        draw3DCollectible(
                            this,
                            item.collectible,
                            x,
                            y - elevationOffset,
                            laneWidth,
                            itemScale,
                            state.runCycleProgress
                        )
                    }
                }
            }

            // 4. Heroic 3D Quacky Duck Mascot with Cyberpunk Hoverboard
            drawHeroDuck(
                this,
                state,
                width,
                horizonY,
                groundBottomY,
                trackWidthHorizon,
                trackWidthBottom
            )

            // 5. Dynamic Track Sparks, Slide Flames & Dust Particles
            val playerDepth = 0.86f
            val playerGroundY = horizonY + playerDepth * (groundBottomY - horizonY) - 8f
            val playerTrackW = trackWidthHorizon + playerDepth * (trackWidthBottom - trackWidthHorizon)
            val playerLaneW = playerTrackW / 3f
            drawParticles(this, state.particles, width, playerGroundY, playerLaneW)

            // 6. Floating Score & Power-Up Popups
            drawScorePopups(this, state.popups, width, playerGroundY, playerLaneW)

            // 7. High-Speed Edge Streaks & Vignette
            if (state.speed > 0.45f) {
                drawSpeedVignette(this, width, height, state.speed)
            }
        }
    }

    private sealed class RenderItem(val z: Float) {
        class Obs(val obstacle: Obstacle) : RenderItem(obstacle.z)
        class Col(val collectible: Collectible) : RenderItem(collectible.z)
    }

    // =========================================================================
    // 1. URBAN SUNSET SKY & SUBWAY INFRASTRUCTURE
    // =========================================================================
    private fun drawSubwaySunsetBackdrop(
        scope: DrawScope,
        width: Float,
        height: Float,
        horizonY: Float
    ) {
        // Dramatic Sunset Gradient
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF090C16), // Dark indigo night
                    Color(0xFF1F1138), // Twilight purple
                    Color(0xFF4A144E), // Deep magenta
                    Color(0xFF881A38), // Crimson sunset
                    Color(0xFFD35400), // Burning amber
                    Color(0xFFF39C12), // Golden horizon
                    Color(0xFFFFD54F)  // Atmospheric horizon haze
                ),
                startY = 0f,
                endY = horizonY
            ),
            size = Size(width, horizonY)
        )

        // Distant City Skyline (Layer 1 - Far Silhouette)
        val numFarBuildings = 14
        val farBw = width / numFarBuildings
        for (i in 0 until numFarBuildings) {
            val bh = ((i * 37) % 65 + 40).toFloat()
            val bx = i * farBw
            scope.drawRect(
                color = Color(0xFF130922),
                topLeft = Offset(bx, horizonY - bh),
                size = Size(farBw + 1f, bh)
            )
            // Illuminated office windows
            if (i % 2 == 0) {
                for (wy in 1..4) {
                    scope.drawRect(
                        color = Color(0x77FFE082),
                        topLeft = Offset(bx + 4f, horizonY - bh + (wy * 11f)),
                        size = Size(farBw * 0.4f, 4f)
                    )
                }
            }
            // Antenna mast with blinking red beacon
            if (i % 4 == 1) {
                scope.drawLine(
                    color = Color(0xFF351C52),
                    start = Offset(bx + farBw / 2f, horizonY - bh),
                    end = Offset(bx + farBw / 2f, horizonY - bh - 16f),
                    strokeWidth = 2f
                )
                scope.drawCircle(
                    color = Color(0xFFFF1744),
                    radius = 2.5f,
                    center = Offset(bx + farBw / 2f, horizonY - bh - 16f)
                )
            }
        }

        // Midground Brick Warehouse & Subway Arch Bridge
        val archBridgeH = horizonY * 0.45f
        val archPath = Path().apply {
            moveTo(0f, horizonY * 0.95f)
            lineTo(0f, horizonY - archBridgeH)
            quadraticTo(width / 2f, horizonY - archBridgeH - 12f, width, horizonY - archBridgeH)
            lineTo(width, horizonY * 0.95f)
            quadraticTo(width / 2f, horizonY - 14f, 0f, horizonY * 0.95f)
            close()
        }
        scope.drawPath(
            path = archPath,
            color = Color(0xFF1A1528)
        )
        scope.drawPath(
            path = archPath,
            color = Color(0xFF2C223E),
            style = Stroke(width = 2.5f)
        )

        // Overhead High-Voltage Catenary Truss & Electrical Cables
        val catenaryTopY = horizonY * 0.38f
        // Steel lattice cross-girder
        scope.drawLine(
            color = Color(0xFF2E2638),
            start = Offset(width * 0.10f, catenaryTopY),
            end = Offset(width * 0.90f, catenaryTopY),
            strokeWidth = 4f
        )
        // Swooping power lines above the tracks
        listOf(0.28f, 0.50f, 0.72f).forEach { wireXFactor ->
            val wirePath = Path().apply {
                moveTo(width * (wireXFactor - 0.12f), catenaryTopY)
                quadraticTo(width * wireXFactor, catenaryTopY + 18f, width * (wireXFactor + 0.12f), catenaryTopY)
            }
            scope.drawPath(
                path = wirePath,
                color = Color(0x88453852),
                style = Stroke(width = 1.5f)
            )
            // Insulator bead
            scope.drawCircle(
                color = Color(0xFFECEFF1),
                radius = 2f,
                center = Offset(width * wireXFactor, catenaryTopY + 2f)
            )
        }

        // Overhead Railroad Signal Lamps (Green, Amber, Red glow)
        val sigY = catenaryTopY + 8f
        val sigs = listOf(
            Pair(width * 0.34f, Color(0xFF00E676)),
            Pair(width * 0.50f, Color(0xFFFFB300)),
            Pair(width * 0.66f, Color(0xFFFF1744))
        )
        sigs.forEach { (sx, color) ->
            scope.drawCircle(color = color.copy(alpha = 0.35f), radius = 10f, center = Offset(sx, sigY))
            scope.drawCircle(color = Color(0xFF1E1E24), radius = 5.5f, center = Offset(sx, sigY))
            scope.drawCircle(color = color, radius = 3.5f, center = Offset(sx, sigY))
        }
    }

    // =========================================================================
    // 2. SUBWAY RAILWAY TRACKS (6 GLEAMING STEEL RAILS)
    // =========================================================================
    private fun drawSubwayRailwayCorridor(
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

        // Ballast Gravel Roadbed Polygon
        val roadbed = Path().apply {
            moveTo(centerX - hHalf - 12f, horizonY)
            lineTo(centerX + hHalf + 12f, horizonY)
            lineTo(centerX + bHalf + 35f, groundBottomY)
            lineTo(centerX - bHalf - 35f, groundBottomY)
            close()
        }
        scope.drawPath(
            path = roadbed,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF15171E), // Horizon ballast
                    Color(0xFF1E212B),
                    Color(0xFF252936),
                    Color(0xFF1A1C24)  // Foreground ballast
                ),
                startY = horizonY,
                endY = groundBottomY
            )
        )

        // Concrete side retaining curbs
        val leftCurb = Path().apply {
            moveTo(centerX - hHalf - 12f, horizonY)
            lineTo(centerX - hHalf - 2f, horizonY)
            lineTo(centerX - bHalf - 5f, groundBottomY)
            lineTo(centerX - bHalf - 35f, groundBottomY)
            close()
        }
        scope.drawPath(path = leftCurb, color = Color(0xFF101217))

        val rightCurb = Path().apply {
            moveTo(centerX + hHalf + 2f, horizonY)
            lineTo(centerX + hHalf + 12f, horizonY)
            lineTo(centerX + bHalf + 35f, groundBottomY)
            lineTo(centerX + bHalf + 5f, groundBottomY)
            close()
        }
        scope.drawPath(path = rightCurb, color = Color(0xFF101217))

        // Moving 3D Wooden Railway Ties (Sleepers) in Perspective
        val numTies = 16
        for (i in 0 until numTies) {
            val progress = ((i + trackScrollOffset) / numTies.toFloat()) % 1f
            val depth = progress * progress // Mathematical perspective spacing
            val tieY = horizonY + depth * (groundBottomY - horizonY)
            val currentTrackW = trackWidthHorizon + depth * (trackWidthBottom - trackWidthHorizon)
            val tieThickness = (2.2f + 7.5f * depth).coerceAtLeast(2.0f)

            // Tie drop shadow on gravel
            scope.drawLine(
                color = Color(0x66000000),
                start = Offset(centerX - currentTrackW / 2f - 6f, tieY + 2f),
                end = Offset(centerX + currentTrackW / 2f + 6f, tieY + 2f),
                strokeWidth = tieThickness,
                cap = StrokeCap.Round
            )
            // Wooden/concrete sleeper bar
            scope.drawLine(
                color = Color(0xFF2D2926),
                start = Offset(centerX - currentTrackW / 2f - 6f, tieY),
                end = Offset(centerX + currentTrackW / 2f + 6f, tieY),
                strokeWidth = tieThickness,
                cap = StrokeCap.Round
            )
            // Top specular highlight of the sleeper
            scope.drawLine(
                color = Color(0xFF453F3B),
                start = Offset(centerX - currentTrackW / 2f - 4f, tieY - tieThickness * 0.3f),
                end = Offset(centerX + currentTrackW / 2f + 4f, tieY - tieThickness * 0.3f),
                strokeWidth = (tieThickness * 0.35f).coerceAtLeast(1f),
                cap = StrokeCap.Round
            )
        }

        // 6 SHINY METALLIC STEEL RAILS (2 Parallel Rails per Lane)
        // Lane offsets: Left (-1f), Center (0f), Right (+1f)
        val laneFactors = listOf(-1.0f, 0.0f, 1.0f)

        for (laneFactor in laneFactors) {
            // Track gauge (width between two rails in the same track)
            val gaugeH = (trackWidthHorizon / 3f) * 0.58f
            val gaugeB = (trackWidthBottom / 3f) * 0.58f

            val hCenter = centerX + (laneFactor * (trackWidthHorizon / 3f))
            val bCenter = centerX + (laneFactor * (trackWidthBottom / 3f))

            // Two parallel rails: Left rail and Right rail
            val railPairs = listOf(
                Pair(hCenter - gaugeH / 2f, bCenter - gaugeB / 2f),
                Pair(hCenter + gaugeH / 2f, bCenter + gaugeB / 2f)
            )

            for ((hrx, brx) in railPairs) {
                // Rail base shadow
                scope.drawLine(
                    color = Color(0x99000000),
                    start = Offset(hrx + 2f, horizonY),
                    end = Offset(brx + 3f, groundBottomY),
                    strokeWidth = 5f
                )
                // Steel rail body (dark metallic web)
                scope.drawLine(
                    color = Color(0xFF37474F),
                    start = Offset(hrx, horizonY),
                    end = Offset(brx, groundBottomY),
                    strokeWidth = 3.8f
                )
                // Gleaming Chrome/Silver Top Specular Rail Head (reflects sunset)
                scope.drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF90A4AE),
                            Color(0xFFCFD8DC),
                            Color(0xFFFFFFFF),
                            Color(0xFFECEFF1)
                        ),
                        startY = horizonY,
                        endY = groundBottomY
                    ),
                    start = Offset(hrx, horizonY),
                    end = Offset(brx, groundBottomY),
                    strokeWidth = 2.2f
                )
            }
        }

        // Electrified Third Rails / Safety Covers Between Tracks
        listOf(-0.5f, 0.5f).forEach { dividerFactor ->
            val hDivX = centerX + dividerFactor * (trackWidthHorizon / 3f) * 2f
            val bDivX = centerX + dividerFactor * (trackWidthBottom / 3f) * 2f

            scope.drawLine(
                color = Color(0xFFE65100), // Safety orange hazard third rail
                start = Offset(hDivX, horizonY),
                end = Offset(bDivX, groundBottomY),
                strokeWidth = 2.0f
            )
        }
    }

    // =========================================================================
    // 3. HYPER-REALISTIC 3D SUBWAY TRAINS & BARRICADES
    // =========================================================================
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
                // 3D REALISTIC COMMUTER TRAIN CARRIAGE
                val trainW = laneWidth * 0.94f
                val trainH = trainW * 1.62f
                val depth3D = trainW * 0.65f // Perspective extrusion toward vanishing point
                val frontTopY = cy - trainH
                val backTopY = frontTopY - depth3D * 0.40f

                // 1. Train Under-chassis Heavy Drop Shadow
                scope.drawOval(
                    color = Color(0x99000000),
                    topLeft = Offset(cx - trainW * 0.55f, cy - 12f * scaleFactor),
                    size = Size(trainW * 1.10f, 24f * scaleFactor)
                )

                // 2. 3D Corrugated Roof Top Quad (slanted back towards horizon)
                val roofPath = Path().apply {
                    moveTo(cx - trainW / 2f, frontTopY)
                    lineTo(cx + trainW / 2f, frontTopY)
                    lineTo(cx + trainW * 0.38f, backTopY)
                    lineTo(cx - trainW * 0.38f, backTopY)
                    close()
                }
                scope.drawPath(
                    path = roofPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E242B), Color(0xFF2C353F), Color(0xFF37424E)),
                        startY = backTopY,
                        endY = frontTopY
                    )
                )

                // Roof AC Unit & Ventilation Grille
                val acW = trainW * 0.50f
                val acH = depth3D * 0.28f
                scope.drawRoundRect(
                    color = Color(0xFF1A1F26),
                    topLeft = Offset(cx - acW / 2f, backTopY + 4f * scaleFactor),
                    size = Size(acW, acH),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // 3. 3D Perspective Side Wall (visible based on lane position)
                val isLeftLane = obstacle.lane.index == 0
                val isRightLane = obstacle.lane.index == 2

                if (isLeftLane) {
                    // Right side wall visible to the player in center/right
                    val sidePath = Path().apply {
                        moveTo(cx + trainW / 2f, frontTopY)
                        lineTo(cx + trainW * 0.38f, backTopY)
                        lineTo(cx + trainW * 0.38f, cy - 14f * scaleFactor)
                        lineTo(cx + trainW / 2f, cy)
                        close()
                    }
                    scope.drawPath(
                        path = sidePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF28313A), Color(0xFF1B2228)),
                            startX = cx + trainW * 0.38f,
                            endX = cx + trainW / 2f
                        )
                    )
                    // Side Glowing Passenger Windows
                    val winY1 = frontTopY + trainH * 0.25f
                    val winY2 = backTopY + trainH * 0.25f
                    for (w in 1..2) {
                        val frac = w * 0.32f
                        val wx = (cx + trainW / 2f) * (1f - frac) + (cx + trainW * 0.38f) * frac
                        val wy = winY1 * (1f - frac) + winY2 * frac
                        scope.drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFEE58), Color(0xFFFFA000)),
                                startY = wy,
                                endY = wy + trainH * 0.16f
                            ),
                            topLeft = Offset(wx - 2f, wy),
                            size = Size(trainW * 0.08f, trainH * 0.18f * (1f - frac * 0.3f)),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                } else if (isRightLane) {
                    // Left side wall visible
                    val sidePath = Path().apply {
                        moveTo(cx - trainW / 2f, frontTopY)
                        lineTo(cx - trainW * 0.38f, backTopY)
                        lineTo(cx - trainW * 0.38f, cy - 14f * scaleFactor)
                        lineTo(cx - trainW / 2f, cy)
                        close()
                    }
                    scope.drawPath(
                        path = sidePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1B2228), Color(0xFF28313A)),
                            startX = cx - trainW / 2f,
                            endX = cx - trainW * 0.38f
                        )
                    )
                    // Side Passenger Windows
                    val winY1 = frontTopY + trainH * 0.25f
                    val winY2 = backTopY + trainH * 0.25f
                    for (w in 1..2) {
                        val frac = w * 0.32f
                        val wx = (cx - trainW / 2f) * (1f - frac) + (cx - trainW * 0.38f) * frac
                        val wy = winY1 * (1f - frac) + winY2 * frac
                        scope.drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFEE58), Color(0xFFFFA000)),
                                startY = wy,
                                endY = wy + trainH * 0.16f
                            ),
                            topLeft = Offset(wx - trainW * 0.06f, wy),
                            size = Size(trainW * 0.08f, trainH * 0.18f * (1f - frac * 0.3f)),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }

                // 4. Front Cab Face with Rounded Corner Aerodynamics
                scope.drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF283593), // Cobalt commuter blue
                            Color(0xFF1A237E),
                            Color(0xFF121858),
                            Color(0xFF0D123D)
                        ),
                        startY = frontTopY,
                        endY = cy
                    ),
                    topLeft = Offset(cx - trainW / 2f, frontTopY),
                    size = Size(trainW, trainH),
                    cornerRadius = CornerRadius(9f * scaleFactor, 9f * scaleFactor)
                )

                // Front Outer Bezel Rim
                scope.drawRoundRect(
                    color = Color(0xFF5C6BC0),
                    topLeft = Offset(cx - trainW / 2f, frontTopY),
                    size = Size(trainW, trainH),
                    cornerRadius = CornerRadius(9f * scaleFactor, 9f * scaleFactor),
                    style = Stroke(width = 2.5f * scaleFactor)
                )

                // 5. LED Matrix Destination Rollsign ("QUACK EXP")
                val signW = trainW * 0.62f
                val signH = trainH * 0.09f
                val signY = frontTopY + trainH * 0.06f
                scope.drawRoundRect(
                    color = Color(0xFF0A0A0D),
                    topLeft = Offset(cx - signW / 2f, signY),
                    size = Size(signW, signH),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Amber dot-matrix text glow
                scope.drawRoundRect(
                    color = Color(0xFFFFB300),
                    topLeft = Offset(cx - signW * 0.40f, signY + signH * 0.25f),
                    size = Size(signW * 0.80f, signH * 0.50f),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // 6. Large Curved Windshield
                val winW = trainW * 0.82f
                val winH = trainH * 0.30f
                val winTopY = frontTopY + trainH * 0.18f
                scope.drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D47A1), Color(0xFF002171)),
                        startY = winTopY,
                        endY = winTopY + winH
                    ),
                    topLeft = Offset(cx - winW / 2f, winTopY),
                    size = Size(winW, winH),
                    cornerRadius = CornerRadius(6f * scaleFactor, 6f * scaleFactor)
                )
                // Interior driver cab console glow
                scope.drawRoundRect(
                    color = Color(0x44FFE082),
                    topLeft = Offset(cx - winW * 0.42f, winTopY + winH * 0.55f),
                    size = Size(winW * 0.84f, winH * 0.40f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Windshield wiper divider
                scope.drawLine(
                    color = Color(0xFF1A237E),
                    start = Offset(cx, winTopY),
                    end = Offset(cx, winTopY + winH),
                    strokeWidth = 2.5f * scaleFactor
                )

                // 7. Bold Yellow/Black Reflective Hazard Band across front
                val bandY = frontTopY + trainH * 0.52f
                val bandH = trainH * 0.12f
                scope.drawRect(
                    color = Color(0xFFFFD600),
                    topLeft = Offset(cx - trainW / 2f, bandY),
                    size = Size(trainW, bandH)
                )
                val stripeCount = 6
                val sWidth = trainW / stripeCount
                for (s in 0 until stripeCount) {
                    val sx = cx - trainW / 2f + s * sWidth
                    val stripe = Path().apply {
                        moveTo(sx, bandY + bandH)
                        lineTo(sx + sWidth * 0.5f, bandY + bandH)
                        lineTo(sx + sWidth * 0.9f, bandY)
                        lineTo(sx + sWidth * 0.4f, bandY)
                        close()
                    }
                    scope.drawPath(path = stripe, color = Color(0xFF212121))
                }

                // 8. Blinding Halogen Projector Headlights with Volumetric Track Beams
                val lightRadius = 10f * scaleFactor
                val leftLightCenter = Offset(cx - trainW * 0.32f, frontTopY + trainH * 0.76f)
                val rightLightCenter = Offset(cx + trainW * 0.32f, frontTopY + trainH * 0.76f)

                // Volumetric Light Cones Shining Down on the Tracks
                val beamLeft = Path().apply {
                    moveTo(leftLightCenter.x, leftLightCenter.y)
                    lineTo(cx - trainW * 0.75f, cy + 30f * scaleFactor)
                    lineTo(cx - trainW * 0.05f, cy + 30f * scaleFactor)
                    close()
                }
                scope.drawPath(
                    path = beamLeft,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x44FFFDE7), Color(0x00FFFDE7)),
                        startY = leftLightCenter.y,
                        endY = cy + 30f * scaleFactor
                    )
                )

                val beamRight = Path().apply {
                    moveTo(rightLightCenter.x, rightLightCenter.y)
                    lineTo(cx + trainW * 0.05f, cy + 30f * scaleFactor)
                    lineTo(cx + trainW * 0.75f, cy + 30f * scaleFactor)
                    close()
                }
                scope.drawPath(
                    path = beamRight,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x44FFFDE7), Color(0x00FFFDE7)),
                        startY = rightLightCenter.y,
                        endY = cy + 30f * scaleFactor
                    )
                )

                // Headlight Chrome Bezels & Projector Bulbs
                listOf(leftLightCenter, rightLightCenter).forEach { pos ->
                    scope.drawCircle(color = Color(0x66FFECB3), radius = lightRadius * 2.2f, center = pos)
                    scope.drawCircle(color = Color(0xFFCFD8DC), radius = lightRadius + 2f, center = pos)
                    scope.drawCircle(color = Color(0xFF455A64), radius = lightRadius, center = pos)
                    scope.drawCircle(color = Color(0xFFFFFFFD), radius = lightRadius * 0.8f, center = pos)
                }

                // 9. Heavy Steel Cowcatcher & Knuckle Coupler
                val bumperY = cy - trainH * 0.10f
                scope.drawRoundRect(
                    color = Color(0xFF101318),
                    topLeft = Offset(cx - trainW * 0.46f, bumperY),
                    size = Size(trainW * 0.92f, trainH * 0.09f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Center automatic coupler
                scope.drawRoundRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(cx - 7f * scaleFactor, cy - 8f * scaleFactor),
                    size = Size(14f * scaleFactor, 12f * scaleFactor),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Optional Front Climb Ramp
                if (obstacle.hasRamp) {
                    val rampPath = Path().apply {
                        moveTo(cx - trainW * 0.35f, cy)
                        lineTo(cx + trainW * 0.35f, cy)
                        lineTo(cx + trainW * 0.30f, frontTopY + trainH * 0.45f)
                        lineTo(cx - trainW * 0.30f, frontTopY + trainH * 0.45f)
                        close()
                    }
                    scope.drawPath(
                        path = rampPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFD600), Color(0xFFF57F17)),
                            startY = frontTopY + trainH * 0.45f,
                            endY = cy
                        )
                    )
                    // Upward hazard arrow on ramp
                    val arrowPath = Path().apply {
                        moveTo(cx, frontTopY + trainH * 0.60f)
                        lineTo(cx + 12f * scaleFactor, frontTopY + trainH * 0.75f)
                        lineTo(cx - 12f * scaleFactor, frontTopY + trainH * 0.75f)
                        close()
                    }
                    scope.drawPath(path = arrowPath, color = Color(0xFF212121))
                }
            }
            ObstacleType.LOW_BARRIER -> {
                // REALISTIC CONSTRUCTION HAZARD ROADBLOCK
                val barW = laneWidth * 0.90f
                val barH = laneWidth * 0.50f
                val topY = cy - barH

                // Cast Iron A-Frame Legs with Depth
                val legW = 9f * scaleFactor
                scope.drawLine(Color(0xFF263238), Offset(cx - barW * 0.44f, cy), Offset(cx - barW * 0.36f, topY - 6f), legW)
                scope.drawLine(Color(0xFF37474F), Offset(cx - barW * 0.36f, cy), Offset(cx - barW * 0.36f, topY - 6f), legW * 0.7f)
                scope.drawLine(Color(0xFF263238), Offset(cx + barW * 0.44f, cy), Offset(cx + barW * 0.36f, topY - 6f), legW)
                scope.drawLine(Color(0xFF37474F), Offset(cx + barW * 0.36f, cy), Offset(cx + barW * 0.36f, topY - 6f), legW * 0.7f)

                // Dual Heavy Planks with Diagonal Yellow/Black Reflective Chevrons
                val plankH = barH * 0.35f
                val plank1Y = topY
                val plank2Y = topY + plankH * 1.15f

                listOf(plank1Y, plank2Y).forEach { py ->
                    // 3D Top wood edge
                    scope.drawRect(
                        color = Color(0xFFFFB300),
                        topLeft = Offset(cx - barW / 2f, py - 3f * scaleFactor),
                        size = Size(barW, 3f * scaleFactor)
                    )
                    // Yellow reflective board
                    scope.drawRoundRect(
                        color = Color(0xFFFFC107),
                        topLeft = Offset(cx - barW / 2f, py),
                        size = Size(barW, plankH),
                        cornerRadius = CornerRadius(3f * scaleFactor, 3f * scaleFactor)
                    )
                    // Black hazard chevrons
                    val numStripes = 6
                    val stripeWidth = barW / numStripes
                    for (s in 0 until numStripes) {
                        val sx = cx - barW / 2f + s * stripeWidth
                        val stripePath = Path().apply {
                            moveTo(sx, py + plankH)
                            lineTo(sx + stripeWidth * 0.48f, py + plankH)
                            lineTo(sx + stripeWidth * 0.90f, py)
                            lineTo(sx + stripeWidth * 0.42f, py)
                            close()
                        }
                        scope.drawPath(path = stripePath, color = Color(0xFF212121))
                    }
                }

                // Dual Pulsing Amber Strobe Warning Beacons on Top Posts
                listOf(cx - barW * 0.36f, cx + barW * 0.36f).forEach { bx ->
                    val bCenter = Offset(bx, topY - 12f * scaleFactor)
                    val bRadius = 8f * scaleFactor
                    scope.drawCircle(color = Color(0x66FF8F00), radius = bRadius * 2.2f, center = bCenter)
                    scope.drawCircle(color = Color(0xFFFFD54F), radius = bRadius, center = bCenter)
                    scope.drawCircle(color = Color(0xFFFFFFFF), radius = bRadius * 0.4f, center = bCenter)
                }
            }
            ObstacleType.HIGH_BARRIER -> {
                // INDUSTRIAL STEEL TRUSS OVERHEAD GANTRY CLEARANCE
                val gantryW = laneWidth * 0.96f
                val towerW = 14f * scaleFactor
                val beamH = 30f * scaleFactor
                val clearanceH = laneWidth * 0.68f // High enough for duck to slide under
                val beamTopY = cy - clearanceH - beamH

                // Steel Lattice Vertical Support Columns
                val colBrush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF37474F), Color(0xFF546E7A), Color(0xFF263238))
                )
                // Left tower
                scope.drawRect(
                    brush = colBrush,
                    topLeft = Offset(cx - gantryW / 2f, beamTopY),
                    size = Size(towerW, clearanceH + beamH)
                )
                // Right tower
                scope.drawRect(
                    brush = colBrush,
                    topLeft = Offset(cx + gantryW / 2f - towerW, beamTopY),
                    size = Size(towerW, clearanceH + beamH)
                )

                // Main Overhead Steel Cross-Beam
                scope.drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF455A64), Color(0xFF263238)),
                        startY = beamTopY,
                        endY = beamTopY + beamH
                    ),
                    topLeft = Offset(cx - gantryW / 2f, beamTopY),
                    size = Size(gantryW, beamH),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Bold Warning Clearance Sign: "CAUTION - DUCK"
                val signH = beamH * 0.65f
                val signY = beamTopY + beamH * 0.18f
                scope.drawRect(
                    color = Color(0xFFFFD600),
                    topLeft = Offset(cx - gantryW * 0.42f, signY),
                    size = Size(gantryW * 0.84f, signH)
                )
                // Downward warning hazard chevrons
                val numChevrons = 8
                val cW = (gantryW * 0.84f) / numChevrons
                for (c in 0 until numChevrons) {
                    val chx = cx - gantryW * 0.42f + c * cW
                    val chPath = Path().apply {
                        moveTo(chx, signY)
                        lineTo(chx + cW * 0.5f, signY + signH)
                        lineTo(chx + cW, signY)
                        close()
                    }
                    scope.drawPath(path = chPath, color = Color(0xFF212121))
                }

                // Hanging Red-and-White Clearance Warning Pipes
                val pipeCount = 3
                val pipeSpacing = gantryW * 0.60f / (pipeCount + 1)
                for (p in 1..pipeCount) {
                    val px = cx - gantryW * 0.30f + p * pipeSpacing
                    val chainTopY = beamTopY + beamH
                    val chainLen = 14f * scaleFactor
                    // Chain
                    scope.drawLine(
                        color = Color(0xFFCFD8DC),
                        start = Offset(px, chainTopY),
                        end = Offset(px, chainTopY + chainLen),
                        strokeWidth = 2f
                    )
                    // Hanging pipe
                    scope.drawRoundRect(
                        color = if (p % 2 == 0) Color(0xFFD32F2F) else Color(0xFFFFFFFF),
                        topLeft = Offset(px - 4f * scaleFactor, chainTopY + chainLen),
                        size = Size(8f * scaleFactor, 18f * scaleFactor),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }
        }
    }

    // =========================================================================
    // 4. 3D SPINNING GOLD COINS & POWER-UPS
    // =========================================================================
    private fun draw3DCollectible(
        scope: DrawScope,
        col: Collectible,
        cx: Float,
        cy: Float,
        laneWidth: Float,
        scaleFactor: Float,
        runCycleProgress: Float
    ) {
        when (col.type) {
            CollectibleType.COIN -> {
                // TRUE 3D CYLINDRICAL SPINNING GOLD COIN
                val coinBaseR = laneWidth * 0.28f * scaleFactor
                // Bobbing levitation
                val bob = sin((col.id * 1.3f) + (runCycleProgress * 2 * PI.toFloat())) * 5f * scaleFactor
                val coinCenter = Offset(cx, cy + bob)

                // 3D Rotation around Y axis
                val spinAngle = ((col.id * 1.5f) + (runCycleProgress * 4 * PI.toFloat()))
                val cosSpin = cos(spinAngle)
                val widthFactor = abs(cosSpin).coerceAtLeast(0.12f)
                val coinW = coinBaseR * 2f * widthFactor
                val coinH = coinBaseR * 2f

                // Radiating Golden Glow Halo
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x77FFD700), Color(0x22FFA000), Color(0x00FFD700)),
                        center = coinCenter,
                        radius = coinBaseR * 2.2f
                    ),
                    radius = coinBaseR * 2.2f,
                    center = coinCenter
                )

                // Coin 3D Rim Depth
                val rimOffset = if (cosSpin >= 0) 5f * scaleFactor else -5f * scaleFactor
                scope.drawOval(
                    color = Color(0xFFB8860B), // Dark bronze rim
                    topLeft = Offset(coinCenter.x - coinW / 2f + rimOffset, coinCenter.y - coinH / 2f),
                    size = Size(coinW, coinH)
                )

                // Main Coin Golden Face
                scope.drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFEE), // Specular glint core
                            Color(0xFFFFD54F), // Bright gold
                            Color(0xFFFFB300), // Rich gold
                            Color(0xFFE65100)  // Deep bevel edge
                        ),
                        center = Offset(coinCenter.x - coinW * 0.15f, coinCenter.y - coinH * 0.15f),
                        radius = coinBaseR * 1.1f
                    ),
                    topLeft = Offset(coinCenter.x - coinW / 2f, coinCenter.y - coinH / 2f),
                    size = Size(coinW, coinH)
                )

                // Embossed Concentric Inner Ridge
                scope.drawOval(
                    color = Color(0xFFFFD54F),
                    topLeft = Offset(coinCenter.x - (coinW * 0.70f) / 2f, coinCenter.y - (coinH * 0.70f) / 2f),
                    size = Size(coinW * 0.70f, coinH * 0.70f),
                    style = Stroke(width = 2.2f * scaleFactor)
                )

                // Embossed Center Star / Emblem
                if (widthFactor > 0.45f) {
                    val starR = coinBaseR * 0.32f * widthFactor
                    scope.drawCircle(
                        color = Color(0xFFFFF9C4),
                        radius = starR,
                        center = coinCenter
                    )
                }
            }
            CollectibleType.MAGNET -> {
                // 3D HORSESHOE MAGNET WITH ELECTRIC ARCS
                val magSize = laneWidth * 0.44f * scaleFactor
                val magY = cy - magSize / 2f

                // Outer horseshoe body
                scope.drawArc(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE53935), Color(0xFFB71C1C))
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - magSize / 2f, magY),
                    size = Size(magSize, magSize),
                    style = Stroke(width = magSize * 0.32f, cap = StrokeCap.Square)
                )
                // Silver magnetic pole tips
                listOf(cx - magSize / 2f, cx + magSize / 2f - magSize * 0.32f).forEach { px ->
                    scope.drawRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFFECEFF1), Color(0xFF90A4AE))),
                        topLeft = Offset(px, magY + magSize * 0.35f),
                        size = Size(magSize * 0.32f, magSize * 0.28f)
                    )
                }
                // Electric blue lightning sparks
                scope.drawCircle(color = Color(0xFF00E5FF), radius = 4f * scaleFactor, center = Offset(cx, cy))
            }
            CollectibleType.DASH_BOOST -> {
                // ROCKET JETPACK POWER-UP
                val rocketW = laneWidth * 0.40f * scaleFactor
                val rocketH = rocketW * 1.3f
                // Rocket body
                scope.drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFF7043), Color(0xFFD84315))),
                    topLeft = Offset(cx - rocketW / 2f, cy - rocketH / 2f),
                    size = Size(rocketW, rocketH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Jet thruster flames
                scope.drawCircle(color = Color(0xFFFFEB3B), radius = rocketW * 0.25f, center = Offset(cx, cy + rocketH * 0.45f))
            }
            CollectibleType.SHIELD -> {
                // GEODESIC ENERGY DOME SHIELD
                val shieldR = laneWidth * 0.32f * scaleFactor
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x3300E676), Color(0x9900E676), Color(0xFF00E676)),
                        center = Offset(cx, cy),
                        radius = shieldR
                    ),
                    radius = shieldR,
                    center = Offset(cx, cy)
                )
                scope.drawCircle(color = Color.White, radius = shieldR, center = Offset(cx, cy), style = Stroke(width = 2.5f))
            }
            CollectibleType.MULTIPLIER_2X -> {
                // 3D 2X MULTIPLIER STAR BADGE
                val badgeR = laneWidth * 0.30f * scaleFactor
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF4081), Color(0xFFC2185B)),
                        center = Offset(cx, cy),
                        radius = badgeR
                    ),
                    radius = badgeR,
                    center = Offset(cx, cy)
                )
                scope.drawCircle(color = Color.White, radius = badgeR, center = Offset(cx, cy), style = Stroke(width = 2.5f))
            }
            CollectibleType.HOVERBOARD_PICKUP -> {
                // CYBER HOVERBOARD PICKUP CRATE
                val crateW = laneWidth * 0.48f * scaleFactor
                val crateH = crateW * 0.42f
                scope.drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFF0091EA))),
                    topLeft = Offset(cx - crateW / 2f, cy - crateH / 2f),
                    size = Size(crateW, crateH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                scope.drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(cx - crateW / 2f, cy - crateH / 2f),
                    size = Size(crateW, crateH),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 2f)
                )
            }
        }
    }

    // =========================================================================
    // 5. HEROIC 3D QUACKY MASCOT & CYBERPUNK HOVERBOARD
    // =========================================================================
    private fun drawHeroDuck(
        scope: DrawScope,
        state: SurferGameState,
        width: Float,
        horizonY: Float,
        groundBottomY: Float,
        trackWidthHorizon: Float,
        trackWidthBottom: Float
    ) {
        val playerDepth = 0.86f
        val groundY = horizonY + playerDepth * (groundBottomY - horizonY) - 8f
        val currentTrackW = trackWidthHorizon + playerDepth * (trackWidthBottom - trackWidthHorizon)
        val laneWidth = currentTrackW / 3f

        val playerX = (width / 2f) + (state.lanePositionFloat * laneWidth)
        val duckBaseW = laneWidth * 0.68f
        val duckBaseH = duckBaseW * 1.15f

        // Jump physics parabola
        val jumpHeight = if (state.isJumping) {
            val progress = state.jumpProgress
            4.0f * progress * (1.0f - progress) * (laneWidth * 1.65f)
        } else 0f

        // Crouch physics on slide
        val duckScaleY = if (state.isSliding) 0.50f else if (state.isJumping) 1.08f else 1.0f
        val duckScaleX = if (state.isSliding) 1.30f else 1.0f

        val animatedDuckY = groundY - jumpHeight
        val shadowScale = (1.0f - (jumpHeight / (laneWidth * 1.8f))).coerceIn(0.35f, 1.0f)

        // 1. Ground Drop Shadow
        scope.drawOval(
            color = Color(0x66000000).copy(alpha = 0.45f * shadowScale),
            topLeft = Offset(playerX - (duckBaseW * 0.55f * shadowScale), groundY - (10f * shadowScale)),
            size = Size(duckBaseW * 1.10f * shadowScale, 20f * shadowScale)
        )

        val stepAngle = state.runCycleProgress * 2 * PI.toFloat()
        val bounceY = if (!state.isJumping && !state.isSliding) abs(sin(stepAngle)) * 8f else 0f

        scope.translate(left = playerX, top = animatedDuckY - bounceY) {
            // Apply character bank tilt when swiping lanes
            rotate(degrees = state.cameraRollDegrees * 1.5f, pivot = Offset(0f, 0f)) {
                scale(scaleX = duckScaleX, scaleY = duckScaleY, pivot = Offset(0f, 0f)) {
                    val duckW = duckBaseW
                    val duckH = duckBaseH

                    // 2. CYBERPUNK HOVERBOARD (When active)
                    if (state.isHoverboardActive) {
                        val boardW = duckW * 1.45f
                        val boardH = duckH * 0.28f
                        val boardY = duckH * 0.38f

                        // Hoverboard deck
                        scope.drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF00E5FF))
                            ),
                            topLeft = Offset(-boardW / 2f, boardY),
                            size = Size(boardW, boardH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        // Neon edge glow
                        scope.drawRoundRect(
                            color = Color(0xFFFFFFFF),
                            topLeft = Offset(-boardW / 2f, boardY),
                            size = Size(boardW, boardH),
                            cornerRadius = CornerRadius(10f, 10f),
                            style = Stroke(width = 2.5f)
                        )
                        // Twin rear plasma thrusters
                        listOf(-boardW * 0.35f, boardW * 0.35f).forEach { tx ->
                            scope.drawCircle(
                                color = Color(0xFF00E5FF),
                                radius = 7f,
                                center = Offset(tx, boardY + boardH * 0.6f)
                            )
                            scope.drawCircle(
                                color = Color.White,
                                radius = 3.5f,
                                center = Offset(tx, boardY + boardH * 0.6f)
                            )
                        }
                    }

                    // 3. SPRINTING LEGS & ATHLETIC PADDLE SNEAKERS
                    if (!state.isSliding && !state.isHoverboardActive) {
                        val hipY = duckH * 0.18f
                        val legStroke = 5.5f
                        val leftHipX = -duckW * 0.20f
                        val rightHipX = duckW * 0.20f

                        val leftPhase = sin(stepAngle)
                        val rightPhase = sin(stepAngle + PI.toFloat())

                        val maxFootDrop = duckH * 0.46f
                        val leftFootY = maxFootDrop - (leftPhase.coerceAtLeast(0f) * 16f)
                        val rightFootY = maxFootDrop - (rightPhase.coerceAtLeast(0f) * 16f)

                        listOf(
                            Triple(leftHipX, leftHipX + leftPhase * 10f, leftFootY),
                            Triple(rightHipX, rightHipX + rightPhase * 10f, rightFootY)
                        ).forEach { (hx, fx, fy) ->
                            // Leg segment
                            scope.drawLine(
                                color = Color(0xFFF57C00),
                                start = Offset(hx, hipY),
                                end = Offset(fx, fy),
                                strokeWidth = legStroke,
                                cap = StrokeCap.Round
                            )
                            // Athletic Sneaker / Paddle Shoe (White rubber sole + Red/Orange body)
                            val shoeW = duckW * 0.34f
                            val shoeH = duckH * 0.18f
                            scope.drawRoundRect(
                                color = Color(0xFFD32F2F), // Red athletic shoe
                                topLeft = Offset(fx - shoeW / 2f, fy - shoeH * 0.4f),
                                size = Size(shoeW, shoeH),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                            // White rubber sole
                            scope.drawRoundRect(
                                color = Color(0xFFFFFFFF),
                                topLeft = Offset(fx - shoeW / 2f, fy + shoeH * 0.25f),
                                size = Size(shoeW, shoeH * 0.35f),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                        }
                    }

                    // 4. Volumetric 3D Duck Torso
                    val bodyW = duckW * 0.84f
                    val bodyH = duckH * 0.74f
                    val bodyCenter = Offset(0f, duckH * 0.05f)

                    val bodyColorTop = if (state.activePowerUp == CollectibleType.DASH_BOOST) Color(0xFFFFF176) else Color(0xFFFFFFFF)
                    val bodyColorBottom = if (state.activePowerUp == CollectibleType.DASH_BOOST) Color(0xFFFFB300) else Color(0xFFCFD8DC)

                    scope.drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(bodyColorTop, Color(0xFFECEFF1), bodyColorBottom),
                            center = Offset(bodyCenter.x, bodyCenter.y - bodyH * 0.15f),
                            radius = bodyW * 0.65f
                        ),
                        topLeft = Offset(bodyCenter.x - bodyW * 0.50f, bodyCenter.y - bodyH * 0.46f),
                        size = Size(bodyW, bodyH)
                    )

                    // 5. Dynamic Flapping Wings
                    val wingW = duckW * 0.32f
                    val wingH = duckH * 0.52f
                    val leftWingAngle = if (state.isJumping) -34f else if (state.isSliding) -8f else sin(stepAngle) * 24f - 10f
                    val rightWingAngle = if (state.isJumping) 34f else if (state.isSliding) 8f else sin(stepAngle + PI.toFloat()) * 24f + 10f

                    scope.rotate(degrees = leftWingAngle, pivot = Offset(-bodyW * 0.38f, bodyCenter.y - wingH * 0.2f)) {
                        val wingPath = Path().apply {
                            val sx = -bodyW * 0.36f
                            val sy = bodyCenter.y - wingH * 0.2f
                            moveTo(sx, sy)
                            cubicTo(sx - wingW * 1.2f, sy + wingH * 0.2f, sx - wingW * 0.9f, sy + wingH * 0.9f, sx - wingW * 0.2f, sy + wingH)
                            cubicTo(sx + wingW * 0.1f, sy + wingH * 0.6f, sx + wingW * 0.1f, sy + wingH * 0.2f, sx, sy)
                            close()
                        }
                        scope.drawPath(path = wingPath, brush = Brush.linearGradient(listOf(bodyColorTop, bodyColorBottom)))
                    }

                    scope.rotate(degrees = rightWingAngle, pivot = Offset(bodyW * 0.38f, bodyCenter.y - wingH * 0.2f)) {
                        val wingPath = Path().apply {
                            val sx = bodyW * 0.36f
                            val sy = bodyCenter.y - wingH * 0.2f
                            moveTo(sx, sy)
                            cubicTo(sx + wingW * 1.2f, sy + wingH * 0.2f, sx + wingW * 0.9f, sy + wingH * 0.9f, sx + wingW * 0.2f, sy + wingH)
                            cubicTo(sx - wingW * 0.1f, sy + wingH * 0.6f, sx - wingW * 0.1f, sy + wingH * 0.2f, sx, sy)
                            close()
                        }
                        scope.drawPath(path = wingPath, brush = Brush.linearGradient(listOf(bodyColorTop, bodyColorBottom)))
                    }

                    // 6. Volumetric 3D Duck Head & Street Snapback Cap
                    val headR = duckW * 0.33f
                    val headCenterY = bodyCenter.y - bodyH * 0.45f

                    scope.drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(bodyColorTop, Color(0xFFECEFF1), bodyColorBottom),
                            center = Offset(0f, headCenterY - headR * 0.25f),
                            radius = headR * 1.1f
                        ),
                        radius = headR,
                        center = Offset(0f, headCenterY)
                    )

                    // Street Style Backwards Snapback Cap (Red with white duck logo)
                    val capY = headCenterY - headR * 0.45f
                    scope.drawArc(
                        brush = Brush.verticalGradient(listOf(Color(0xFFE53935), Color(0xFFC62828))),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(-headR * 0.98f, capY - headR * 0.55f),
                        size = Size(headR * 1.96f, headR * 1.10f)
                    )
                    // Backwards cap brim
                    scope.drawOval(
                        color = Color(0xFFB71C1C),
                        topLeft = Offset(-headR * 0.70f, capY - headR * 0.05f),
                        size = Size(headR * 1.40f, headR * 0.35f)
                    )

                    // 7. 3/4 Beak Pointing Forward
                    val beakW = duckW * 0.36f
                    val beakH = duckH * 0.18f
                    val beakY = headCenterY - headR * 0.25f
                    val beakPath = Path().apply {
                        moveTo(-beakW * 0.35f, beakY)
                        cubicTo(-beakW * 0.30f, beakY - beakH * 1.1f, beakW * 0.30f, beakY - beakH * 1.1f, beakW * 0.35f, beakY)
                        cubicTo(beakW * 0.20f, beakY + beakH * 0.3f, -beakW * 0.20f, beakY + beakH * 0.3f, -beakW * 0.35f, beakY)
                        close()
                    }
                    scope.drawPath(
                        path = beakPath,
                        brush = Brush.verticalGradient(listOf(Color(0xFFFFB74D), Color(0xFFFF9800), Color(0xFFF57C00)))
                    )

                    // 8. Expressive Eyes
                    listOf(-1f, 1f).forEach { side ->
                        val eyeX = side * (headR * 0.58f)
                        val eyeY = headCenterY - headR * 0.05f
                        val eyeR = headR * 0.20f
                        scope.drawOval(color = Color(0xFF1E1E24), topLeft = Offset(eyeX - eyeR, eyeY - eyeR), size = Size(eyeR * 2f, eyeR * 2f))
                        if (state.duckBlink < 0.5f) {
                            scope.drawCircle(color = Color.White, radius = eyeR * 0.45f, center = Offset(eyeX + side * eyeR * 0.25f, eyeY - eyeR * 0.25f))
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 6. FLOATING SCORE POPUPS, PARTICLES & SPEED VIGNETTE
    // =========================================================================
    private fun drawScorePopups(
        scope: DrawScope,
        popups: List<ScorePopup>,
        width: Float,
        groundY: Float,
        laneWidth: Float
    ) {
        val centerX = width / 2f
        for (p in popups) {
            val px = centerX + ((p.laneIndex - 1) * laneWidth)
            val py = groundY - 120f + p.yOffset

            scope.drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = p.color.copy(alpha = p.alpha).toArgb()
                    textSize = 42f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(10f, 0f, 0f, android.graphics.Color.BLACK)
                }
                drawText(p.text, px, py, paint)
            }
        }
    }

    private fun drawParticles(
        scope: DrawScope,
        particles: List<SurferParticle>,
        width: Float,
        groundY: Float,
        laneWidth: Float
    ) {
        val centerX = width / 2f
        for (p in particles) {
            val px = centerX + (p.x * laneWidth) + (p.vx * 20f)
            val py = groundY - 20f + (p.y * 36f)
            scope.drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(px, py)
            )
        }
    }

    private fun drawSpeedVignette(
        scope: DrawScope,
        width: Float,
        height: Float,
        speed: Float
    ) {
        val intensity = ((speed - 0.45f) / 0.27f).coerceIn(0f, 1f)
        // High-speed wind streaks at screen edges
        val numLines = 8
        for (i in 0 until numLines) {
            val ly = (height * 0.15f) + (i * height * 0.09f)
            val lineLen = (50f + i * 15f) * intensity
            // Left streak
            scope.drawLine(
                color = Color.White.copy(alpha = 0.25f * intensity),
                start = Offset(0f, ly),
                end = Offset(lineLen, ly + 8f),
                strokeWidth = 2f
            )
            // Right streak
            scope.drawLine(
                color = Color.White.copy(alpha = 0.25f * intensity),
                start = Offset(width, ly),
                end = Offset(width - lineLen, ly + 8f),
                strokeWidth = 2f
            )
        }
    }
}
