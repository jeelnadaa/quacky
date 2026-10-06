package app.quacky.feature.compressor.domain

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PdfCompressEngine(private val context: Context) {

    suspend fun compressPdf(
        uri: Uri,
        fileName: String,
        originalSize: Long,
        config: PdfCompressionConfig,
        onProgress: (page: Int, totalPages: Int) -> Unit = { _, _ -> }
    ): CompressionResult = withContext(Dispatchers.IO) {
        // Copy PDF to temp file for ParcelFileDescriptor
        val tempPdf = File(context.cacheDir, "quacky_pdf_input_${System.currentTimeMillis()}.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempPdf).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot read source PDF")

        val pfd = ParcelFileDescriptor.open(tempPdf, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val outputDoc = PdfDocument()
        val tempOutputFile = File(context.cacheDir, "quacky_pdf_output_${System.currentTimeMillis()}.pdf")

        try {
            // Standard PDF points are 72 per inch
            val scaleFactor = config.dpi / 72.0f

            for (pageIndex in 0 until pageCount) {
                onProgress(pageIndex + 1, pageCount)

                val page = renderer.openPage(pageIndex)
                val targetWidth = (page.width * scaleFactor).toInt().coerceAtLeast(1)
                val targetHeight = (page.height * scaleFactor).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                // Re-encode to JPEG to compress
                val jpegStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, config.pageQuality.coerceIn(1, 100), jpegStream)
                bitmap.recycle() // Immediate memory recycling!

                val compressedJpegBytes = jpegStream.toByteArray()
                val compressedPageBitmap = BitmapFactory.decodeByteArray(compressedJpegBytes, 0, compressedJpegBytes.size)

                // Add to PdfDocument
                val pageInfo = PdfDocument.PageInfo.Builder(targetWidth, targetHeight, pageIndex + 1).create()
                val docPage = outputDoc.startPage(pageInfo)
                val canvas = docPage.canvas
                canvas.drawBitmap(compressedPageBitmap, 0f, 0f, null)
                outputDoc.finishPage(docPage)

                compressedPageBitmap.recycle() // Recycle again!
            }

            FileOutputStream(tempOutputFile).use { out ->
                outputDoc.writeTo(out)
            }
            outputDoc.close()
            renderer.close()
            pfd.close()
            tempPdf.delete()

            val compressedSize = tempOutputFile.length()
            val isSmaller = compressedSize < originalSize

            if (!isSmaller) {
                // Keep original if not smaller per 5A.5
                val outputUri = savePdfToMediaStore(uri, "original_kept_$fileName")
                tempOutputFile.delete()

                CompressionResult(
                    originalFileName = fileName,
                    originalSizeBytes = originalSize,
                    compressedSizeBytes = originalSize,
                    outputUri = outputUri,
                    percentSaved = 0,
                    isSmaller = false,
                    message = "Could not compress smaller than original. Original preserved.",
                    isPdf = true
                )
            } else {
                val percentSaved = CompressionResult.calculatePercentSaved(originalSize, compressedSize)
                val cleanName = "compressed_${fileName.removePrefix("compressed_")}"
                val outputUri = saveFileToMediaStore(tempOutputFile, cleanName)
                tempOutputFile.delete()

                CompressionResult(
                    originalFileName = fileName,
                    originalSizeBytes = originalSize,
                    compressedSizeBytes = compressedSize,
                    outputUri = outputUri,
                    percentSaved = percentSaved,
                    isSmaller = true,
                    message = "Saved $percentSaved% (${CompressionResult.formatBytes(originalSize)} → ${CompressionResult.formatBytes(compressedSize)})",
                    isPdf = true
                )
            }
        } catch (e: Exception) {
            outputDoc.close()
            renderer.close()
            pfd.close()
            tempPdf.delete()
            tempOutputFile.delete()
            throw e
        }
    }

    private fun saveFileToMediaStore(file: File, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/Quacky")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
            ?: throw IllegalStateException("Failed to create MediaStore PDF entry")

        context.contentResolver.openOutputStream(uri)?.use { out ->
            FileInputStream(file).use { input ->
                input.copyTo(out)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }

        return uri
    }

    private fun savePdfToMediaStore(sourceUri: Uri, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/Quacky")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
            ?: throw IllegalStateException("Failed to create MediaStore PDF entry")

        context.contentResolver.openOutputStream(uri)?.use { out ->
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.copyTo(out)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }

        return uri
    }
}
