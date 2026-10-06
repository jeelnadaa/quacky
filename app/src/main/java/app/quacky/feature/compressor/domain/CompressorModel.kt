package app.quacky.feature.compressor.domain

import android.net.Uri

enum class CompressionMode {
    QUALITY_SLIDER,
    TARGET_SIZE
}

enum class OutputFormat(val extension: String, val mimeType: String) {
    ORIGINAL("auto", ""),
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp")
}

enum class CompressorPreset(val label: String, val quality: Int, val scale: Float) {
    SMALL("Small", 40, 0.75f),
    BALANCED("Balanced", 70, 1.0f),
    HIGH("High", 85, 1.0f),
    CUSTOM("Custom", 70, 1.0f)
}

data class ImageCompressionConfig(
    val mode: CompressionMode = CompressionMode.QUALITY_SLIDER,
    val quality: Int = 70,
    val targetSizeKb: Long = 200L,
    val preset: CompressorPreset = CompressorPreset.BALANCED,
    val outputFormat: OutputFormat = OutputFormat.ORIGINAL,
    val stripMetadata: Boolean = true,
    val keepDimensions: Boolean = true,
    val resizeScale: Float = 1.0f,
    val pngBgColorWhite: Boolean = true
)

data class PdfCompressionConfig(
    val dpi: Int = 100,
    val pageQuality: Int = 70
)

data class CompressibleFile(
    val uri: Uri,
    val fileName: String,
    val originalSizeBytes: Long,
    val mimeType: String,
    val isPdf: Boolean = false,
    val formattedOriginalSize: String = ""
)

data class CompressionResult(
    val originalFileName: String,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val outputUri: Uri,
    val percentSaved: Int,
    val isSmaller: Boolean,
    val message: String,
    val isPdf: Boolean = false
) {
    val formattedOriginalSize: String
        get() = formatBytes(originalSizeBytes)
    val formattedCompressedSize: String
        get() = formatBytes(compressedSizeBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.2f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }

        fun calculatePercentSaved(original: Long, compressed: Long): Int {
            if (original <= 0 || compressed >= original) return 0
            val saved = ((original - compressed).toDouble() / original.toDouble()) * 100.0
            return saved.toInt().coerceIn(0, 99)
        }
    }
}
