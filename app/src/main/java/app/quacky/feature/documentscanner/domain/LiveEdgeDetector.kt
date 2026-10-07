package app.quacky.feature.documentscanner.domain

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class LiveEdgeDetector(
    private val onEdgeDetected: (DocumentQuad?) -> Unit
) : ImageAnalysis.Analyzer {

    private var previousQuad: DocumentQuad? = null
    private var lastAnalyzedTime = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        // Throttle analysis to ~12 fps so UI and preview remain silky smooth
        if (currentTime - lastAnalyzedTime < 80) {
            image.close()
            return
        }
        lastAnalyzedTime = currentTime

        try {
            val planes = image.planes
            if (planes.isEmpty()) {
                image.close()
                return
            }
            val yBuffer: ByteBuffer = planes[0].buffer
            val width = image.width
            val height = image.height
            val rowStride = planes[0].rowStride
            val pixelStride = planes[0].pixelStride

            // Downsample to a small grid for instant edge analysis
            val sampleW = 64
            val sampleH = 48
            val stepX = max(1, width / sampleW)
            val stepY = max(1, height / sampleH)

            val sampled = IntArray(sampleW * sampleH)
            var sumLum = 0L
            var count = 0

            for (sy in 0 until sampleH) {
                val imgY = min(height - 1, sy * stepY)
                val rowOffset = imgY * rowStride
                for (sx in 0 until sampleW) {
                    val imgX = min(width - 1, sx * stepX)
                    val pos = rowOffset + imgX * pixelStride
                    if (pos < yBuffer.capacity()) {
                        val lum = yBuffer.get(pos).toInt() and 0xFF
                        sampled[sy * sampleW + sx] = lum
                        sumLum += lum
                        count++
                    }
                }
            }

            if (count == 0) {
                image.close()
                return
            }

            val avgLum = (sumLum / count).toInt()
            // Threshold: find brighter document pixels against background
            val threshold = (avgLum * 1.15f).toInt().coerceIn(60, 220)

            var minX = sampleW
            var maxX = 0
            var minY = sampleH
            var maxY = 0
            var docPixels = 0

            for (sy in 0 until sampleH) {
                for (sx in 0 until sampleW) {
                    val lum = sampled[sy * sampleW + sx]
                    if (lum > threshold) {
                        docPixels++
                        if (sx < minX) minX = sx
                        if (sx > maxX) maxX = sx
                        if (sy < minY) minY = sy
                        if (sy > maxY) maxY = sy
                    }
                }
            }

            val areaFraction = docPixels.toFloat() / (sampleW * sampleH)
            // If reasonable document region detected (between 12% and 92% of frame)
            if (areaFraction in 0.12f..0.92f && maxX > minX + 10 && maxY > minY + 10) {
                val padX = (maxX - minX) * 0.03f
                val padY = (maxY - minY) * 0.03f

                val normTL = CornerPoint(((minX - padX) / sampleW).coerceIn(0.04f, 0.45f), ((minY - padY) / sampleH).coerceIn(0.04f, 0.45f))
                val normTR = CornerPoint(((maxX + padX) / sampleW).coerceIn(0.55f, 0.96f), ((minY - padY) / sampleH).coerceIn(0.04f, 0.45f))
                val normBR = CornerPoint(((maxX + padX) / sampleW).coerceIn(0.55f, 0.96f), ((maxY + padY) / sampleH).coerceIn(0.55f, 0.96f))
                val normBL = CornerPoint(((minX - padX) / sampleW).coerceIn(0.04f, 0.45f), ((maxY + padY) / sampleH).coerceIn(0.55f, 0.96f))

                val newQuad = DocumentQuad(normTL, normTR, normBR, normBL)

                // Smooth with previous quad to avoid jitter
                val prev = previousQuad
                val smoothed = if (prev != null) {
                    smoothQuad(prev, newQuad, 0.35f)
                } else {
                    newQuad
                }
                previousQuad = smoothed
                onEdgeDetected(smoothed)
            } else {
                val fallback = DocumentQuad(
                    topLeft = CornerPoint(0.10f, 0.10f),
                    topRight = CornerPoint(0.90f, 0.10f),
                    bottomRight = CornerPoint(0.90f, 0.90f),
                    bottomLeft = CornerPoint(0.10f, 0.90f)
                )
                onEdgeDetected(fallback)
            }
        } catch (_: Exception) {
            // Keep preview running cleanly on any frame read failure
        } finally {
            image.close()
        }
    }

    private fun smoothQuad(a: DocumentQuad, b: DocumentQuad, factor: Float): DocumentQuad {
        return DocumentQuad(
            topLeft = CornerPoint(a.topLeft.x * (1 - factor) + b.topLeft.x * factor, a.topLeft.y * (1 - factor) + b.topLeft.y * factor),
            topRight = CornerPoint(a.topRight.x * (1 - factor) + b.topRight.x * factor, a.topRight.y * (1 - factor) + b.topRight.y * factor),
            bottomRight = CornerPoint(a.bottomRight.x * (1 - factor) + b.bottomRight.x * factor, a.bottomRight.y * (1 - factor) + b.bottomRight.y * factor),
            bottomLeft = CornerPoint(a.bottomLeft.x * (1 - factor) + b.bottomLeft.x * factor, a.bottomLeft.y * (1 - factor) + b.bottomLeft.y * factor)
        )
    }
}
