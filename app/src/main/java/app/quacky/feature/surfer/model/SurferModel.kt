package app.quacky.feature.surfer.model

import androidx.compose.ui.graphics.Color

enum class SurferLane(val index: Int, val xOffsetFactor: Float) {
    LEFT(0, -1.0f),
    CENTER(1, 0.0f),
    RIGHT(2, 1.0f);

    fun left(): SurferLane = when (this) {
        RIGHT -> CENTER
        CENTER -> LEFT
        LEFT -> LEFT
    }

    fun right(): SurferLane = when (this) {
        LEFT -> CENTER
        CENTER -> RIGHT
        RIGHT -> RIGHT
    }

    companion object {
        fun fromIndex(index: Int): SurferLane = when (index) {
            0 -> LEFT
            2 -> RIGHT
            else -> CENTER
        }
    }
}

enum class ObstacleType {
    LOW_BARRIER,   // Ground construction roadblock: player must JUMP or dodge
    HIGH_BARRIER,  // Overhead pipe/gantry beam: player must SLIDE/DUCK or dodge
    TALL_TRAIN     // Realistic commuter train carriage: solid obstacle, player must CHANGE LANES
}

data class Obstacle(
    val id: Long,
    val lane: SurferLane,
    val type: ObstacleType,
    val z: Float, // 1.0f at horizon, 0.0f at player, <0 behind player
    val trainLength: Float = 0.45f,
    val hasRamp: Boolean = false
)

enum class CollectibleType {
    COIN,               // Golden coin: +20 pts, +1 coin
    MAGNET,             // Pulls all nearby coins towards player (8s)
    DASH_BOOST,         // Invincible supersonic quack dash destroying obstacles (6s)
    SHIELD,             // Absorbs 1 collision
    MULTIPLIER_2X,      // 2x score multiplier (10s)
    HOVERBOARD_PICKUP   // +1 Hoverboard inventory
}

data class Collectible(
    val id: Long,
    val lane: SurferLane,
    val type: CollectibleType,
    val z: Float,
    val isElevated: Boolean = false // If true, requires jumping to collect
)

enum class GameStatus {
    READY,
    PLAYING,
    PAUSED,
    GAME_OVER
}

data class SurferParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val alpha: Float,
    val size: Float,
    val life: Float,
    val maxLife: Float
)

data class ScorePopup(
    val id: Long,
    val text: String,
    val laneIndex: Int, // 0 = Left, 1 = Center, 2 = Right
    val color: Color,
    val yOffset: Float = 0f,
    val alpha: Float = 1.0f,
    val ageMs: Long = 0L,
    val maxAgeMs: Long = 850L
)

data class SurferGameState(
    val status: GameStatus = GameStatus.READY,
    val currentLane: SurferLane = SurferLane.CENTER,
    val targetLane: SurferLane = SurferLane.CENTER,
    val lanePositionFloat: Float = 0.0f, // Interpolated between -1.0f and 1.0f
    val isJumping: Boolean = false,
    val jumpProgress: Float = 0f, // 0.0f to 1.0f parabolic jump
    val isSliding: Boolean = false,
    val slideProgress: Float = 0f, // 0.0f to 1.0f crouch/duck
    val distanceMeters: Float = 0f,
    val score: Int = 0,
    val coins: Int = 0,
    val speed: Float = 0.38f, // Base track speed (units of Z per second)
    val hasShield: Boolean = false,
    val activePowerUp: CollectibleType? = null,
    val powerUpRemainingMs: Long = 0L,
    val powerUpTotalMs: Long = 0L,
    val isInvincible: Boolean = false,
    val hasHoverboard: Boolean = false,
    val hoverboardRemainingMs: Long = 0L,
    val hoverboardTotalMs: Long = 20_000L,
    val hoverboardsInventory: Int = 3,
    val cameraRollDegrees: Float = 0f,
    val isNewHighScore: Boolean = false,
    val highScore: Int = 0,
    val totalCoins: Int = 0,
    val obstacles: List<Obstacle> = emptyList(),
    val collectibles: List<Collectible> = emptyList(),
    val particles: List<SurferParticle> = emptyList(),
    val popups: List<ScorePopup> = emptyList(),
    val duckBlink: Float = 0f,
    val runCycleProgress: Float = 0f,
    val trackScrollOffset: Float = 0f,
    val lastCrashReason: String? = null
) {
    val powerUpFraction: Float
        get() = if (powerUpTotalMs > 0) (powerUpRemainingMs.toFloat() / powerUpTotalMs.toFloat()).coerceIn(0f, 1f) else 0f

    val hoverboardFraction: Float
        get() = if (hoverboardTotalMs > 0) (hoverboardRemainingMs.toFloat() / hoverboardTotalMs.toFloat()).coerceIn(0f, 1f) else 0f

    val isHoverboardActive: Boolean
        get() = hasHoverboard && hoverboardRemainingMs > 0L

    val scoreMultiplier: Int
        get() = if (activePowerUp == CollectibleType.MULTIPLIER_2X) 2 else 1
}
