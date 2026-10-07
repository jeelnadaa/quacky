package app.quacky.feature.arruler.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointStabilizerTest {

    @Test
    fun `stabilizer transitions to LOCKED when samples are consistent for 300ms`() {
        val stabilizer = PointStabilizer()
        val target = Vec3(0.10, 0.20, -1.00)

        // Feed 8 samples with < 1 mm jitter over 400 ms
        var state = ReticleState.SEARCHING
        for (i in 0 until 8) {
            val hit = SurfaceHit(
                worldPoint = target + Vec3(0.0005 * (i % 2), 0.0, 0.0),
                normal = Vec3.UP,
                kind = SurfaceKind.WALL,
                source = HitSource.FUSED,
                distanceFromCamera = 1.0f,
                confidence = 0.85f
            )
            state = stabilizer.update(hit, timestampMs = i * 60L)
        }

        assertEquals(ReticleState.LOCKED, state)
        val result = stabilizer.getStabilizedPoint()
        assertTrue(result.isLocked)
        assertTrue(result.jitterMeters <= 0.003)
        assertEquals(target.z, result.point.z, 0.005)
    }

    @Test
    fun `stabilizer clears buffer when target jumps by more than 1 point 5 cm`() {
        val stabilizer = PointStabilizer()
        val target1 = Vec3(0.0, 0.0, -1.0)
        val target2 = Vec3(0.10, 0.0, -1.0) // 10 cm jump

        // Fill buffer with target1
        for (i in 0 until 6) {
            val hit = SurfaceHit(
                worldPoint = target1,
                normal = Vec3.UP,
                kind = SurfaceKind.WALL,
                source = HitSource.PLANE,
                distanceFromCamera = 1.0f,
                confidence = 0.8f
            )
            stabilizer.update(hit, i * 50L)
        }

        // Jump to target2
        val hitJump = SurfaceHit(
            worldPoint = target2,
            normal = Vec3.UP,
            kind = SurfaceKind.WALL,
            source = HitSource.PLANE,
            distanceFromCamera = 1.0f,
            confidence = 0.8f
        )
        val stateAfterJump = stabilizer.update(hitJump, 400L)

        // Buffer was reset, so sample count is now 1 and state is TRACKING (not LOCKED)
        assertFalse(stateAfterJump == ReticleState.LOCKED)
        val res = stabilizer.getStabilizedPoint()
        assertEquals(1, res.sampleCount)
    }
}
