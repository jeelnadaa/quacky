package app.quacky.feature.documentscanner.domain

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    private const val A4_WIDTH_PTS = 595
    private const val A4_HEIGHT_PTS = 842
    private const val MARGIN_PTS = 24f

    /**
     * Compiles a list of processed page bitmaps into a multi-page PDF document.
     */
    fun generatePdf(
        pageBitmaps: List<Bitmap>,
        outputFile: File
    ): Result<File> {
        if (pageBitmaps.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cannot generate PDF from empty pages list"))
        }

        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            pageBitmaps.forEachIndexed { index, bitmap ->
                val isLandscape = bitmap.width > bitmap.height
                val pageWidth = if (isLandscape) A4_HEIGHT_PTS else A4_WIDTH_PTS
                val pageHeight = if (isLandscape) A4_WIDTH_PTS else A4_HEIGHT_PTS

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                // Available printable box
                val printableWidth = pageWidth - (MARGIN_PTS * 2)
                val printableHeight = pageHeight - (MARGIN_PTS * 2)

                val scaleX = printableWidth / bitmap.width.toFloat()
                val scaleY = printableHeight / bitmap.height.toFloat()
                val scale = minOf(scaleX, scaleY)

                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale

                val left = MARGIN_PTS + (printableWidth - scaledWidth) / 2f
                val top = MARGIN_PTS + (printableHeight - scaledHeight) / 2f

                val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
                canvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDoc.finishPage(page)
            }

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }

            return Result.success(outputFile)
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            pdfDoc.close()
        }
    }
}
