package app.quacky.feature.arruler.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveMeasurementControllerTest {

    private val identityMatrix = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    private val validTrackingStatus = TrackingStatus(
        isTracking = true,
        linearSpeed = 0.05,
        angularSpeed = 0.02,
        lateralParallaxMeters = 0.20,
        parallaxProgress = 1.0f,
        isParallaxComplete = true,
        isMovingTooFast = false,
        currentHint = null,
        canPlacePoint = true
    )

    @Test
    fun `live line follows aim point and matches distance`() {
        val controller = LiveMeasurementController()
        val dot1 = Vec3(0.0, 0.0, -1.0)
        controller.addPoint(dot1)

        val aimPoint = Vec3(1.0, 0.0, -1.0) // 1.0m away horizontally
        val hit = SurfaceHit(
            worldPoint = aimPoint,
            normal = Vec3.UP,
            kind = SurfaceKind.WALL,
            source = HitSource.FUSED,
            distanceFromCamera = 1.0f,
            confidence = 0.85f
        )

        val frameState = controller.updateFrame(
            mode = MeasurementMode.DISTANCE,
            unit = RulerUnit.METERS,
            scaleFactor = 1.0,
            viewMatrix = identityMatrix,
            projMatrix = identityMatrix,
            cameraPosition = Vec3.ZERO,
            screenWidth = 1080f,
            screenHeight = 1920f,
            currentHit = hit,
            reticleState = ReticleState.LOCKED,
            trackingStatus = validTrackingStatus,
            snappedPoint = null,
            snapLabel = null,
            timestampMs = 1000L
        )

        assertNotNull(frameState.liveSegment)
        assertEquals(1.0, frameState.liveSegment!!.lengthMeters, 1e-3)
        assertEquals("Level", frameState.levelPlumbHint)
        assertTrue(frameState.canPlace)
    }

    @Test
    fun `path total sums placed segments plus live segment`() {
        val controller = LiveMeasurementController()
        controller.addPoint(Vec3(0.0, 0.0, -1.0))
        controller.addPoint(Vec3(2.0, 0.0, -1.0)) // 2.0m placed

        val aimPoint = Vec3(2.0, 1.0, -1.0) // 1.0m live
        val hit = SurfaceHit(
            worldPoint = aimPoint,
            normal = Vec3.UP,
            kind = SurfaceKind.WALL,
            source = HitSource.PLANE,
            distanceFromCamera = 2.0f,
            confidence = 0.8f
        )

        val state = controller.updateFrame(
            mode = MeasurementMode.PATH,
            unit = RulerUnit.METERS,
            scaleFactor = 1.0,
            viewMatrix = identityMatrix,
            projMatrix = identityMatrix,
            cameraPosition = Vec3.ZERO,
            screenWidth = 1080f,
            screenHeight = 1920f,
            currentHit = hit,
            reticleState = ReticleState.TRACKING,
            trackingStatus = validTrackingStatus,
            snappedPoint = null,
            snapLabel = null,
            timestampMs = 1000L
        )

        assertEquals(1, state.solidSegments.size)
        assertEquals(2.0, state.solidSegments[0].lengthMeters, 1e-3)
        assertEquals(1.0, state.liveSegment?.lengthMeters ?: 0.0, 1e-3)
        // Total should reflect 3.0m
        assertTrue(state.primaryReadoutText.contains("3.000 m"))
    }

    @Test
    fun `stale surface hides live readout after 500ms without valid hit`() {
        val controller = LiveMeasurementController()
        controller.addPoint(Vec3(0.0, 0.0, -1.0))

        // Initial valid hit at t=1000
        val hit = SurfaceHit(
            worldPoint = Vec3(1.0, 0.0, -1.0),
            normal = Vec3.UP,
            kind = SurfaceKind.WALL,
            source = HitSource.FUSED,
            distanceFromCamera = 1.0f,
            confidence = 0.8f
        )
        controller.updateFrame(
            mode = MeasurementMode.DISTANCE,
            unit = RulerUnit.METERS,
            scaleFactor = 1.0,
            viewMatrix = identityMatrix,
            projMatrix = identityMatrix,
            cameraPosition = Vec3.ZERO,
            screenWidth = 1080f,
            screenHeight = 1920f,
            currentHit = hit,
            reticleState = ReticleState.TRACKING,
            trackingStatus = validTrackingStatus,
            snappedPoint = null,
            snapLabel = null,
            timestampMs = 1000L
        )

        // 600ms later with null hit (t=1600)
        val staleState = controller.updateFrame(
            mode = MeasurementMode.DISTANCE,
            unit = RulerUnit.METERS,
            scaleFactor = 1.0,
            viewMatrix = identityMatrix,
            projMatrix = identityMatrix,
            cameraPosition = Vec3.ZERO,
            screenWidth = 1080f,
            screenHeight = 1920f,
            currentHit = null,
            reticleState = ReticleState.SEARCHING,
            trackingStatus = validTrackingStatus,
            snappedPoint = null,
            snapLabel = null,
            timestampMs = 1600L
        )

        assertFalse(staleState.canPlace)
        assertEquals("Aim at a surface", staleState.primaryReadoutText)
    }

    @Test
    fun `undo removes last dot and moves live line start to previous dot`() {
        val controller = LiveMeasurementController()
        controller.addPoint(Vec3(0.0, 0.0, -1.0))
        controller.addPoint(Vec3(1.0, 0.0, -1.0))
        assertEquals(2, controller.placedCount)

        val removed = controller.removeLastPoint()
        assertTrue(removed)
        assertEquals(1, controller.placedCount)
        assertEquals(Vec3(0.0, 0.0, -1.0), controller.currentPoints[0])
    }
}
