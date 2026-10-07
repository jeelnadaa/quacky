package app.quacky.feature.surfer.domain

import kotlin.math.max
import kotlin.math.min

/**
 * Manages player state machine, physics (jump, fast-fall, slide),
 * lane changes with smoothstep interpolation, and input buffering.
 */
class PlayerController {

    enum class State {
        IDLE_READY,
        GROUNDED_RUN,
        JUMPING,
        SLIDING,
        CRASHED
    }

    var state: State = State.IDLE_READY
        private set

    var currentLane: Int = 1 // 0 = Left (-1.6), 1 = Center (0.0), 2 = Right (+1.6)
        private set

    var targetLane: Int = 1
        private set

    var x: Float = 0.0f
        private set

    var y: Float = 0.0f
        private set

    var vy: Float = 0.0f
        private set

    var laneChangeTimer: Float = 0.0f
        private set

    private var startX: Float = 0.0f
    private var bufferedLaneInput: Int? = null

    var slideTimer: Float = 0.0f
        private set

    var activeLean: String? = null // "lean_left" or "lean_right"
        private set

    var crashTimer: Float = 0.0f
        private set

    val isSliding: Boolean
        get() = state == State.SLIDING

    val isJumping: Boolean
        get() = state == State.JUMPING

    val isCrashed: Boolean
        get() = state == State.CRASHED

    fun reset() {
        state = State.IDLE_READY
        currentLane = 1
        targetLane = 1
        x = 0.0f
        y = 0.0f
        vy = 0.0f
        laneChangeTimer = 0.0f
        startX = 0.0f
        bufferedLaneInput = null
        slideTimer = 0.0f
        activeLean = null
        crashTimer = 0.0f
    }

    fun startRunning() {
        state = State.GROUNDED_RUN
        y = 0.0f
        vy = 0.0f
    }

    var currentSpeed: Float = SurferTuning.SPEED_MIN

    fun requestMoveLeft() {
        if (state == State.CRASHED) return
        requestLaneChange(-1)
    }

    fun requestMoveRight() {
        if (state == State.CRASHED) return
        requestLaneChange(1)
    }

    private fun requestLaneChange(delta: Int) {
        val newTarget = (targetLane + delta).coerceIn(0, 2)
        if (newTarget == targetLane) return

        val laneDuration = SurferTuning.calculateLaneChangeDuration(currentSpeed)
        if (laneChangeTimer > 0f) {
            val progress = 1.0f - (laneChangeTimer / laneDuration)
            if (progress >= SurferTuning.LANE_BUFFER_THRESHOLD) {
                bufferedLaneInput = newTarget
            }
            return
        }

        startLaneTransition(newTarget)
    }

    private fun startLaneTransition(newTarget: Int) {
        startX = x
        currentLane = targetLane
        targetLane = newTarget
        laneChangeTimer = SurferTuning.calculateLaneChangeDuration(currentSpeed)
        activeLean = if (newTarget < currentLane) "lean_left" else "lean_right"
    }

    fun requestJump() {
        if (state == State.CRASHED) return
        val (v0, _) = SurferTuning.calculateJumpPhysics(currentSpeed)
        if (state == State.GROUNDED_RUN) {
            state = State.JUMPING
            vy = v0
        } else if (state == State.SLIDING) {
            // Cancel slide and jump
            state = State.JUMPING
            vy = v0
            slideTimer = 0.0f
        }
    }

    fun requestSlide() {
        if (state == State.CRASHED) return
        if (state == State.JUMPING) {
            // Fast fall slam
            vy = SurferTuning.FAST_FALL_VY
        } else if (state == State.GROUNDED_RUN || state == State.SLIDING) {
            state = State.SLIDING
            slideTimer = SurferTuning.calculateSlideDuration(currentSpeed)
        }
    }

    fun triggerCrash() {
        state = State.CRASHED
        crashTimer = 0.0f
    }

    fun update(dt: Float, speed: Float = currentSpeed) {
        currentSpeed = speed
        if (state == State.CRASHED) {
            crashTimer += dt
            return
        }

        val laneDuration = SurferTuning.calculateLaneChangeDuration(currentSpeed)

        // Lane change smoothstep
        if (laneChangeTimer > 0f) {
            laneChangeTimer = max(0.0f, laneChangeTimer - dt)
            val t = 1.0f - (laneChangeTimer / laneDuration)
            val smoothT = t * t * (3.0f - 2.0f * t)
            val destX = SurferTuning.LANES[targetLane]
            x = startX + (destX - startX) * smoothT

            if (laneChangeTimer == 0f) {
                currentLane = targetLane
                x = destX
                activeLean = null

                bufferedLaneInput?.let { nextTarget ->
                    bufferedLaneInput = null
                    startLaneTransition(nextTarget)
                }
            }
        } else {
            x = SurferTuning.LANES[targetLane]
        }

        // Vertical physics
        if (state == State.JUMPING) {
            val (_, g) = SurferTuning.calculateJumpPhysics(currentSpeed)
            vy -= g * dt
            y += vy * dt

            if (y <= 0.0f) {
                y = 0.0f
                vy = 0.0f
                // If fast fell, enter slide on landing
                state = if (slideTimer > 0f) State.SLIDING else State.GROUNDED_RUN
            }
        } else {
            y = 0.0f
        }

        // Slide timer
        if (state == State.SLIDING) {
            slideTimer = max(0.0f, slideTimer - dt)
            if (slideTimer <= 0.0f) {
                state = State.GROUNDED_RUN
            }
        }
    }
}
