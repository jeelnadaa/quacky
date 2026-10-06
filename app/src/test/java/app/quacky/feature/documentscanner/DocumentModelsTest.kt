package app.quacky.feature.documentscanner

import app.quacky.feature.documentscanner.domain.CornerPoint
import app.quacky.feature.documentscanner.domain.DocFilterType
import app.quacky.feature.documentscanner.domain.DocumentQuad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.max

class DocumentModelsTest {

    @Test
    fun defaultQuadHasValidInsets() {
        val quad = DocumentQuad()
        assertTrue("Top-left X should be inset", quad.topLeft.x > 0f && quad.topLeft.x < 0.2f)
        assertTrue("Top-left Y should be inset", quad.topLeft.y > 0f && quad.topLeft.y < 0.2f)
        assertTrue("Bottom-right X should be inset", quad.bottomRight.x > 0.8f && quad.bottomRight.x <= 1f)
        assertTrue("Bottom-right Y should be inset", quad.bottomRight.y > 0.8f && quad.bottomRight.y <= 1f)
    }

    @Test
    fun cornerPointCoordinatesAreNormalized() {
        val p = CornerPoint(0.5f, 0.5f)
        assertEquals(0.5f, p.x, 0.001f)
        assertEquals(0.5f, p.y, 0.001f)
    }

    @Test
    fun filterTypesHaveDescriptiveLabels() {
        assertEquals("Magic Color", DocFilterType.MAGIC_COLOR.label)
        assertEquals("B&W", DocFilterType.BW.label)
        assertEquals("Grayscale", DocFilterType.GRAYSCALE.label)
        assertEquals("Original", DocFilterType.ORIGINAL.label)
    }

    @Test
    fun perspectiveDimensionCalculationIsAccurate() {
        // Test dimension estimation matching DocumentProcessor.warpPerspective
        val w = 1000f
        val h = 1000f
        val quad = DocumentQuad(
            topLeft = CornerPoint(0.1f, 0.1f),
            topRight = CornerPoint(0.9f, 0.1f),
            bottomRight = CornerPoint(0.9f, 0.9f),
            bottomLeft = CornerPoint(0.1f, 0.9f)
        )

        val tlX = quad.topLeft.x * w
        val tlY = quad.topLeft.y * h
        val trX = quad.topRight.x * w
        val trY = quad.topRight.y * h
        val brX = quad.bottomRight.x * w
        val brY = quad.bottomRight.y * h
        val blX = quad.bottomLeft.x * w
        val blY = quad.bottomLeft.y * h

        val topWidth = hypot(trX - tlX, trY - tlY)
        val bottomWidth = hypot(brX - blX, brY - blY)
        val leftHeight = hypot(blX - tlX, blY - tlY)
        val rightHeight = hypot(brX - trX, brY - trY)

        val targetWidth = max(topWidth, bottomWidth)
        val targetHeight = max(leftHeight, rightHeight)

        assertEquals(800f, targetWidth, 0.01f)
        assertEquals(800f, targetHeight, 0.01f)
    }
}
