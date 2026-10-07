package app.quacky.feature.documentscanner.domain

import android.graphics.Bitmap
import java.util.UUID

enum class DocFilterType(val label: String) {
    MAGIC_COLOR("Magic Color"),
    BW("B&W"),
    GRAYSCALE("Grayscale"),
    ORIGINAL("Original")
}

data class CornerPoint(
    val x: Float, // Normalized 0f..1f relative to image width/height
    val y: Float
)

data class DocumentQuad(
    val topLeft: CornerPoint = CornerPoint(0.08f, 0.08f),
    val topRight: CornerPoint = CornerPoint(0.92f, 0.08f),
    val bottomRight: CornerPoint = CornerPoint(0.92f, 0.92f),
    val bottomLeft: CornerPoint = CornerPoint(0.08f, 0.92f),
    val score: Float = 0f,
    val isLocked: Boolean = false,
    val isFrameLimited: Boolean = false
)

data class ScannedPage(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val originalBitmap: Bitmap,
    val quad: DocumentQuad = DocumentQuad(),
    val rotationDegrees: Int = 0,
    val filterType: DocFilterType = DocFilterType.MAGIC_COLOR,
    val warpedBitmap: Bitmap? = null
)
