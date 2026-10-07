package app.quacky.feature.surfer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlayerControllerTest {

    private lateinit var controller: PlayerController

    @Before
    fun setUp() {
        controller = PlayerController()
    }

    @Test
    fun `initial state is idle ready in center lane`() {
        assertEquals(PlayerController.State.IDLE_READY, controller.state)
        assertEquals(1, controller.currentLane)
        assertEquals(0.0f, controller.x, 0.001f)
        assertEquals(0.0f, controller.y, 0.001f)
    }

    @Test
    fun `lane change smoothstep moves towards lane coordinate`() {
        controller.startRunning()
        controller.requestMoveRight()
        assertEquals(2, controller.targetLane)

        // Advance half duration (0.06s of 0.12s)
        controller.update(0.06f)
        assertTrue("X position should be between 0 and 1.6", controller.x > 0.0f && controller.x < 1.6f)

        // Advance to completion
        controller.update(0.07f)
        assertEquals(1.6f, controller.x, 0.001f)
        assertEquals(2, controller.currentLane)
    }

    @Test
    fun `jump reaches apex and lands back on ground`() {
        controller.startRunning()
        controller.requestJump()
        assertEquals(PlayerController.State.JUMPING, controller.state)

        var maxHeight = 0.0f
        var totalTime = 0.0f
        val dt = 1.0f / 120.0f

        while (controller.state == PlayerController.State.JUMPING && totalTime < 1.5f) {
            controller.update(dt)
            totalTime += dt
            if (controller.y > maxHeight) maxHeight = controller.y
        }

        assertEquals(PlayerController.State.GROUNDED_RUN, controller.state)
        assertEquals(1.61f, maxHeight, 0.05f)
        assertEquals(0.68f, totalTime, 0.05f)
        assertEquals(0.0f, controller.y, 0.001f)
    }

    @Test
    fun `slide activates and returns to run after duration`() {
        controller.startRunning()
        controller.requestSlide()
        assertEquals(PlayerController.State.SLIDING, controller.state)
        assertTrue(controller.isSliding)

        controller.update(0.3f)
        assertTrue(controller.isSliding)

        controller.update(0.4f)
        assertFalse(controller.isSliding)
        assertEquals(PlayerController.State.GROUNDED_RUN, controller.state)
    }
}
