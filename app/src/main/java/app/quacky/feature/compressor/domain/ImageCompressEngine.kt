package app.quacky.feature.compressor.domain

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.sqrt

class ImageCompressEngine(private val context: Context) {

    suspend fun compressImage(
        uri: Uri,
        fileName: String,
        originalSize: Long,
        config: ImageCompressionConfig,
        onProgress: (String) -> Unit = {}
    ): CompressionResult = withContext(Dispatchers.IO) {
        onProgress("Decoding $fileName…")

        // Read source bytes and check memory safety
        val sourceBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot read image")

        // Determine output format
        val format = when (config.outputFormat) {
            OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG
            OutputFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            OutputFormat.ORIGINAL -> {
                if (fileName.endsWith(".png", ignoreCase = true)) Bitmap.CompressFormat.PNG
                else if (fileName.endsWith(".webp", ignoreCase = true)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
                    else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
                } else Bitmap.CompressFormat.JPEG
            }
        }

        val originalBitmap = decodeMemorySafe(sourceBytes)
            ?: throw IllegalStateException("Failed to decode image")

        try {
            var quality = config.quality
            var scale = config.resizeScale

            if (config.mode == CompressionMode.TARGET_SIZE) {
                onProgress("Optimizing size to under ${config.targetSizeKb} KB…")
                val targetBytes = config.targetSizeKb * 1024L
                val (optQuality, optScale) = binarySearchTargetSize(
                    bitmap = originalBitmap,
                    targetBytes = targetBytes,
                    format = format,
                    keepDimensions = config.keepDimensions
                )
                quality = optQuality
                scale = optScale
            }

            onProgress("Compressing at quality $quality%…")
            val compressedBytes = compressBitmapToBytes(
                source = originalBitmap,
                format = format,
                quality = quality,
                scale = scale,
                fillBgWhite = config.pngBgColorWhite
            )

            val compressedSize = compressedBytes.size.toLong()
            val isSmaller = compressedSize < originalSize

            // Section 5A.5: Never claim saved unless actually smaller. If not smaller, preserve original
            if (!isSmaller) {
                val outputUri = saveImageToMediaStore(
                    bytes = sourceBytes,
                    displayName = "original_kept_$fileName",
                    mimeType = getMimeType(format)
                )
                CompressionResult(
                    originalFileName = fileName,
                    originalSizeBytes = originalSize,
                    compressedSizeBytes = originalSize,
                    outputUri = outputUri,
                    percentSaved = 0,
                    isSmaller = false,
                    message = "Could not compress smaller than original without severe degradation. Original preserved."
                )
            } else {
                val percentSaved = CompressionResult.calculatePercentSaved(originalSize, compressedSize)
                val cleanName = "compressed_${fileName.substringBeforeLast('.')}.${getExtension(format)}"
                val outputUri = saveImageToMediaStore(
                    bytes = compressedBytes,
                    displayName = cleanName,
                    mimeType = getMimeType(format)
                )

                CompressionResult(
                    originalFileName = fileName,
                    originalSizeBytes = originalSize,
                    compressedSizeBytes = compressedSize,
                    outputUri = outputUri,
                    percentSaved = percentSaved,
                    isSmaller = true,
                    message = "Saved $percentSaved% (${CompressionResult.formatBytes(originalSize)} → ${CompressionResult.formatBytes(compressedSize)})"
                )
            }
        } finally {
            originalBitmap.recycle()
        }
    }

    private fun decodeMemorySafe(bytes: ByteArray): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

        val maxDim = 4096
        var sampleSize = 1
        var w = boundsOptions.outWidth
        var h = boundsOptions.outHeight
        while (w > maxDim || h > maxDim) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

    fun binarySearchTargetSize(
        bitmap: Bitmap,
        targetBytes: Long,
        format: Bitmap.CompressFormat,
        keepDimensions: Boolean
    ): Pair<Int, Float> {
        var lowQ = 5
        var highQ = 95
        var bestQ = 50
        var currentScale = 1.0f

        // First attempt with scale 1.0
        for (i in 0..6) {
            val midQ = (lowQ + highQ) / 2
            val size = compressBitmapToBytes(bitmap, format, midQ, currentScale).size.toLong()
            if (size <= targetBytes) {
                bestQ = midQ
                lowQ = midQ + 1
            } else {
                highQ = midQ - 1
            }
        }

        // If at lowest quality it still exceeds target, downscale dimensions if allowed
        if (!keepDimensions) {
            val minSize = compressBitmapToBytes(bitmap, format, 10, currentScale).size.toLong()
            if (minSize > targetBytes) {
                val ratio = (targetBytes.toDouble() / minSize.toDouble()).coerceIn(0.1, 0.9)
                currentScale = (sqrt(ratio) * 0.9).toFloat().coerceIn(0.2f, 0.9f)
                bestQ = 60
            }
        }

        return Pair(bestQ, currentScale)
    }

    private fun compressBitmapToBytes(
        source: Bitmap,
        format: Bitmap.CompressFormat,
        quality: Int,
        scale: Float = 1.0f,
        fillBgWhite: Boolean = true
    ): ByteArray {
        val targetBitmap = if (scale < 0.99f) {
            val w = (source.width * scale).toInt().coerceAtLeast(1)
            val h = (source.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(source, w, h, true)
        } else source

        // Handle JPEG background fill for transparency
        val finalBitmap = if (format == Bitmap.CompressFormat.JPEG && targetBitmap.hasAlpha()) {
            val opaque = Bitmap.createBitmap(targetBitmap.width, targetBitmap.height, Bitmap.Config.RGB_565)
            val canvas = Canvas(opaque)
            canvas.drawColor(if (fillBgWhite) Color.WHITE else Color.BLACK)
            canvas.drawBitmap(targetBitmap, 0f, 0f, null)
            opaque
        } else targetBitmap

        val out = ByteArrayOutputStream()
        finalBitmap.compress(format, quality.coerceIn(1, 100), out)

        if (finalBitmap != source && finalBitmap != targetBitmap) {
            finalBitmap.recycle()
        }
        if (targetBitmap != source) {
            targetBitmap.recycle()
        }

        return out.toByteArray()
    }

    private fun saveImageToMediaStore(bytes: ByteArray, displayName: String, mimeType: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Quacky")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Failed to create MediaStore image entry")

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }

        return uri
    }

    private fun getMimeType(format: Bitmap.CompressFormat): String {
        return when (format) {
            Bitmap.CompressFormat.PNG -> "image/png"
            Bitmap.CompressFormat.JPEG -> "image/jpeg"
            else -> "image/webp"
        }
    }

    private fun getExtension(format: Bitmap.CompressFormat): String {
        return when (format) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.JPEG -> "jpg"
            else -> "webp"
        }
    }
}
