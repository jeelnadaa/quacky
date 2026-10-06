package app.quacky.feature.qrgenerator.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream

enum class GeneratorFormat(val zxingFormat: BarcodeFormat, val displayName: String, val is2D: Boolean) {
    QR_CODE(BarcodeFormat.QR_CODE, "QR Code", true),
    CODE_128(BarcodeFormat.CODE_128, "Code 128", false),
    CODE_39(BarcodeFormat.CODE_39, "Code 39", false),
    EAN_13(BarcodeFormat.EAN_13, "EAN-13", false),
    EAN_8(BarcodeFormat.EAN_8, "EAN-8", false),
    UPC_A(BarcodeFormat.UPC_A, "UPC-A", false),
    DATA_MATRIX(BarcodeFormat.DATA_MATRIX, "Data Matrix", true),
    PDF_417(BarcodeFormat.PDF_417, "PDF 417", true),
    AZTEC(BarcodeFormat.AZTEC, "Aztec", true)
}

enum class EcLevel(val zxingLevel: ErrorCorrectionLevel, val label: String) {
    L(ErrorCorrectionLevel.L, "L (7%)"),
    M(ErrorCorrectionLevel.M, "M (15%)"),
    Q(ErrorCorrectionLevel.Q, "Q (25%)"),
    H(ErrorCorrectionLevel.H, "H (30%)")
}

data class GeneratorOptions(
    val format: GeneratorFormat = GeneratorFormat.QR_CODE,
    val ecLevel: EcLevel = EcLevel.M,
    val margin: Int = 2,
    val foregroundColor: Int = Color.BLACK,
    val backgroundColor: Int = Color.WHITE,
    val width: Int = 600,
    val height: Int = 600
)

object BarcodeGeneratorEngine {

    /**
     * Validates input for specific barcode formats and sanitizes/appends check digits.
     */
    fun validateAndSanitizeInput(content: String, format: GeneratorFormat): Result<String> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Content cannot be empty"))
        }

        return when (format) {
            GeneratorFormat.EAN_13 -> {
                val digits = trimmed.filter { it.isDigit() }
                when (digits.length) {
                    12 -> Result.success(digits + calculateEanChecksum(digits))
                    13 -> {
                        val expected = calculateEanChecksum(digits.take(12))
                        if (digits.last() == expected) Result.success(digits)
                        else Result.failure(IllegalArgumentException("Invalid EAN-13 checksum digit (expected $expected)"))
                    }
                    else -> Result.failure(IllegalArgumentException("EAN-13 requires exactly 12 or 13 digits"))
                }
            }
            GeneratorFormat.EAN_8 -> {
                val digits = trimmed.filter { it.isDigit() }
                when (digits.length) {
                    7 -> Result.success(digits + calculateEan8Checksum(digits))
                    8 -> {
                        val expected = calculateEan8Checksum(digits.take(7))
                        if (digits.last() == expected) Result.success(digits)
                        else Result.failure(IllegalArgumentException("Invalid EAN-8 checksum digit (expected $expected)"))
                    }
                    else -> Result.failure(IllegalArgumentException("EAN-8 requires exactly 7 or 8 digits"))
                }
            }
            GeneratorFormat.UPC_A -> {
                val digits = trimmed.filter { it.isDigit() }
                when (digits.length) {
                    11 -> Result.success(digits + calculateUpcAChecksum(digits))
                    12 -> {
                        val expected = calculateUpcAChecksum(digits.take(11))
                        if (digits.last() == expected) Result.success(digits)
                        else Result.failure(IllegalArgumentException("Invalid UPC-A checksum digit (expected $expected)"))
                    }
                    else -> Result.failure(IllegalArgumentException("UPC-A requires exactly 11 or 12 digits"))
                }
            }
            GeneratorFormat.CODE_39 -> {
                val valid = trimmed.all { it in "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ -.$/+%" }
                if (valid) Result.success(trimmed.uppercase())
                else Result.failure(IllegalArgumentException("Code 39 only allows uppercase letters, digits, and -.$/+%"))
            }
            else -> Result.success(trimmed)
        }
    }

    /**
     * Generates a ZXing BitMatrix.
     */
    fun generateBitMatrix(
        content: String,
        options: GeneratorOptions
    ): BitMatrix {
        val hints = mutableMapOf<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = options.margin

        if (options.format == GeneratorFormat.QR_CODE) {
            hints[EncodeHintType.ERROR_CORRECTION] = options.ecLevel.zxingLevel
        }

        val h = if (options.format.is2D) options.height else (options.width / 3).coerceAtLeast(150)
        return MultiFormatWriter().encode(
            content,
            options.format.zxingFormat,
            options.width,
            h,
            hints
        )
    }

    /**
     * Renders a BitMatrix to an Android Bitmap.
     */
    fun renderBitmap(matrix: BitMatrix, options: GeneratorOptions): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix.get(x, y)) options.foregroundColor else options.backgroundColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Exports barcode BitMatrix to vector SVG string.
     */
    fun exportToSvg(matrix: BitMatrix, options: GeneratorOptions): String {
        val width = matrix.width
        val height = matrix.height
        val fgHex = String.format("#%06X", 0xFFFFFF and options.foregroundColor)
        val bgHex = String.format("#%06X", 0xFFFFFF and options.backgroundColor)

        val sb = StringBuilder()
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $width $height" width="$width" height="$height">""")
        sb.append("""<rect width="100%" height="100%" fill="$bgHex"/>""")

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (matrix.get(x, y)) {
                    sb.append("""<rect x="$x" y="$y" width="1" height="1" fill="$fgHex"/>""")
                }
            }
        }

        sb.append("</svg>")
        return sb.toString()
    }

    /**
     * Exports barcode Bitmap to a single-page PDF document bytes.
     */
    fun exportToPdf(bitmap: Bitmap, title: String): ByteArray {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        // Paint background white
        val paint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Title text
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title.take(50), 595f / 2, 80f, textPaint)

        // Draw scaled barcode at center
        val barcodeSize = 400f
        val left = (595f - barcodeSize) / 2
        val top = (842f - barcodeSize) / 2
        val destRect = android.graphics.RectF(left, top, left + barcodeSize, top + barcodeSize)
        canvas.drawBitmap(bitmap, null, destRect, null)

        doc.finishPage(page)

        val outputStream = ByteArrayOutputStream()
        doc.writeTo(outputStream)
        doc.close()
        return outputStream.toByteArray()
    }

    private fun calculateEanChecksum(digits12: String): Char {
        var sum = 0
        for (i in digits12.indices) {
            val d = digits12[i].digitToInt()
            sum += if (i % 2 == 0) d else d * 3
        }
        val rem = sum % 10
        val check = if (rem == 0) 0 else 10 - rem
        return check.digitToChar()
    }

    private fun calculateEan8Checksum(digits7: String): Char {
        var sum = 0
        for (i in digits7.indices) {
            val d = digits7[i].digitToInt()
            sum += if (i % 2 == 0) d * 3 else d
        }
        val rem = sum % 10
        val check = if (rem == 0) 0 else 10 - rem
        return check.digitToChar()
    }

    private fun calculateUpcAChecksum(digits11: String): Char {
        var sum = 0
        for (i in digits11.indices) {
            val d = digits11[i].digitToInt()
            sum += if (i % 2 == 0) d * 3 else d
        }
        val rem = sum % 10
        val check = if (rem == 0) 0 else 10 - rem
        return check.digitToChar()
    }
}
