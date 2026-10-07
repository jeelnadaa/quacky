package app.quacky.feature.surfer.domain

import app.quacky.feature.surfer.model.Collectible
import app.quacky.feature.surfer.model.CollectibleType
import app.quacky.feature.surfer.model.GameStatus
import app.quacky.feature.surfer.model.Obstacle
import app.quacky.feature.surfer.model.ObstacleType
import app.quacky.feature.surfer.model.SurferGameState
import app.quacky.feature.surfer.model.SurferLane
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class SurferEngineTest {

    private lateinit var engine: SurferEngine

    @Before
    fun setUp() {
        engine = SurferEngine(Random(12345))
    }

    @Test
    fun `startNewGame initializes playing state in center lane`() {
        val state = engine.startNewGame(highScore = 500, totalCoins = 42)
        assertEquals(GameStatus.PLAYING, state.status)
        assertEquals(SurferLane.CENTER, state.currentLane)
        assertEquals(SurferLane.CENTER, state.targetLane)
        assertEquals(500, state.highScore)
        assertEquals(42, state.totalCoins)
        assertEquals(0, state.coins)
        assertEquals(0, state.score)
        assertTrue("Should have initial obstacles generated", state.obstacles.isNotEmpty())
        assertTrue("Should have initial collectibles generated", state.collectibles.isNotEmpty())
    }

    @Test
    fun `switchLane boundaries are respected`() {
        val initial = engine.startNewGame(0, 0)
        assertEquals(SurferLane.CENTER, initial.targetLane)

        // Center -> Left
        val (stateLeft, changed1) = engine.switchLane(initial, toRight = false)
        assertTrue(changed1)
        assertEquals(SurferLane.LEFT, stateLeft.targetLane)

        // Left -> Left (clamped)
        val (stateLeftAgain, changed2) = engine.switchLane(stateLeft, toRight = false)
        assertFalse(changed2)
        assertEquals(SurferLane.LEFT, stateLeftAgain.targetLane)

        // Left -> Center
        val (stateBackCenter, changed3) = engine.switchLane(stateLeft, toRight = true)
        assertTrue(changed3)
        assertEquals(SurferLane.CENTER, stateBackCenter.targetLane)

        // Center -> Right
        val (stateRight, changed4) = engine.switchLane(stateBackCenter, toRight = true)
        assertTrue(changed4)
        assertEquals(SurferLane.RIGHT, stateRight.targetLane)

        // Right -> Right (clamped)
        val (stateRightAgain, changed5) = engine.switchLane(stateRight, toRight = true)
        assertFalse(changed5)
        assertEquals(SurferLane.RIGHT, stateRightAgain.targetLane)
    }

    @Test
    fun `jump cancels slide and initiates jump progress`() {
        var state = engine.startNewGame(0, 0)
        val (slided, _) = engine.slide(state)
        assertTrue(slided.isSliding)

        val (jumped, _) = engine.jump(slided)
        assertTrue(jumped.isJumping)
        assertFalse(jumped.isSliding)
        assertEquals(0f, jumped.jumpProgress, 0.001f)
    }

    @Test
    fun `slide cancels jump and initiates slide progress`() {
        var state = engine.startNewGame(0, 0)
        val (jumped, _) = engine.jump(state)
        assertTrue(jumped.isJumping)

        val (slided, _) = engine.slide(jumped)
        assertTrue(slided.isSliding)
        assertFalse(slided.isJumping)
        assertEquals(0f, slided.slideProgress, 0.001f)
    }

    @Test
    fun `jumping avoids low barrier collision`() {
        val obstacle = Obstacle(id = 1L, lane = SurferLane.CENTER, type = ObstacleType.LOW_BARRIER, z = 0.03f)
        val state = SurferGameState(
            status = GameStatus.PLAYING,
            currentLane = SurferLane.CENTER,
            targetLane = SurferLane.CENTER,
            lanePositionFloat = 0.0f,
            isJumping = true,
            jumpProgress = 0.5f, // Mid-air
            obstacles = listOf(obstacle)
        )

        val result = engine.tick(state, 16L)
        assertEquals(GameStatus.PLAYING, result.state.status)
        assertFalse(result.events.contains(SurferEngine.GameEvent.CRASH))
    }

    @Test
    fun `sliding avoids high barrier collision`() {
        val obstacle = Obstacle(id = 1L, lane = SurferLane.CENTER, type = ObstacleType.HIGH_BARRIER, z = 0.03f)
        val state = SurferGameState(
            status = GameStatus.PLAYING,
            currentLane = SurferLane.CENTER,
            targetLane = SurferLane.CENTER,
            lanePositionFloat = 0.0f,
            isSliding = true,
            slideProgress = 0.4f, // Crouched
            obstacles = listOf(obstacle)
        )

        val result = engine.tick(state, 16L)
        assertEquals(GameStatus.PLAYING, result.state.status)
        assertFalse(result.events.contains(SurferEngine.GameEvent.CRASH))
    }

    @Test
    fun `standing still on low barrier results in crash`() {
        val obstacle = Obstacle(id = 1L, lane = SurferLane.CENTER, type = ObstacleType.LOW_BARRIER, z = 0.02f)
        val state = SurferGameState(
            status = GameStatus.PLAYING,
            currentLane = SurferLane.CENTER,
            targetLane = SurferLane.CENTER,
            lanePositionFloat = 0.0f,
            isJumping = false,
            isSliding = false,
            obstacles = listOf(obstacle)
        )

        val result = engine.tick(state, 16L)
        assertEquals(GameStatus.GAME_OVER, result.state.status)
        assertTrue(result.events.contains(SurferEngine.GameEvent.CRASH))
    }

    @Test
    fun `shield absorbs collision and preserves playing state`() {
        val obstacle = Obstacle(id = 1L, lane = SurferLane.CENTER, type = ObstacleType.TALL_TRAIN, z = 0.02f)
        val state = SurferGameState(
            status = GameStatus.PLAYING,
            currentLane = SurferLane.CENTER,
            targetLane = SurferLane.CENTER,
            lanePositionFloat = 0.0f,
            hasShield = true,
            obstacles = listOf(obstacle)
        )

        val result = engine.tick(state, 16L)
        assertEquals(GameStatus.PLAYING, result.state.status)
        assertFalse(result.state.hasShield)
        assertTrue(result.events.contains(SurferEngine.GameEvent.SHIELD_BREAK))
    }

    @Test
    fun `dash boost destroys obstacle`() {
        val obstacle = Obstacle(id = 1L, lane = SurferLane.CENTER, type = ObstacleType.TALL_TRAIN, z = 0.02f)
        val state = SurferGameState(
            status = GameStatus.PLAYING,
            currentLane = SurferLane.CENTER,
            targetLane = SurferLane.CENTER,
            lanePositionFloat = 0.0f,
            activePowerUp = CollectibleType.DASH_BOOST,
            powerUpRemainingMs = 4000L,
            obstacles = listOf(obstacle)
        )

        val result = engine.tick(state, 16L)
        assertEquals(GameStatus.PLAYING, result.state.status)
        assertTrue(result.events.contains(SurferEngine.GameEvent.OBSTACLE_SMASHED))
    }

    @Test
    fun `coin pickup increments coin count and score`() {
        val coin = Collectible(id = 1L, lane = SurferLane.CENTER, type = CollectibleType.COIN, z = 0.01f)
        val state = SurferGameState(
            status = GameStatus.PLAYING,
            currentLane = SurferLane.CENTER,
            targetLane = SurferLane.CENTER,
            lanePositionFloat = 0.0f,
            coins = 5,
            score = 100,
            collectibles = listOf(coin)
        )

        val result = engine.tick(state, 16L)
        assertEquals(6, result.state.coins)
        assertTrue(result.state.score > 100)
        assertTrue(result.events.contains(SurferEngine.GameEvent.COIN_PICKUP))
    }
}
