package app.quacky.feature.surfer.domain

import androidx.compose.ui.graphics.Color
import app.quacky.feature.surfer.model.Collectible
import app.quacky.feature.surfer.model.CollectibleType
import app.quacky.feature.surfer.model.GameStatus
import app.quacky.feature.surfer.model.Obstacle
import app.quacky.feature.surfer.model.ObstacleType
import app.quacky.feature.surfer.model.ScorePopup
import app.quacky.feature.surfer.model.SurferGameState
import app.quacky.feature.surfer.model.SurferLane
import app.quacky.feature.surfer.model.SurferParticle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class SurferEngine(
    private val random: Random = Random(System.currentTimeMillis())
) {
    private var nextEntityId = 1L
    private var nextSpawnZ = 1.0f
    private var nextPowerUpDistance = 50f

    enum class GameEvent {
        LANE_SWITCH,
        JUMP,
        SLIDE,
        COIN_PICKUP,
        POWERUP_PICKUP,
        HOVERBOARD_ACTIVATE,
        HOVERBOARD_BREAK,
        SHIELD_BREAK,
        OBSTACLE_SMASHED,
        CRASH
    }

    data class EngineStepResult(
        val state: SurferGameState,
        val events: List<GameEvent>
    )

    fun startNewGame(highScore: Int, totalCoins: Int): SurferGameState {
        nextEntityId = 1L
        nextSpawnZ = 1.0f
        nextPowerUpDistance = 45f

        val initialObstacles = mutableListOf<Obstacle>()
        val initialCoins = mutableListOf<Collectible>()

        // Generate initial track layout ahead of player
        var curZ = 1.2f
        repeat(5) {
            generateTrackSegment(curZ, initialObstacles, initialCoins, 0f)
            curZ += 0.55f
        }
        nextSpawnZ = curZ

        return SurferGameState(
            status = GameStatus.PLAYING,
            currentLane = SurferLane.CENTER,
            targetLane = SurferLane.CENTER,
            lanePositionFloat = 0.0f,
            highScore = highScore,
            totalCoins = totalCoins,
            speed = 0.40f,
            hoverboardsInventory = 3,
            obstacles = initialObstacles,
            collectibles = initialCoins
        )
    }

    fun switchLane(currentState: SurferGameState, toRight: Boolean): Pair<SurferGameState, Boolean> {
        if (currentState.status != GameStatus.PLAYING) return Pair(currentState, false)

        val nextLane = if (toRight) currentState.targetLane.right() else currentState.targetLane.left()
        if (nextLane == currentState.targetLane) return Pair(currentState, false)

        val roll = if (toRight) 9.0f else -9.0f
        return Pair(
            currentState.copy(
                targetLane = nextLane,
                cameraRollDegrees = roll
            ),
            true
        )
    }

    fun activateHoverboard(currentState: SurferGameState): Pair<SurferGameState, Boolean> {
        if (currentState.status != GameStatus.PLAYING) return Pair(currentState, false)
        if (currentState.isHoverboardActive) return Pair(currentState, false)
        if (currentState.hoverboardsInventory <= 0) return Pair(currentState, false)

        val updated = currentState.copy(
            hasHoverboard = true,
            hoverboardRemainingMs = 20_000L,
            hoverboardsInventory = currentState.hoverboardsInventory - 1,
            popups = currentState.popups + ScorePopup(
                id = nextEntityId++,
                text = "🛹 HOVERBOARD!",
                laneIndex = currentState.targetLane.index,
                color = Color(0xFF00E5FF)
            )
        )
        return Pair(updated, true)
    }

    fun jump(currentState: SurferGameState): Pair<SurferGameState, Boolean> {
        if (currentState.status != GameStatus.PLAYING) return Pair(currentState, false)
        if (currentState.isJumping) return Pair(currentState, false)

        return Pair(
            currentState.copy(
                isJumping = true,
                jumpProgress = 0f,
                isSliding = false,
                slideProgress = 0f
            ),
            true
        )
    }

    fun slide(currentState: SurferGameState): Pair<SurferGameState, Boolean> {
        if (currentState.status != GameStatus.PLAYING) return Pair(currentState, false)
        if (currentState.isSliding) return Pair(currentState, false)

        return Pair(
            currentState.copy(
                isSliding = true,
                slideProgress = 0f,
                isJumping = false,
                jumpProgress = 0f
            ),
            true
        )
    }

    fun tick(currentState: SurferGameState, deltaMs: Long): EngineStepResult {
        if (currentState.status != GameStatus.PLAYING) {
            return EngineStepResult(currentState, emptyList())
        }

        val events = mutableListOf<GameEvent>()
        val dtSec = (deltaMs / 1000f).coerceIn(0.001f, 0.05f)

        // 1. Dynamic speed scaling based on distance
        val isDashing = currentState.activePowerUp == CollectibleType.DASH_BOOST
        val baseSpeed = (0.38f + (currentState.distanceMeters / 1500f) * 0.28f).coerceIn(0.38f, 0.72f)
        val currentSpeed = if (isDashing) baseSpeed * 1.6f else baseSpeed

        // 2. Track distance and score
        val movedMeters = currentSpeed * 22f * dtSec
        val newDistance = currentState.distanceMeters + movedMeters
        val scoreIncrement = (movedMeters * 3f * currentState.scoreMultiplier).toInt()
        val newScore = currentState.score + scoreIncrement

        // 3. Lane interpolation (swift snappy glide)
        val targetX = currentState.targetLane.xOffsetFactor
        val diffX = targetX - currentState.lanePositionFloat
        val newLaneFloat = if (abs(diffX) < 0.04f) {
            targetX
        } else {
            currentState.lanePositionFloat + diffX * min(1f, dtSec * 18f)
        }

        // 4. Jump & Slide physics
        var isJumping = currentState.isJumping
        var jumpProgress = currentState.jumpProgress
        if (isJumping) {
            jumpProgress += dtSec / 0.70f // 700ms jump arc
            if (jumpProgress >= 1f) {
                isJumping = false
                jumpProgress = 0f
            }
        }

        var isSliding = currentState.isSliding
        var slideProgress = currentState.slideProgress
        if (isSliding) {
            slideProgress += dtSec / 0.65f // 650ms slide
            if (slideProgress >= 1f) {
                isSliding = false
                slideProgress = 0f
            }
        }

        // 5. Power-up timer countdown
        var activePowerUp = currentState.activePowerUp
        var powerUpRemainingMs = currentState.powerUpRemainingMs
        var powerUpTotalMs = currentState.powerUpTotalMs
        if (activePowerUp != null) {
            powerUpRemainingMs -= deltaMs
            if (powerUpRemainingMs <= 0) {
                activePowerUp = null
                powerUpRemainingMs = 0L
                powerUpTotalMs = 0L
            }
        }

        // 6. Move obstacles & check collisions
        val survivingObstacles = mutableListOf<Obstacle>()
        var hasShield = currentState.hasShield
        var hasHoverboard = currentState.hasHoverboard
        var hoverboardRemainingMs = currentState.hoverboardRemainingMs
        var hoverboardsInventory = currentState.hoverboardsInventory
        var isGameOver = false
        var crashReason: String? = null
        val newParticles = currentState.particles.toMutableList()
        val newPopups = mutableListOf<ScorePopup>()

        if (hasHoverboard) {
            hoverboardRemainingMs -= deltaMs
            if (hoverboardRemainingMs <= 0L) {
                hasHoverboard = false
                hoverboardRemainingMs = 0L
            }
        }

        val isMagnetActive = activePowerUp == CollectibleType.MAGNET

        for (obs in currentState.obstacles) {
            val nextZ = obs.z - (currentSpeed * dtSec)

            // Collision check window: Z around 0.0f (where player stands)
            val inCollisionWindow = nextZ in -0.06f..0.06f
            val laneOverlap = abs(newLaneFloat - obs.lane.xOffsetFactor) < 0.58f

            if (inCollisionWindow && laneOverlap) {
                if (isDashing) {
                    // Dash smash! Destroy obstacle
                    events.add(GameEvent.OBSTACLE_SMASHED)
                    spawnExplosionParticles(newParticles, obs.lane.xOffsetFactor, 0.0f, Color(0xFFFFA000))
                    newPopups.add(ScorePopup(nextEntityId++, "SMASHED!", obs.lane.index, Color(0xFFFF9800)))
                    continue // Removed
                }

                // Check evasion techniques
                val canEvadeJump = obs.type == ObstacleType.LOW_BARRIER && isJumping && jumpProgress in 0.16f..0.84f
                val canEvadeSlide = obs.type == ObstacleType.HIGH_BARRIER && isSliding && slideProgress in 0.12f..0.88f

                if (canEvadeJump || canEvadeSlide) {
                    // Evaded successfully
                    survivingObstacles.add(obs.copy(z = nextZ))
                } else {
                    // Hit!
                    if (hasHoverboard) {
                        hasHoverboard = false
                        hoverboardRemainingMs = 0L
                        events.add(GameEvent.HOVERBOARD_BREAK)
                        spawnExplosionParticles(newParticles, obs.lane.xOffsetFactor, 0.0f, Color(0xFF00E5FF))
                        newPopups.add(ScorePopup(nextEntityId++, "SAVED!", obs.lane.index, Color(0xFF00E5FF)))
                        continue
                    } else if (hasShield) {
                        hasShield = false
                        events.add(GameEvent.SHIELD_BREAK)
                        spawnExplosionParticles(newParticles, obs.lane.xOffsetFactor, 0.0f, Color(0xFF00E5FF))
                        newPopups.add(ScorePopup(nextEntityId++, "SHIELD BROKE", obs.lane.index, Color(0xFF00E5FF)))
                        continue
                    } else {
                        // Crash game over
                        isGameOver = true
                        crashReason = when (obs.type) {
                            ObstacleType.LOW_BARRIER -> "Tripped over hurdle! Jump to clear low barriers."
                            ObstacleType.HIGH_BARRIER -> "Hit overhead duct! Duck to slide underneath."
                            ObstacleType.TALL_TRAIN -> "Crashed into commuter train! Switch lanes to avoid trains."
                        }
                        events.add(GameEvent.CRASH)
                        spawnCrashFeathers(newParticles, newLaneFloat)
                        break
                    }
                }
            } else if (nextZ > -0.2f) {
                survivingObstacles.add(obs.copy(z = nextZ))
            }
        }

        // 7. Move collectibles & pickups
        var newCoins = currentState.coins
        var newScoreAfterPickups = newScore
        val survivingCollectibles = mutableListOf<Collectible>()

        for (col in currentState.collectibles) {
            var colZ = col.z - (currentSpeed * dtSec)
            var colLane = col.lane

            // Magnet attraction
            if (isMagnetActive && col.type == CollectibleType.COIN && colZ in 0.0f..0.60f) {
                colLane = currentState.targetLane
                colZ -= (currentSpeed * dtSec * 0.4f)
            }

            val inPickupWindow = colZ in -0.06f..0.06f
            val laneOverlap = abs(newLaneFloat - colLane.xOffsetFactor) < 0.60f

            if (inPickupWindow && laneOverlap) {
                // If elevated coin, require jumping
                if (col.isElevated && (!isJumping || jumpProgress !in 0.15f..0.85f)) {
                    survivingCollectibles.add(col.copy(z = colZ, lane = colLane))
                    continue
                }

                // Picked up!
                when (col.type) {
                    CollectibleType.COIN -> {
                        newCoins += 1
                        newScoreAfterPickups += 20 * currentState.scoreMultiplier
                        events.add(GameEvent.COIN_PICKUP)
                        spawnCoinSparks(newParticles, colLane.xOffsetFactor)
                        newPopups.add(
                            ScorePopup(
                                id = nextEntityId++,
                                text = "+${20 * currentState.scoreMultiplier}",
                                laneIndex = colLane.index,
                                color = Color(0xFFFFD54F)
                            )
                        )
                    }
                    CollectibleType.MAGNET -> {
                        activePowerUp = CollectibleType.MAGNET
                        powerUpRemainingMs = 8_000L
                        powerUpTotalMs = 8_000L
                        events.add(GameEvent.POWERUP_PICKUP)
                        newPopups.add(ScorePopup(nextEntityId++, "MAGNET!", colLane.index, Color(0xFF00E5FF)))
                    }
                    CollectibleType.DASH_BOOST -> {
                        activePowerUp = CollectibleType.DASH_BOOST
                        powerUpRemainingMs = 6_000L
                        powerUpTotalMs = 6_000L
                        events.add(GameEvent.POWERUP_PICKUP)
                        newPopups.add(ScorePopup(nextEntityId++, "SUPER DASH!", colLane.index, Color(0xFFFF5722)))
                    }
                    CollectibleType.SHIELD -> {
                        hasShield = true
                        events.add(GameEvent.POWERUP_PICKUP)
                        newPopups.add(ScorePopup(nextEntityId++, "SHIELD UP!", colLane.index, Color(0xFF00E676)))
                    }
                    CollectibleType.MULTIPLIER_2X -> {
                        activePowerUp = CollectibleType.MULTIPLIER_2X
                        powerUpRemainingMs = 10_000L
                        powerUpTotalMs = 10_000L
                        events.add(GameEvent.POWERUP_PICKUP)
                        newPopups.add(ScorePopup(nextEntityId++, "2X SCORE!", colLane.index, Color(0xFFFF4081)))
                    }
                    CollectibleType.HOVERBOARD_PICKUP -> {
                        hoverboardsInventory += 1
                        events.add(GameEvent.POWERUP_PICKUP)
                        newPopups.add(ScorePopup(nextEntityId++, "+1 HOVERBOARD", colLane.index, Color(0xFF00E5FF)))
                    }
                }
            } else if (colZ > -0.2f) {
                survivingCollectibles.add(col.copy(z = colZ, lane = colLane))
            }
        }

        // 8. Spawn new track segments ahead
        var currentSpawnZ = nextSpawnZ - (currentSpeed * dtSec)
        while (currentSpawnZ < 2.5f) {
            generateTrackSegment(currentSpawnZ, survivingObstacles, survivingCollectibles, newDistance)
            currentSpawnZ += random.nextFloat() * 0.15f + 0.40f
        }
        nextSpawnZ = currentSpawnZ

        // 9. Update particles & floating popups
        val updatedParticles = updateParticles(newParticles, dtSec)
        val updatedPopups = currentState.popups.mapNotNull { popup ->
            val newAge = popup.ageMs + deltaMs
            if (newAge < popup.maxAgeMs) {
                popup.copy(
                    ageMs = newAge,
                    yOffset = popup.yOffset - dtSec * 35f,
                    alpha = (1f - (newAge.toFloat() / popup.maxAgeMs.toFloat())).coerceIn(0f, 1f)
                )
            } else null
        } + newPopups

        // Decay camera banking roll angle
        val newCameraRoll = currentState.cameraRollDegrees * (1f - min(1f, dtSec * 6.5f))

        // 10. Run cycle animation (accelerates noticeably as player/track speeds up)
        val runCycleRate = 2.4f + (currentSpeed / 0.38f) * 3.8f
        val runCycle = (currentState.runCycleProgress + dtSec * runCycleRate) % 1f
        val trackScroll = (currentState.trackScrollOffset + dtSec * currentSpeed * 5.5f) % 1f

        // Running dust puffs at duck's feet when running on ground
        if (!isJumping && !isSliding && random.nextFloat() < (currentSpeed * 0.55f)) {
            val footX = newLaneFloat + (if (random.nextBoolean()) -0.12f else 0.12f)
            newParticles.add(
                SurferParticle(
                    id = nextEntityId++,
                    x = footX,
                    y = 0.05f,
                    vx = (random.nextFloat() - 0.5f) * 0.8f,
                    vy = -random.nextFloat() * 0.5f,
                    color = Color(0x55CFD8DC),
                    alpha = 0.6f,
                    size = random.nextFloat() * 5f + 4f,
                    life = 0.25f,
                    maxLife = 0.25f
                )
            )
        } else if (isSliding) {
            // Rail sparks when sliding
            newParticles.add(
                SurferParticle(
                    id = nextEntityId++,
                    x = newLaneFloat + (random.nextFloat() - 0.5f) * 0.25f,
                    y = 0.04f,
                    vx = (random.nextFloat() - 0.5f) * 1.6f,
                    vy = -random.nextFloat() * 1.0f - 0.4f,
                    color = Color(0xFFFFD54F),
                    alpha = 0.9f,
                    size = random.nextFloat() * 4f + 3f,
                    life = 0.20f,
                    maxLife = 0.20f
                )
            )
        }

        val finalState = currentState.copy(
            status = if (isGameOver) GameStatus.GAME_OVER else GameStatus.PLAYING,
            currentLane = SurferLane.fromIndex(
                when {
                    newLaneFloat < -0.5f -> 0
                    newLaneFloat > 0.5f -> 2
                    else -> 1
                }
            ),
            lanePositionFloat = newLaneFloat,
            isJumping = isJumping,
            jumpProgress = jumpProgress,
            isSliding = isSliding,
            slideProgress = slideProgress,
            distanceMeters = newDistance,
            score = newScoreAfterPickups,
            coins = newCoins,
            speed = currentSpeed,
            hasShield = hasShield,
            hasHoverboard = hasHoverboard,
            hoverboardRemainingMs = hoverboardRemainingMs,
            hoverboardsInventory = hoverboardsInventory,
            cameraRollDegrees = newCameraRoll,
            activePowerUp = activePowerUp,
            powerUpRemainingMs = powerUpRemainingMs,
            powerUpTotalMs = powerUpTotalMs,
            isInvincible = isDashing,
            obstacles = survivingObstacles,
            collectibles = survivingCollectibles,
            particles = updatedParticles,
            popups = updatedPopups,
            runCycleProgress = runCycle,
            trackScrollOffset = trackScroll,
            lastCrashReason = crashReason ?: currentState.lastCrashReason
        )

        return EngineStepResult(finalState, events)
    }

    private fun generateTrackSegment(
        z: Float,
        obstacles: MutableList<Obstacle>,
        collectibles: MutableList<Collectible>,
        distanceMeters: Float
    ) {
        val pattern = random.nextInt(6)
        when (pattern) {
            0 -> {
                // Single low barrier with parabolic arch of coins above (jump challenge)
                val lane = SurferLane.fromIndex(random.nextInt(3))
                obstacles.add(Obstacle(nextEntityId++, lane, ObstacleType.LOW_BARRIER, z))
                collectibles.add(Collectible(nextEntityId++, lane, CollectibleType.COIN, z, isElevated = true))
                collectibles.add(Collectible(nextEntityId++, lane, CollectibleType.COIN, z - 0.08f, isElevated = false))
                collectibles.add(Collectible(nextEntityId++, lane, CollectibleType.COIN, z + 0.08f, isElevated = false))
            }
            1 -> {
                // Overhead clearance barrier with low sliding coins
                val lane = SurferLane.fromIndex(random.nextInt(3))
                obstacles.add(Obstacle(nextEntityId++, lane, ObstacleType.HIGH_BARRIER, z))
                collectibles.add(Collectible(nextEntityId++, lane, CollectibleType.COIN, z, isElevated = false))
                collectibles.add(Collectible(nextEntityId++, lane, CollectibleType.COIN, z + 0.07f, isElevated = false))
            }
            2 -> {
                // Realistic commuter passenger train car on one lane
                val trainLane = SurferLane.fromIndex(random.nextInt(3))
                val coinLane = SurferLane.fromIndex((trainLane.index + 1) % 3)
                val hasRamp = random.nextFloat() < 0.35f
                obstacles.add(Obstacle(nextEntityId++, trainLane, ObstacleType.TALL_TRAIN, z, trainLength = 0.50f, hasRamp = hasRamp))
                repeat(4) { i ->
                    collectibles.add(
                        Collectible(nextEntityId++, coinLane, CollectibleType.COIN, z + (i * 0.06f), isElevated = false)
                    )
                }
            }
            3 -> {
                // Double obstacle with 1 guaranteed open escape lane
                val openLaneIndex = random.nextInt(3)
                for (i in 0..2) {
                    if (i != openLaneIndex) {
                        val type = if (random.nextBoolean()) ObstacleType.TALL_TRAIN else ObstacleType.LOW_BARRIER
                        obstacles.add(Obstacle(nextEntityId++, SurferLane.fromIndex(i), type, z, trainLength = 0.45f))
                    }
                }
                // Reward on open lane
                val rewardType = checkPowerUpSpawn(distanceMeters)
                collectibles.add(Collectible(nextEntityId++, SurferLane.fromIndex(openLaneIndex), rewardType, z))
            }
            4 -> {
                // Long ribbon of gold coins along a lane
                val lane = SurferLane.fromIndex(random.nextInt(3))
                repeat(5) { i ->
                    collectibles.add(
                        Collectible(nextEntityId++, lane, CollectibleType.COIN, z + (i * 0.07f), isElevated = false)
                    )
                }
            }
            5 -> {
                // Power-up or hoverboard crate
                val lane = SurferLane.fromIndex(random.nextInt(3))
                val powerUp = checkPowerUpSpawn(distanceMeters)
                collectibles.add(Collectible(nextEntityId++, lane, powerUp, z))
            }
        }
    }

    private fun checkPowerUpSpawn(distanceMeters: Float): CollectibleType {
        if (distanceMeters >= nextPowerUpDistance) {
            nextPowerUpDistance = distanceMeters + random.nextInt(35, 60)
            return when (random.nextInt(5)) {
                0 -> CollectibleType.MAGNET
                1 -> CollectibleType.DASH_BOOST
                2 -> CollectibleType.SHIELD
                3 -> CollectibleType.HOVERBOARD_PICKUP
                else -> CollectibleType.MULTIPLIER_2X
            }
        }
        return CollectibleType.COIN
    }

    private fun spawnCoinSparks(particles: MutableList<SurferParticle>, laneX: Float) {
        repeat(8) {
            val angle = random.nextFloat() * 2 * PI.toFloat()
            val speed = random.nextFloat() * 1.8f + 0.8f
            particles.add(
                SurferParticle(
                    id = nextEntityId++,
                    x = laneX,
                    y = 0.2f,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - 1.2f,
                    color = Color(0xFFFFD700),
                    alpha = 1.0f,
                    size = random.nextFloat() * 5f + 4f,
                    life = 0.40f,
                    maxLife = 0.40f
                )
            )
        }
    }

    private fun spawnExplosionParticles(
        particles: MutableList<SurferParticle>,
        laneX: Float,
        y: Float,
        color: Color
    ) {
        repeat(16) {
            val angle = random.nextFloat() * 2 * PI.toFloat()
            val speed = random.nextFloat() * 3.5f + 1.2f
            particles.add(
                SurferParticle(
                    id = nextEntityId++,
                    x = laneX,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - 2.0f,
                    color = color,
                    alpha = 1.0f,
                    size = random.nextFloat() * 6f + 4f,
                    life = 0.55f,
                    maxLife = 0.55f
                )
            )
        }
    }

    private fun spawnCrashFeathers(particles: MutableList<SurferParticle>, playerX: Float) {
        repeat(24) {
            val angle = random.nextFloat() * 2 * PI.toFloat()
            val speed = random.nextFloat() * 3.0f + 0.8f
            particles.add(
                SurferParticle(
                    id = nextEntityId++,
                    x = playerX,
                    y = 0.1f,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - 2.5f,
                    color = if (random.nextBoolean()) Color(0xFFF2F2F2) else Color(0xFFFFA000),
                    alpha = 1.0f,
                    size = random.nextFloat() * 8f + 5f,
                    life = 0.70f,
                    maxLife = 0.70f
                )
            )
        }
    }

    private fun updateParticles(
        particles: List<SurferParticle>,
        dtSec: Float
    ): List<SurferParticle> {
        val updated = mutableListOf<SurferParticle>()
        for (p in particles) {
            val newLife = p.life - dtSec
            if (newLife > 0) {
                val newX = p.x + p.vx * dtSec
                val newY = p.y + p.vy * dtSec + (4.0f * dtSec * dtSec) // subtle gravity
                val newAlpha = (newLife / p.maxLife).coerceIn(0f, 1f)
                updated.add(p.copy(x = newX, y = newY, life = newLife, alpha = newAlpha))
            }
        }
        return updated
    }
}
