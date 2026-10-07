package app.quacky.feature.arruler.engine

import android.media.Image
import com.google.ar.core.Camera
import com.google.ar.core.CameraIntrinsics
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.sqrt

data class DepthSampleResult(
    val samples: List<WeightedSample>,
    val centerDepthMeters: Double?,
    val isDepthReady: Boolean,
    val meanConfidence: Float,
    val validSampleRatio: Float
)

class DepthSampler {

    // Preallocated coordinate arrays
    private val viewCoords = FloatArray(2)
    private val normalizedCoords = FloatArray(2)
    private val point3d = FloatArray(3)

    // Preallocated sample list buffer to avoid allocations
    private val maxSamples = (MeasureTuning.DEPTH_WINDOW_RADIUS_DEFAULT * 2 + 1) *
            (MeasureTuning.DEPTH_WINDOW_RADIUS_DEFAULT * 2 + 1)
    private val samplesBuffer = ArrayList<WeightedSample>(maxSamples)

    fun sampleWindow(
        frame: Frame,
        camera: Camera,
        viewX: Float,
        viewY: Float,
        estimatedDistanceMeters: Float?
    ): DepthSampleResult {
        samplesBuffer.clear()

        var rawDepthImage: Image? = null
        var rawConfidenceImage: Image? = null
        var fullDepthImage: Image? = null

        try {
            rawDepthImage = try { frame.acquireRawDepthImage16Bits() } catch (e: NotYetAvailableException) { null }
            rawConfidenceImage = try { frame.acquireRawDepthConfidenceImage() } catch (e: NotYetAvailableException) { null }
            fullDepthImage = try { frame.acquireDepthImage16Bits() } catch (e: NotYetAvailableException) { null }

            if (rawDepthImage == null && fullDepthImage == null) {
                return DepthSampleResult(
                    samples = emptyList(),
                    centerDepthMeters = null,
                    isDepthReady = false,
                    meanConfidence = 0f,
                    validSampleRatio = 0f
                )
            }

            // Map view coordinates to normalized image coordinates
            viewCoords[0] = viewX
            viewCoords[1] = viewY
            frame.transformCoordinates2d(
                Coordinates2d.VIEW,
                viewCoords,
                Coordinates2d.IMAGE_NORMALIZED,
                normalizedCoords
            )

            val primaryImage = rawDepthImage ?: fullDepthImage!!
            val depthW = primaryImage.width
            val depthH = primaryImage.height

            val centerU = (normalizedCoords[0] * depthW).toInt().coerceIn(0, depthW - 1)
            val centerV = (normalizedCoords[1] * depthH).toInt().coerceIn(0, depthH - 1)

            val radius = if (estimatedDistanceMeters != null &&
                estimatedDistanceMeters < MeasureTuning.DEPTH_CLOSE_THRESHOLD_METERS) {
                MeasureTuning.DEPTH_WINDOW_RADIUS_CLOSE
            } else {
                MeasureTuning.DEPTH_WINDOW_RADIUS_DEFAULT
            }
            val sigma = radius / 2.0
            val twoSigmaSq = 2.0 * sigma * sigma

            val intrinsics = camera.imageIntrinsics
            val intrinsicsDims = intrinsics.imageDimensions
            val sx = depthW.toDouble() / intrinsicsDims[0]
            val sy = depthH.toDouble() / intrinsicsDims[1]
            val fx = intrinsics.focalLength[0] * sx
            val fy = intrinsics.focalLength[1] * sy
            val cx = intrinsics.principalPoint[0] * sx
            val cy = intrinsics.principalPoint[1] * sy

            val cameraPose = camera.pose // Sensor-oriented pose

            val rawDepthBuffer = rawDepthImage?.planes?.get(0)?.buffer?.order(ByteOrder.LITTLE_ENDIAN)
            val rawDepthRowStride = rawDepthImage?.planes?.get(0)?.rowStride ?: 0
            val rawDepthPixelStride = rawDepthImage?.planes?.get(0)?.pixelStride ?: 2

            val confBuffer = rawConfidenceImage?.planes?.get(0)?.buffer
            val confRowStride = rawConfidenceImage?.planes?.get(0)?.rowStride ?: 0
            val confPixelStride = rawConfidenceImage?.planes?.get(0)?.pixelStride ?: 1

            val fullDepthBuffer = fullDepthImage?.planes?.get(0)?.buffer?.order(ByteOrder.LITTLE_ENDIAN)
            val fullDepthRowStride = fullDepthImage?.planes?.get(0)?.rowStride ?: 0
            val fullDepthPixelStride = fullDepthImage?.planes?.get(0)?.pixelStride ?: 2

            var centerDepthMeters: Double? = null
            var totalValidSamples = 0
            var totalQueried = 0
            var sumConfidence = 0.0

            for (dv in -radius..radius) {
                val v = centerV + dv
                if (v !in 0 until depthH) continue

                for (du in -radius..radius) {
                    val u = centerU + du
                    if (u !in 0 until depthW) continue

                    totalQueried++
                    val spatialDistSq = (du * du + dv * dv).toDouble()
                    val spatialWeight = exp(-spatialDistSq / twoSigmaSq)

                    var sampleZ: Double? = null
                    var sampleWeight = 0.0
                    var isFromFull = false

                    // Try raw depth + confidence first
                    if (rawDepthBuffer != null && confBuffer != null) {
                        val rawIndex = v * rawDepthRowStride + u * rawDepthPixelStride
                        val depthMm = (rawDepthBuffer.getShort(rawIndex).toInt() and 0xFFFF)
                        val confIndex = v * confRowStride + u * confPixelStride
                        val confByte = (confBuffer.get(confIndex).toInt() and 0xFF)

                        if (depthMm > 0) {
                            val zMeters = depthMm / 1000.0
                            if (zMeters in MeasureTuning.DEPTH_MIN_RANGE_METERS..MeasureTuning.DEPTH_MAX_RANGE_METERS &&
                                confByte >= MeasureTuning.DEPTH_MIN_CONFIDENCE_THRESHOLD) {
                                sampleZ = zMeters
                                val confNorm = confByte / 255.0
                                sampleWeight = (0.5 + 0.5 * confNorm) * spatialWeight
                                totalValidSamples++
                                sumConfidence += confNorm
                            }
                        }
                    }

                    // Fallback to full depth hole filler
                    if (sampleZ == null && fullDepthBuffer != null) {
                        val fullIndex = v * fullDepthRowStride + u * fullDepthPixelStride
                        val depthMm = (fullDepthBuffer.getShort(fullIndex).toInt() and 0xFFFF)
                        if (depthMm > 0) {
                            val zMeters = depthMm / 1000.0
                            if (zMeters in MeasureTuning.DEPTH_MIN_RANGE_METERS..MeasureTuning.DEPTH_MAX_RANGE_METERS) {
                                sampleZ = zMeters
                                sampleWeight = 0.25 * spatialWeight
                                isFromFull = true
                            }
                        }
                    }

                    if (sampleZ != null && sampleWeight > 0.0) {
                        if (du == 0 && dv == 0) {
                            centerDepthMeters = sampleZ
                        }

                        // Back-project to 3D in camera sensor space:
                        // X =  (u - cx) * z / fx
                        // Y = -(v - cy) * z / fy
                        // Z = -z
                        val camX = (u - cx) * sampleZ / fx
                        val camY = -(v - cy) * sampleZ / fy
                        val camZ = -sampleZ

                        point3d[0] = camX.toFloat()
                        point3d[1] = camY.toFloat()
                        point3d[2] = camZ.toFloat()

                        val worldPt = cameraPose.transformPoint(point3d)
                        samplesBuffer.add(
                            WeightedSample(
                                point = Vec3(worldPt[0].toDouble(), worldPt[1].toDouble(), worldPt[2].toDouble()),
                                weight = sampleWeight,
                                isFromFullDepth = isFromFull
                            )
                        )
                    }
                }
            }

            val validRatio = if (totalQueried > 0) totalValidSamples.toFloat() / totalQueried else 0f
            val meanConf = if (totalValidSamples > 0) (sumConfidence / totalValidSamples).toFloat() else 0f
            val isDepthReady = validRatio >= MeasureTuning.DEPTH_READY_MIN_VALID_RATIO &&
                    meanConf >= MeasureTuning.DEPTH_READY_MIN_MEAN_CONFIDENCE

            return DepthSampleResult(
                samples = ArrayList(samplesBuffer),
                centerDepthMeters = centerDepthMeters,
                isDepthReady = isDepthReady,
                meanConfidence = meanConf,
                validSampleRatio = validRatio
            )
        } finally {
            rawDepthImage?.close()
            rawConfidenceImage?.close()
            fullDepthImage?.close()
        }
    }
}
