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

            // Compute horizontal and vertical gradients to identify true document edges
            val grad = IntArray(sampleW * sampleH)
            var sumGrad = 0L
            var gradCount = 0

            for (sy in 1 until sampleH - 1) {
                for (sx in 1 until sampleW - 1) {
                    val gx = kotlin.math.abs(sampled[sy * sampleW + (sx + 1)] - sampled[sy * sampleW + (sx - 1)])
                    val gy = kotlin.math.abs(sampled[(sy + 1) * sampleW + sx] - sampled[(sy - 1) * sampleW + sx])
                    val magnitude = gx + gy
                    grad[sy * sampleW + sx] = magnitude
                    sumGrad += magnitude
                    gradCount++
                }
            }

            val avgGrad = if (gradCount > 0) (sumGrad / gradCount).toInt() else 15
            val edgeThreshold = max(25, avgGrad * 2)

            // Scan inward from each border to locate the outermost strong edge lines
            // Left edge
            var leftEdge = 0
            for (sx in 2 until sampleW / 2) {
                var edgeHits = 0
                for (sy in sampleH / 5 until (sampleH * 4) / 5) {
                    if (grad[sy * sampleW + sx] > edgeThreshold) edgeHits++
                }
                if (edgeHits >= sampleH / 8) {
                    leftEdge = sx
                    break
                }
            }

            // Right edge
            var rightEdge = sampleW - 1
            for (sx in sampleW - 3 downTo sampleW / 2) {
                var edgeHits = 0
                for (sy in sampleH / 5 until (sampleH * 4) / 5) {
                    if (grad[sy * sampleW + sx] > edgeThreshold) edgeHits++
                }
                if (edgeHits >= sampleH / 8) {
                    rightEdge = sx
                    break
                }
            }

            // Top edge
            var topEdge = 0
            for (sy in 2 until sampleH / 2) {
                var edgeHits = 0
                for (sx in sampleW / 5 until (sampleW * 4) / 5) {
                    if (grad[sy * sampleW + sx] > edgeThreshold) edgeHits++
                }
                if (edgeHits >= sampleW / 8) {
                    topEdge = sy
                    break
                }
            }

            // Bottom edge
            var bottomEdge = sampleH - 1
            for (sy in sampleH - 3 downTo sampleH / 2) {
                var edgeHits = 0
                for (sx in sampleW / 5 until (sampleW * 4) / 5) {
                    if (grad[sy * sampleW + sx] > edgeThreshold) edgeHits++
                }
                if (edgeHits >= sampleW / 8) {
                    bottomEdge = sy
                    break
                }
            }

            // Also check brightness contrast to refine corners
            val avgLum = (sumLum / count).toInt()
            val lumThreshold = (avgLum * 1.1f).toInt().coerceIn(50, 230)

            var minX = leftEdge.coerceAtLeast(1)
            var maxX = rightEdge.coerceAtMost(sampleW - 2)
            var minY = topEdge.coerceAtLeast(1)
            var maxY = bottomEdge.coerceAtMost(sampleH - 2)

            // If edge scan gave narrow region, fallback to adaptive luminance bounding
            if (maxX - minX < sampleW / 4 || maxY - minY < sampleH / 4) {
                var docMinX = sampleW
                var docMaxX = 0
                var docMinY = sampleH
                var docMaxY = 0
                var found = 0

                for (sy in 0 until sampleH) {
                    for (sx in 0 until sampleW) {
                        if (sampled[sy * sampleW + sx] > lumThreshold) {
                            found++
                            if (sx < docMinX) docMinX = sx
                            if (sx > docMaxX) docMaxX = sx
                            if (sy < docMinY) docMinY = sy
                            if (sy > docMaxY) docMaxY = sy
                        }
                    }
                }

                if (found > (sampleW * sampleH) * 0.10f && docMaxX > docMinX + 8 && docMaxY > docMinY + 8) {
                    minX = docMinX
                    maxX = docMaxX
                    minY = docMinY
                    maxY = docMaxY
                }
            }

            // Compute normalized corners without artificial quadrant clamping
            val padX = (maxX - minX) * 0.02f
            val padY = (maxY - minY) * 0.02f

            val normLeft = ((minX - padX) / sampleW).coerceIn(0.03f, 0.90f)
            val normRight = ((maxX + padX) / sampleW).coerceIn(0.10f, 0.97f)
            val normTop = ((minY - padY) / sampleH).coerceIn(0.03f, 0.90f)
            val normBottom = ((maxY + padY) / sampleH).coerceIn(0.10f, 0.97f)

            if (normRight > normLeft + 0.15f && normBottom > normTop + 0.15f) {
                val newQuad = DocumentQuad(
                    topLeft = CornerPoint(normLeft, normTop),
                    topRight = CornerPoint(normRight, normTop),
                    bottomRight = CornerPoint(normRight, normBottom),
                    bottomLeft = CornerPoint(normLeft, normBottom)
                )

                val prev = previousQuad
                val smoothed = if (prev != null) {
                    smoothQuad(prev, newQuad, 0.30f)
                } else {
                    newQuad
                }
                previousQuad = smoothed
                onEdgeDetected(smoothed)
            } else {
                val fallback = DocumentQuad(
                    topLeft = CornerPoint(0.08f, 0.08f),
                    topRight = CornerPoint(0.92f, 0.08f),
                    bottomRight = CornerPoint(0.92f, 0.92f),
                    bottomLeft = CornerPoint(0.08f, 0.92f)
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
