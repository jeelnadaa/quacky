package app.quacky.feature.surfer.domain

import kotlin.math.max
import kotlin.math.min

/**
 * Single source of truth for all Quacky Surfer 3D gameplay numbers, physics,
 * dimensions, collider bounds, and timing rules as specified in the 3D rebuild specification.
 */
object SurferTuning {
    // -------------------------------------------------------------------------
    // World & Lanes
    // -------------------------------------------------------------------------
    val LANES = floatArrayOf(-1.6f, 0.0f, 1.6f)
    const val LANE_WIDTH = 1.6f
    const val TRACK_HALF_WIDTH = 2.4f

    // -------------------------------------------------------------------------
    // Speed Progression
    // -------------------------------------------------------------------------
    const val SPEED_MIN = 10.0f
    const val SPEED_MAX = 22.0f
    const val SPEED_ACCEL = 0.07f

    fun calculateSpeed(elapsedSeconds: Float): Float =
        min(SPEED_MAX, SPEED_MIN + SPEED_ACCEL * elapsedSeconds)

    // -------------------------------------------------------------------------
    // Player Movement & Physics
    // -------------------------------------------------------------------------
    const val LANE_CHANGE_DURATION = 0.12f // 120 ms smoothstep
    const val LANE_BUFFER_THRESHOLD = 0.60f // Can buffer next lane input after 60% done

    const val JUMP_V0 = 9.5f       // m/s
    const val GRAVITY = 28.0f      // m/s² (apex ≈ 1.61 m, air time ≈ 0.68 s)
    const val FAST_FALL_VY = -16.0f // m/s slam on swipe down in air

    const val SLIDE_DURATION = 0.65f // seconds
    const val SLIDE_CANCEL_JUMP_DELAY = 0.080f // 80 ms delay if swiping up during slide

    const val INPUT_BUFFER_WINDOW_MS = 120L // Input buffer memory window

    // -------------------------------------------------------------------------
    // Camera
    // -------------------------------------------------------------------------
    const val CAM_SPRING_OMEGA = 10.0f // critically-damped spring frequency
    const val CAM_Y = 2.55f
    const val CAM_Z = 4.3f
    const val CAM_LOOK_Y = 0.9f
    const val CAM_LOOK_Z = -7.0f

    const val FOV_MIN = 58.0f // vertical FOV at speed 10 m/s
    const val FOV_MAX = 66.0f // vertical FOV at speed 22 m/s

    const val READY_CAM_Y = 2.0f
    const val READY_CAM_Z = 3.4f

    const val CRASH_SHAKE_DURATION = 0.25f
    const val CRASH_SHAKE_AMPLITUDE = 0.06f

    // -------------------------------------------------------------------------
    // Chunks & Spawning
    // -------------------------------------------------------------------------
    const val CHUNK_LENGTH = 24.0f
    const val CHUNK_COUNT = 8
    const val SPAWN_HORIZON = 110.0f // generate rows up to 110 m ahead
    const val DESPAWN_BEHIND_Z = 8.0f // cull objects > 8 m behind camera

    fun calculateRowGap(speed: Float): Float =
        max(7.0f, speed * 0.55f)

    // -------------------------------------------------------------------------
    // Colliders (Forgiving AABB bounds relative to origin)
    // -------------------------------------------------------------------------
    object Colliders {
        // Player standing / jumping
        const val PLAYER_HALF_WIDTH = 0.28f
        const val PLAYER_STANDING_HEIGHT = 0.95f
        const val PLAYER_HALF_DEPTH = 0.25f

        // Player sliding
        const val PLAYER_SLIDING_HEIGHT = 0.62f

        // Train (x ±0.70, y 0..2.80, z from -11.0 to 0.0)
        const val TRAIN_HALF_WIDTH = 0.70f
        const val TRAIN_HEIGHT = 2.80f
        const val TRAIN_LENGTH = 11.0f

        // Low Hazard Barrier (x ±0.75, y 0..0.80, z ±0.16)
        const val BARRIER_HALF_WIDTH = 0.75f
        const val BARRIER_HEIGHT = 0.80f
        const val BARRIER_HALF_DEPTH = 0.16f

        // Overhead Duct (bar: x ±0.92, y 0.82..1.32, z ±0.28; posts: x 0.80..0.92, y 0..1.74)
        const val DUCT_BAR_HALF_WIDTH = 0.92f
        const val DUCT_BAR_MIN_Y = 0.82f
        const val DUCT_BAR_MAX_Y = 1.32f
        const val DUCT_BAR_HALF_DEPTH = 0.28f
        const val DUCT_POST_INNER_X = 0.80f
        const val DUCT_POST_OUTER_X = 0.92f
        const val DUCT_POST_HEIGHT = 1.74f

        // Breadcrumb sphere radius
        const val BREADCRUMB_RADIUS = 0.38f
        const val BREADCRUMB_GROUND_Y = 0.75f
        const val BREADCRUMB_HIGH_Y = 1.45f
    }

    // -------------------------------------------------------------------------
    // Scoring & Economy
    // -------------------------------------------------------------------------
    const val BREADCRUMB_SCORE_MULTIPLIER = 20

    fun calculateScore(distanceMeters: Float, breadcrumbs: Int): Int =
        distanceMeters.toInt() + (breadcrumbs * BREADCRUMB_SCORE_MULTIPLIER)
}
