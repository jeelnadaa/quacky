package app.quacky.feature.arruler.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.acos
import kotlin.math.abs

class PlaneFitterTest {

    @Test
    fun `fitPlane recovers tilted plane under noise and 20 percent outliers`() {
        val fitter = PlaneFitter()
        val rng = Random(12345)

        // True plane: horizontal plane at y = -0.50 (normal = (0, 1, 0), offset = -0.50)
        val trueNormal = Vec3(0.0, 1.0, 0.0)
        val trueOffset = -0.50

        val samples = mutableListOf<WeightedSample>()

        // 80 inliers on plane with Gaussian noise sigma = 2 mm
        for (i in 0 until 80) {
            val x = (rng.nextDouble() - 0.5) * 1.0 // 1m patch
            val z = (rng.nextDouble() - 0.5) * 1.0 - 1.5 // 1.5m in front of camera
            val noiseY = rng.nextGaussian() * 0.002 // 2 mm noise
            val y = trueOffset + noiseY

            samples.add(WeightedSample(Vec3(x, y, z), weight = 1.0))
        }

        // 20 outliers scattered away from plane
        for (i in 0 until 20) {
            val x = (rng.nextDouble() - 0.5) * 1.0
            val z = (rng.nextDouble() - 0.5) * 1.0 - 1.5
            val y = trueOffset + (if (rng.nextBoolean()) 0.15 else -0.15) // 15 cm off plane
            samples.add(WeightedSample(Vec3(x, y, z), weight = 0.5))
        }

        val result = fitter.fitPlane(
            samples = samples,
            cameraPosition = Vec3(0.0, 0.0, 0.0),
            medianDistance = 1.5
        )

        assertNotNull(result)
        assertTrue(result!!.isPlanar)

        // Normal error < 1 degree
        val dot = result.normal.dot(trueNormal).coerceIn(-1.0, 1.0)
        val angleDeg = Math.toDegrees(acos(abs(dot)))
        assertTrue("Normal angle error $angleDeg should be < 1.0 deg", angleDeg < 1.0)

        // Offset error < 2 mm
        val offsetErr = abs(result.offset - trueOffset)
        assertTrue("Offset error $offsetErr should be < 2 mm (0.002m)", offsetErr < 0.002)
    }

    @Test
    fun `fitPlane returns non-planar when given sphere or curved cloud`() {
        val fitter = PlaneFitter()
        val rng = Random(42)

        // Points on a cylinder or sphere of radius 0.2m (like a sofa arm)
        val samples = mutableListOf<WeightedSample>()
        for (i in 0 until 100) {
            val theta = rng.nextDouble() * Math.PI
            val r = 0.20
            val x = r * kotlin.math.cos(theta)
            val y = r * kotlin.math.sin(theta)
            val z = -1.0 + (rng.nextDouble() - 0.5) * 0.4
            samples.add(WeightedSample(Vec3(x, y, z), weight = 1.0))
        }

        val result = fitter.fitPlane(
            samples = samples,
            cameraPosition = Vec3.ZERO,
            medianDistance = 1.0
        )

        assertNotNull(result)
        // Should detect curved object / non-planar
        assertTrue(!result!!.isPlanar)
    }
}
