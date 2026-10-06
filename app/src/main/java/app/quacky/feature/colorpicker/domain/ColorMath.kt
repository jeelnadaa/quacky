package app.quacky.feature.colorpicker.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class SampleSize(val size: Int, val label: String) {
    POINT(1, "1×1 (Point)"),
    AVG_3X3(3, "3×3 Avg"),
    AVG_5X5(5, "5×5 Avg"),
    AVG_11X11(11, "11×11 Avg")
}

data class ColorFormats(
    val argbInt: Int,
    val hex: String,
    val rgb: Triple<Int, Int, Int>,
    val hsl: Triple<Float, Float, Float>,
    val hsv: Triple<Float, Float, Float>,
    val cmyk: List<Int>,
    val composeString: String,
    val cssRgb: String
)

data class ContrastResult(
    val ratio: Float,
    val normalAa: Boolean,
    val normalAaa: Boolean,
    val largeAa: Boolean,
    val largeAaa: Boolean
)

object ColorMath {

    const val WHITE = -0x1 // 0xFFFFFFFF
    const val BLACK = -0x1000000 // 0xFF000000
    const val RED = -0x10000 // 0xFFFF0000
    const val GREEN = -0xff0100 // 0xFF00FF00
    const val BLUE = -0xffff01 // 0xFF0000FF
    const val CYAN = -0xff0001 // 0xFF00FFFF

    fun red(color: Int): Int = (color shr 16) and 0xFF
    fun green(color: Int): Int = (color shr 8) and 0xFF
    fun blue(color: Int): Int = color and 0xFF
    fun alpha(color: Int): Int = (color shr 24) and 0xFF

    fun rgb(r: Int, g: Int, b: Int): Int {
        return (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    }

    fun parseHex(hexStr: String): Int {
        val clean = hexStr.removePrefix("#").trim()
        val num = clean.toLong(16)
        return if (clean.length <= 6) {
            (num or 0xFF000000).toInt()
        } else {
            num.toInt()
        }
    }

    fun toFormats(colorInt: Int): ColorFormats {
        val r = red(colorInt)
        val g = green(colorInt)
        val b = blue(colorInt)
        val hex = String.format("#%02X%02X%02X", r, g, b)

        val hsl = rgbToHsl(r, g, b)
        val hsv = rgbToHsv(r, g, b)

        // CMYK
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val k = 1f - max(rf, max(gf, bf))
        val c = if (k < 1f) (1f - rf - k) / (1f - k) else 0f
        val m = if (k < 1f) (1f - gf - k) / (1f - k) else 0f
        val y = if (k < 1f) (1f - bf - k) / (1f - k) else 0f
        val cmyk = listOf(
            (c * 100).roundToInt().coerceIn(0, 100),
            (m * 100).roundToInt().coerceIn(0, 100),
            (y * 100).roundToInt().coerceIn(0, 100),
            (k * 100).roundToInt().coerceIn(0, 100)
        )

        val composeString = String.format("Color(0xFF%02X%02X%02X)", r, g, b)
        val cssRgb = "rgb($r, $g, $b)"

        return ColorFormats(
            argbInt = colorInt,
            hex = hex,
            rgb = Triple(r, g, b),
            hsl = hsl,
            hsv = hsv,
            cmyk = cmyk,
            composeString = composeString,
            cssRgb = cssRgb
        )
    }

    fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val max = max(rf, max(gf, bf))
        val min = min(rf, min(gf, bf))
        val delta = max - min
        val l = (max + min) / 2f

        val s = if (delta == 0f) {
            0f
        } else if (l <= 0.5f) {
            delta / (max + min)
        } else {
            delta / (2f - max - min)
        }

        var h = if (delta == 0f) {
            0f
        } else if (max == rf) {
            ((gf - bf) / delta + (if (gf < bf) 6f else 0f))
        } else if (max == gf) {
            ((bf - rf) / delta + 2f)
        } else {
            ((rf - gf) / delta + 4f)
        }
        h = (h * 60f) % 360f
        if (h < 0f) h += 360f

        return Triple(h, s * 100f, l * 100f)
    }

    fun hslToRgb(hDeg: Float, sPercent: Float, lPercent: Float): Int {
        val h = (hDeg % 360f + 360f) % 360f / 360f
        val s = (sPercent / 100f).coerceIn(0f, 1f)
        val l = (lPercent / 100f).coerceIn(0f, 1f)

        if (s == 0f) {
            val v = (l * 255f).roundToInt()
            return rgb(v, v, v)
        }

        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q

        fun hue2rgb(p: Float, q: Float, tInput: Float): Float {
            var t = tInput
            if (t < 0f) t += 1f
            if (t > 1f) t -= 1f
            return when {
                t < 1f / 6f -> p + (q - p) * 6f * t
                t < 1f / 2f -> q
                t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
                else -> p
            }
        }

        val r = (hue2rgb(p, q, h + 1f / 3f) * 255f).roundToInt()
        val g = (hue2rgb(p, q, h) * 255f).roundToInt()
        val b = (hue2rgb(p, q, h - 1f / 3f) * 255f).roundToInt()
        return rgb(r, g, b)
    }

    fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val max = max(rf, max(gf, bf))
        val min = min(rf, min(gf, bf))
        val delta = max - min

        val s = if (max == 0f) 0f else delta / max
        val v = max

        var h = if (delta == 0f) {
            0f
        } else if (max == rf) {
            ((gf - bf) / delta + (if (gf < bf) 6f else 0f))
        } else if (max == gf) {
            ((bf - rf) / delta + 2f)
        } else {
            ((rf - gf) / delta + 4f)
        }
        h = (h * 60f) % 360f
        if (h < 0f) h += 360f

        return Triple(h, s * 100f, v * 100f)
    }

    fun calculateLuminance(color: Int): Float {
        fun linearize(c: Int): Float {
            val s = c / 255f
            return if (s <= 0.04045f) s / 12.92f else ((s + 0.055f) / 1.055f).pow(2.4f)
        }
        val r = linearize(red(color))
        val g = linearize(green(color))
        val b = linearize(blue(color))
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    /**
     * Converts sRGB color to CIELAB (L*, a*, b*).
     */
    fun toLab(colorInt: Int): Triple<Float, Float, Float> {
        val r = red(colorInt) / 255.0
        val g = green(colorInt) / 255.0
        val b = blue(colorInt) / 255.0

        val rL = if (r <= 0.04045) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
        val gL = if (g <= 0.04045) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
        val bL = if (b <= 0.04045) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

        // D65 standard illuminant
        val x = (rL * 0.4124 + gL * 0.3576 + bL * 0.1805) * 100.0 / 95.047
        val y = (rL * 0.2126 + gL * 0.7152 + bL * 0.0722) * 100.0 / 100.000
        val z = (rL * 0.0193 + gL * 0.1192 + bL * 0.9505) * 100.0 / 108.883

        fun f(t: Double): Double = if (t > 0.008856) t.pow(1.0 / 3.0) else (7.787 * t) + (16.0 / 116.0)

        val fx = f(x)
        val fy = f(y)
        val fz = f(z)

        val l = (116.0 * fy) - 16.0
        val a = 500.0 * (fx - fy)
        val bLab = 200.0 * (fy - fz)

        return Triple(l.toFloat(), a.toFloat(), bLab.toFloat())
    }

    /**
     * Calculates CIELAB Delta E (Euclidean distance) between two colors.
     */
    fun deltaE(color1: Int, color2: Int): Float {
        val lab1 = toLab(color1)
        val lab2 = toLab(color2)
        val dL = lab1.first - lab2.first
        val da = lab1.second - lab2.second
        val db = lab1.third - lab2.third
        return sqrt(dL * dL + da * da + db * db)
    }

    /**
     * WCAG 2.1 Contrast Ratio between two colors.
     */
    fun calculateContrast(foreground: Int, background: Int): ContrastResult {
        val lum1 = calculateLuminance(foreground)
        val lum2 = calculateLuminance(background)
        val l1 = max(lum1, lum2)
        val l2 = min(lum1, lum2)
        val ratio = (l1 + 0.05f) / (l2 + 0.05f)

        return ContrastResult(
            ratio = ratio,
            normalAa = ratio >= 4.5f,
            normalAaa = ratio >= 7.0f,
            largeAa = ratio >= 3.0f,
            largeAaa = ratio >= 4.5f
        )
    }

    fun blend(c1: Int, c2: Int, ratio: Float): Int {
        val inverse = 1f - ratio
        val r = (red(c1) * inverse + red(c2) * ratio).roundToInt().coerceIn(0, 255)
        val g = (green(c1) * inverse + green(c2) * ratio).roundToInt().coerceIn(0, 255)
        val b = (blue(c1) * inverse + blue(c2) * ratio).roundToInt().coerceIn(0, 255)
        return rgb(r, g, b)
    }

    fun generateTintsAndShades(colorInt: Int): Pair<List<Int>, List<Int>> {
        val tints = mutableListOf<Int>()
        val shades = mutableListOf<Int>()

        for (i in 1..5) {
            val ratio = i * 0.15f
            tints.add(blend(colorInt, WHITE, ratio))
            shades.add(blend(colorInt, BLACK, ratio))
        }

        return Pair(tints, shades)
    }

    fun generateHarmonies(colorInt: Int): Map<String, List<Int>> {
        val (baseH, s, l) = rgbToHsl(red(colorInt), green(colorInt), blue(colorInt))

        fun hueShift(degrees: Float): Int {
            val newH = (baseH + degrees + 360f) % 360f
            return hslToRgb(newH, s, l)
        }

        return mapOf(
            "Complementary" to listOf(hueShift(180f)),
            "Analogous" to listOf(hueShift(-30f), hueShift(30f)),
            "Triadic" to listOf(hueShift(120f), hueShift(240f)),
            "Split-Comp" to listOf(hueShift(150f), hueShift(210f))
        )
    }

    fun extractDominantColors(pixels: IntArray, count: Int = 6): List<Int> {
        if (pixels.isEmpty()) return emptyList()
        val step = max(1, pixels.size / 1000)
        val sampled = mutableListOf<Int>()
        for (i in pixels.indices step step) {
            sampled.add(pixels[i])
        }

        val frequencyMap = mutableMapOf<Int, Int>()
        for (p in sampled) {
            val r = (red(p) / 16) * 16
            val g = (green(p) / 16) * 16
            val b = (blue(p) / 16) * 16
            val q = rgb(r, g, b)
            frequencyMap[q] = (frequencyMap[q] ?: 0) + 1
        }

        return frequencyMap.entries
            .sortedByDescending { it.value }
            .take(count)
            .map { it.key }
    }
}
