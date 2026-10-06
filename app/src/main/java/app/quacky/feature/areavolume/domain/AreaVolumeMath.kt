package app.quacky.feature.areavolume.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class LengthUnit(val symbol: String, val toMeters: Double) {
    MM("mm", 0.001),
    CM("cm", 0.01),
    M("m", 1.0),
    INCH("in", 0.0254),
    FOOT("ft", 0.3048),
    YARD("yd", 0.9144)
}

enum class AreaUnit(val symbol: String, val fromSquareMeters: Double) {
    MM2("mm²", 1_000_000.0),
    CM2("cm²", 10_000.0),
    M2("m²", 1.0),
    IN2("in²", 1550.0031),
    FT2("ft²", 10.76391),
    YD2("yd²", 1.19599),
    ACRE("acre", 0.000247105),
    HECTARE("ha", 0.0001)
}

enum class VolumeUnit(val symbol: String, val fromCubicMeters: Double) {
    ML("mL", 1_000_000.0),
    L("L", 1_000.0),
    M3("m³", 1.0),
    IN3("in³", 61023.744),
    FT3("ft³", 35.314667),
    GALLON_US("gal (US)", 264.17205)
}

data class Point2D(val x: Double, val y: Double)

object AreaVolumeMath {

    // --- Length conversions ---
    fun toMeters(value: Double, unit: LengthUnit): Double = value * unit.toMeters

    fun fromMeters(meters: Double, targetUnit: LengthUnit): Double = meters / targetUnit.toMeters

    // --- Area conversions ---
    fun convertArea(sqMeters: Double, targetUnit: AreaUnit): Double = sqMeters * targetUnit.fromSquareMeters

    fun allAreaUnits(sqMeters: Double, decimals: Int = 2): Map<AreaUnit, String> {
        return AreaUnit.entries.associateWith { unit ->
            formatNumber(convertArea(sqMeters, unit), decimals)
        }
    }

    // --- Volume conversions ---
    fun convertVolume(cubicMeters: Double, targetUnit: VolumeUnit): Double = cubicMeters * targetUnit.fromCubicMeters

    fun allVolumeUnits(cubicMeters: Double, decimals: Int = 2): Map<VolumeUnit, String> {
        return VolumeUnit.entries.associateWith { unit ->
            formatNumber(convertVolume(cubicMeters, unit), decimals)
        }
    }

    fun formatNumber(value: Double, decimals: Int = 2): String {
        if (value.isNaN() || value.isInfinite()) return "—"
        return BigDecimal.valueOf(value)
            .setScale(decimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    // --- 2D Area Solvers (Output in sq meters) ---

    fun rectangle(lengthM: Double, widthM: Double): Pair<Double, Double> {
        if (lengthM <= 0 || widthM <= 0) return Pair(0.0, 0.0)
        val area = lengthM * widthM
        val perimeter = 2 * (lengthM + widthM)
        return Pair(area, perimeter)
    }

    fun square(sideM: Double): Pair<Double, Double> {
        if (sideM <= 0) return Pair(0.0, 0.0)
        return Pair(sideM * sideM, 4 * sideM)
    }

    fun triangleBaseHeight(baseM: Double, heightM: Double): Double {
        if (baseM <= 0 || heightM <= 0) return 0.0
        return 0.5 * baseM * heightM
    }

    fun triangleHeron(aM: Double, bM: Double, cM: Double): Pair<Double, Double>? {
        if (aM <= 0 || bM <= 0 || cM <= 0) return null
        // Triangle inequality check
        if (aM + bM <= cM || aM + cM <= bM || bM + cM <= aM) return null
        val s = (aM + bM + cM) / 2.0
        val area = sqrt(s * (s - aM) * (s - bM) * (s - cM))
        val perimeter = aM + bM + cM
        return Pair(area, perimeter)
    }

    fun triangleSas(aM: Double, bM: Double, angleDeg: Double): Double {
        if (aM <= 0 || bM <= 0 || angleDeg <= 0 || angleDeg >= 180) return 0.0
        val rad = Math.toRadians(angleDeg)
        return 0.5 * aM * bM * sin(rad)
    }

    fun circle(radiusM: Double): Pair<Double, Double> {
        if (radiusM <= 0) return Pair(0.0, 0.0)
        val area = PI * radiusM * radiusM
        val perimeter = 2 * PI * radiusM
        return Pair(area, perimeter)
    }

    fun semicircle(radiusM: Double): Pair<Double, Double> {
        if (radiusM <= 0) return Pair(0.0, 0.0)
        val area = 0.5 * PI * radiusM * radiusM
        val perimeter = PI * radiusM + 2 * radiusM
        return Pair(area, perimeter)
    }

    fun trapezoid(baseAM: Double, baseBM: Double, heightM: Double): Double {
        if (baseAM <= 0 || baseBM <= 0 || heightM <= 0) return 0.0
        return 0.5 * (baseAM + baseBM) * heightM
    }

    fun parallelogram(baseM: Double, heightM: Double): Double {
        if (baseM <= 0 || heightM <= 0) return 0.0
        return baseM * heightM
    }

    fun ellipse(radiusAM: Double, radiusBM: Double): Pair<Double, Double> {
        if (radiusAM <= 0 || radiusBM <= 0) return Pair(0.0, 0.0)
        val area = PI * radiusAM * radiusBM
        // Ramanujan approximation for perimeter
        val h = ((radiusAM - radiusBM) * (radiusAM - radiusBM)) / ((radiusAM + radiusBM) * (radiusAM + radiusBM))
        val perimeter = PI * (radiusAM + radiusBM) * (1 + (3 * h) / (10 + sqrt(4 - 3 * h)))
        return Pair(area, perimeter)
    }

    fun ring(outerRadiusM: Double, innerRadiusM: Double): Double {
        if (outerRadiusM <= 0 || innerRadiusM <= 0 || innerRadiusM >= outerRadiusM) return 0.0
        return PI * (outerRadiusM * outerRadiusM - innerRadiusM * innerRadiusM)
    }

    fun regularPolygon(sides: Int, sideLengthM: Double): Pair<Double, Double> {
        if (sides < 3 || sideLengthM <= 0) return Pair(0.0, 0.0)
        val area = (sides * sideLengthM * sideLengthM) / (4.0 * tan(PI / sides))
        val perimeter = sides * sideLengthM
        return Pair(area, perimeter)
    }

    fun shoelace(points: List<Point2D>): Pair<Double, Double> {
        if (points.size < 3) return Pair(0.0, 0.0)
        var sum = 0.0
        var perimeter = 0.0
        val n = points.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            sum += (points[i].x * points[j].y) - (points[j].x * points[i].y)
            val dx = points[j].x - points[i].x
            val dy = points[j].y - points[i].y
            perimeter += sqrt(dx * dx + dy * dy)
        }
        val area = abs(sum) / 2.0
        return Pair(area, perimeter)
    }

    // --- 3D Volume Solvers (Output in cubic meters) ---

    fun cube(sideM: Double): Pair<Double, Double> {
        if (sideM <= 0) return Pair(0.0, 0.0)
        val volume = sideM * sideM * sideM
        val surfaceArea = 6 * sideM * sideM
        return Pair(volume, surfaceArea)
    }

    fun cuboid(lengthM: Double, widthM: Double, heightM: Double): Pair<Double, Double> {
        if (lengthM <= 0 || widthM <= 0 || heightM <= 0) return Pair(0.0, 0.0)
        val volume = lengthM * widthM * heightM
        val surfaceArea = 2 * (lengthM * widthM + lengthM * heightM + widthM * heightM)
        return Pair(volume, surfaceArea)
    }

    fun cylinder(radiusM: Double, heightM: Double): Pair<Double, Double> {
        if (radiusM <= 0 || heightM <= 0) return Pair(0.0, 0.0)
        val volume = PI * radiusM * radiusM * heightM
        val surfaceArea = 2 * PI * radiusM * heightM + 2 * PI * radiusM * radiusM
        return Pair(volume, surfaceArea)
    }

    fun cone(radiusM: Double, heightM: Double): Pair<Double, Double> {
        if (radiusM <= 0 || heightM <= 0) return Pair(0.0, 0.0)
        val volume = (1.0 / 3.0) * PI * radiusM * radiusM * heightM
        val slant = sqrt(radiusM * radiusM + heightM * heightM)
        val surfaceArea = PI * radiusM * (radiusM + slant)
        return Pair(volume, surfaceArea)
    }

    fun sphere(radiusM: Double): Pair<Double, Double> {
        if (radiusM <= 0) return Pair(0.0, 0.0)
        val volume = (4.0 / 3.0) * PI * radiusM * radiusM * radiusM
        val surfaceArea = 4 * PI * radiusM * radiusM
        return Pair(volume, surfaceArea)
    }

    fun hemisphere(radiusM: Double): Pair<Double, Double> {
        if (radiusM <= 0) return Pair(0.0, 0.0)
        val volume = (2.0 / 3.0) * PI * radiusM * radiusM * radiusM
        val surfaceArea = 3 * PI * radiusM * radiusM
        return Pair(volume, surfaceArea)
    }

    fun pyramid(lengthM: Double, widthM: Double, heightM: Double): Double {
        if (lengthM <= 0 || widthM <= 0 || heightM <= 0) return 0.0
        return (1.0 / 3.0) * lengthM * widthM * heightM
    }

    fun triangularPrism(baseM: Double, triangleHeightM: Double, prismLengthM: Double): Double {
        if (baseM <= 0 || triangleHeightM <= 0 || prismLengthM <= 0) return 0.0
        val baseArea = 0.5 * baseM * triangleHeightM
        return baseArea * prismLengthM
    }

    fun capsule(radiusM: Double, cylinderHeightM: Double): Pair<Double, Double> {
        if (radiusM <= 0 || cylinderHeightM <= 0) return Pair(0.0, 0.0)
        val sphereVol = (4.0 / 3.0) * PI * radiusM * radiusM * radiusM
        val cylVol = PI * radiusM * radiusM * cylinderHeightM
        val volume = sphereVol + cylVol
        val surfaceArea = 2 * PI * radiusM * (2 * radiusM + cylinderHeightM)
        return Pair(volume, surfaceArea)
    }

    // --- Estimators ---

    fun estimatePaint(
        wallWidthM: Double,
        wallHeightM: Double,
        openingsAreaM2: Double = 0.0,
        coats: Int = 2,
        coveragePerLitreM2: Double = 10.0
    ): Double {
        val grossArea = wallWidthM * wallHeightM
        val netArea = (grossArea - openingsAreaM2).coerceAtLeast(0.0)
        val totalAreaToPaint = netArea * coats
        return totalAreaToPaint / coveragePerLitreM2.coerceAtLeast(0.1)
    }

    fun estimateTiles(
        roomLengthM: Double,
        roomWidthM: Double,
        tileWidthM: Double,
        tileLengthM: Double,
        wastagePercent: Double = 10.0
    ): Pair<Int, Double> {
        val roomArea = roomLengthM * roomWidthM
        val tileArea = tileWidthM * tileLengthM
        if (tileArea <= 0.0) return Pair(0, 0.0)
        val grossArea = roomArea * (1.0 + wastagePercent / 100.0)
        val count = kotlin.math.ceil(grossArea / tileArea).toInt()
        return Pair(count, grossArea)
    }

    fun estimateConcrete(
        lengthM: Double,
        widthM: Double,
        thicknessM: Double
    ): Pair<Double, Int> {
        val volumeM3 = lengthM * widthM * thicknessM
        // Approximate standard 25kg pre-mixed concrete bags (~108 bags per m³)
        val bags = kotlin.math.ceil(volumeM3 * 108.0).toInt()
        return Pair(volumeM3, bags)
    }

    fun estimateTank(
        isCylindrical: Boolean,
        dim1M: Double, // radius or length
        dim2M: Double, // height or width
        dim3M: Double = 0.0 // height if rectangular
    ): Double {
        val volM3 = if (isCylindrical) {
            PI * dim1M * dim1M * dim2M
        } else {
            dim1M * dim2M * dim3M
        }
        return volM3 * 1000.0 // 1 m³ = 1000 Litres
    }
}
