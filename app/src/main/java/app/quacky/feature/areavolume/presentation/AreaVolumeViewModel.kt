package app.quacky.feature.areavolume.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.capability.ArCoreStatus
import app.quacky.core.capability.DeviceCapabilities
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.repository.HistoryRepository
import app.quacky.feature.areavolume.domain.AreaUnit
import app.quacky.feature.areavolume.domain.AreaVolumeMath
import app.quacky.feature.areavolume.domain.LengthUnit
import app.quacky.feature.areavolume.domain.Point2D
import app.quacky.feature.areavolume.domain.VolumeUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class CalculationResult(
    val primaryLabel: String = "",
    val primaryValue: String = "",
    val primaryUnit: String = "",
    val secondaryLabel: String? = null,
    val secondaryValue: String? = null,
    val secondaryUnit: String? = null,
    val allUnits: Map<String, String> = emptyMap(),
    val isValid: Boolean = true,
    val errorMessage: String? = null
)

data class AreaVolumeUiState(
    val activeTab: Int = 0, // 0 = Area, 1 = Volume, 2 = Estimate
    val activeShape: String = "rectangle",
    val inputs: Map<String, String> = mapOf("length" to "5", "width" to "3", "side" to "4", "radius" to "2", "height" to "4"),
    val units: Map<String, LengthUnit> = mapOf(
        "length" to LengthUnit.M,
        "width" to LengthUnit.M,
        "height" to LengthUnit.M,
        "side" to LengthUnit.M,
        "radius" to LengthUnit.M
    ),
    val selectedAreaUnit: AreaUnit = AreaUnit.M2,
    val selectedVolumeUnit: VolumeUnit = VolumeUnit.M3,
    val irregularPoints: List<Point2D> = emptyList(),
    val showAllUnits: Boolean = false,
    val canImportAr: Boolean = false,
    val availableArLengths: List<Double> = emptyList(),
    val result: CalculationResult = CalculationResult()
)

@HiltViewModel
class AreaVolumeViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val capabilities: DeviceCapabilities
) : ViewModel() {

    private val _uiState = MutableStateFlow(AreaVolumeUiState())
    val uiState: StateFlow<AreaVolumeUiState> = _uiState.asStateFlow()

    init {
        checkArAvailability()
        recalculate()
    }

    private fun checkArAvailability() {
        viewModelScope.launch {
            val arReady = capabilities.arCoreStatus == ArCoreStatus.SUPPORTED_AND_READY
            if (arReady) {
                // Check if any AR Ruler measurements exist in history
                val arHistory = (historyRepository.getHistoryForTool(ToolRegistry.AR_RULER.id).firstOrNull() ?: emptyList()) +
                    (historyRepository.getHistoryForTool("arruler").firstOrNull() ?: emptyList())
                val lengths = mutableListOf<Double>()
                for (entry in arHistory) {
                    try {
                        val json = JSONObject(entry.payloadJson)
                        if (json.has("valueMeters")) {
                            lengths.add(json.getDouble("valueMeters"))
                        }
                    } catch (_: Exception) {}
                }
                _uiState.update {
                    it.copy(
                        canImportAr = lengths.isNotEmpty(),
                        availableArLengths = lengths
                    )
                }
            } else {
                _uiState.update { it.copy(canImportAr = false) }
            }
        }
    }

    fun selectTab(tab: Int) {
        val defaultShape = when (tab) {
            0 -> "rectangle"
            1 -> "cuboid"
            else -> "paint"
        }
        _uiState.update { it.copy(activeTab = tab, activeShape = defaultShape) }
        recalculate()
    }

    fun selectShape(shapeKey: String) {
        _uiState.update { it.copy(activeShape = shapeKey) }
        recalculate()
    }

    fun updateInput(fieldKey: String, value: String) {
        _uiState.update {
            val updated = it.inputs.toMutableMap()
            updated[fieldKey] = value
            it.copy(inputs = updated)
        }
        recalculate()
    }

    fun updateUnit(fieldKey: String, unit: LengthUnit) {
        _uiState.update {
            val updated = it.units.toMutableMap()
            updated[fieldKey] = unit
            it.copy(units = updated)
        }
        recalculate()
    }

    fun toggleShowAllUnits() {
        _uiState.update { it.copy(showAllUnits = !it.showAllUnits) }
    }

    fun addIrregularPoint(p: Point2D) {
        _uiState.update { it.copy(irregularPoints = it.irregularPoints + p) }
        recalculate()
    }

    fun clearIrregularPoints() {
        _uiState.update { it.copy(irregularPoints = emptyList()) }
        recalculate()
    }

    fun undoIrregularPoint() {
        _uiState.update {
            if (it.irregularPoints.isNotEmpty()) {
                it.copy(irregularPoints = it.irregularPoints.dropLast(1))
            } else it
        }
        recalculate()
    }

    fun importArMeasurement(fieldKey: String) {
        val lengths = _uiState.value.availableArLengths
        if (lengths.isNotEmpty()) {
            val lengthM = lengths.first()
            val unit = _uiState.value.units[fieldKey] ?: LengthUnit.M
            val converted = AreaVolumeMath.fromMeters(lengthM, unit)
            updateInput(fieldKey, "%.2f".format(converted))
        }
    }

    fun saveCalculation() {
        viewModelScope.launch {
            val state = _uiState.value
            val res = state.result
            if (!res.isValid) return@launch

            val payload = JSONObject().apply {
                put("tab", state.activeTab)
                put("shape", state.activeShape)
                put("primary", "${res.primaryValue} ${res.primaryUnit}")
                res.secondaryValue?.let { put("secondary", "$it ${res.secondaryUnit}") }
            }.toString()

            val tabName = when (state.activeTab) {
                0 -> "Area"
                1 -> "Volume"
                else -> "Estimate"
            }

            historyRepository.addEntry(
                toolId = "area_volume",
                type = tabName,
                title = "${state.activeShape.replaceFirstChar { it.uppercase() }} ($tabName)",
                subtitle = "${res.primaryLabel}: ${res.primaryValue} ${res.primaryUnit}",
                payloadJson = payload,
                thumbnailBytes = null
            )
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val inputs = state.inputs
        val units = state.units

        fun getM(key: String, default: Double = 0.0): Double {
            val raw = inputs[key]?.toDoubleOrNull() ?: default
            val unit = units[key] ?: LengthUnit.M
            return AreaVolumeMath.toMeters(raw, unit)
        }

        val res = when (state.activeTab) {
            0 -> calculateArea(state.activeShape, ::getM, state.irregularPoints, state.selectedAreaUnit)
            1 -> calculateVolume(state.activeShape, ::getM, state.selectedVolumeUnit)
            else -> calculateEstimate(state.activeShape, ::getM)
        }

        _uiState.update { it.copy(result = res) }
    }

    private fun calculateArea(
        shape: String,
        getM: (String, Double) -> Double,
        points: List<Point2D>,
        targetUnit: AreaUnit
    ): CalculationResult {
        var areaM2 = 0.0
        var perimeterM: Double? = null

        when (shape) {
            "rectangle" -> {
                val (a, p) = AreaVolumeMath.rectangle(getM("length", 1.0), getM("width", 1.0))
                areaM2 = a
                perimeterM = p
            }
            "square" -> {
                val (a, p) = AreaVolumeMath.square(getM("side", 1.0))
                areaM2 = a
                perimeterM = p
            }
            "triangle" -> {
                areaM2 = AreaVolumeMath.triangleBaseHeight(getM("base", 1.0), getM("height", 1.0))
            }
            "circle" -> {
                val (a, p) = AreaVolumeMath.circle(getM("radius", 1.0))
                areaM2 = a
                perimeterM = p
            }
            "semicircle" -> {
                val (a, p) = AreaVolumeMath.semicircle(getM("radius", 1.0))
                areaM2 = a
                perimeterM = p
            }
            "trapezoid" -> {
                areaM2 = AreaVolumeMath.trapezoid(getM("baseA", 1.0), getM("baseB", 1.0), getM("height", 1.0))
            }
            "parallelogram" -> {
                areaM2 = AreaVolumeMath.parallelogram(getM("base", 1.0), getM("height", 1.0))
            }
            "ellipse" -> {
                val (a, p) = AreaVolumeMath.ellipse(getM("radiusA", 1.0), getM("radiusB", 1.0))
                areaM2 = a
                perimeterM = p
            }
            "ring" -> {
                areaM2 = AreaVolumeMath.ring(getM("outerRadius", 2.0), getM("innerRadius", 1.0))
            }
            "irregular_polygon" -> {
                if (points.size >= 3) {
                    val (a, p) = AreaVolumeMath.shoelace(points)
                    areaM2 = a
                    perimeterM = p
                }
            }
        }

        val convertedArea = AreaVolumeMath.convertArea(areaM2, targetUnit)
        val allUnits = AreaVolumeMath.allAreaUnits(areaM2).mapKeys { it.key.symbol }

        return CalculationResult(
            primaryLabel = "Area",
            primaryValue = AreaVolumeMath.formatNumber(convertedArea),
            primaryUnit = targetUnit.symbol,
            secondaryLabel = perimeterM?.let { "Perimeter" },
            secondaryValue = perimeterM?.let { AreaVolumeMath.formatNumber(it) },
            secondaryUnit = perimeterM?.let { "m" },
            allUnits = allUnits,
            isValid = areaM2 > 0.0
        )
    }

    private fun calculateVolume(
        shape: String,
        getM: (String, Double) -> Double,
        targetUnit: VolumeUnit
    ): CalculationResult {
        var volM3 = 0.0
        var surfaceAreaM2: Double? = null

        when (shape) {
            "cube" -> {
                val (v, sa) = AreaVolumeMath.cube(getM("side", 1.0))
                volM3 = v
                surfaceAreaM2 = sa
            }
            "cuboid" -> {
                val (v, sa) = AreaVolumeMath.cuboid(getM("length", 1.0), getM("width", 1.0), getM("height", 1.0))
                volM3 = v
                surfaceAreaM2 = sa
            }
            "cylinder" -> {
                val (v, sa) = AreaVolumeMath.cylinder(getM("radius", 1.0), getM("height", 1.0))
                volM3 = v
                surfaceAreaM2 = sa
            }
            "cone" -> {
                val (v, sa) = AreaVolumeMath.cone(getM("radius", 1.0), getM("height", 1.0))
                volM3 = v
                surfaceAreaM2 = sa
            }
            "sphere" -> {
                val (v, sa) = AreaVolumeMath.sphere(getM("radius", 1.0))
                volM3 = v
                surfaceAreaM2 = sa
            }
            "hemisphere" -> {
                val (v, sa) = AreaVolumeMath.hemisphere(getM("radius", 1.0))
                volM3 = v
                surfaceAreaM2 = sa
            }
            "pyramid" -> {
                volM3 = AreaVolumeMath.pyramid(getM("length", 1.0), getM("width", 1.0), getM("height", 1.0))
            }
            "capsule" -> {
                val (v, sa) = AreaVolumeMath.capsule(getM("radius", 1.0), getM("height", 1.0))
                volM3 = v
                surfaceAreaM2 = sa
            }
        }

        val convertedVol = AreaVolumeMath.convertVolume(volM3, targetUnit)
        val allUnits = AreaVolumeMath.allVolumeUnits(volM3).mapKeys { it.key.symbol }

        return CalculationResult(
            primaryLabel = "Volume",
            primaryValue = AreaVolumeMath.formatNumber(convertedVol),
            primaryUnit = targetUnit.symbol,
            secondaryLabel = surfaceAreaM2?.let { "Surface Area" },
            secondaryValue = surfaceAreaM2?.let { AreaVolumeMath.formatNumber(it) },
            secondaryUnit = surfaceAreaM2?.let { "m²" },
            allUnits = allUnits,
            isValid = volM3 > 0.0
        )
    }

    private fun calculateEstimate(
        estimator: String,
        getM: (String, Double) -> Double
    ): CalculationResult {
        return when (estimator) {
            "paint" -> {
                val litres = AreaVolumeMath.estimatePaint(
                    wallWidthM = getM("length", 5.0),
                    wallHeightM = getM("height", 2.8),
                    coats = 2,
                    coveragePerLitreM2 = 10.0
                )
                CalculationResult(
                    primaryLabel = "Paint needed",
                    primaryValue = AreaVolumeMath.formatNumber(litres, 1),
                    primaryUnit = "Litres (2 coats)"
                )
            }
            "tile" -> {
                val (count, grossArea) = AreaVolumeMath.estimateTiles(
                    roomLengthM = getM("length", 4.0),
                    roomWidthM = getM("width", 3.0),
                    tileWidthM = getM("tileWidth", 0.3),
                    tileLengthM = getM("tileLength", 0.3),
                    wastagePercent = 10.0
                )
                CalculationResult(
                    primaryLabel = "Tiles needed",
                    primaryValue = count.toString(),
                    primaryUnit = "pieces",
                    secondaryLabel = "Gross coverage",
                    secondaryValue = AreaVolumeMath.formatNumber(grossArea, 1),
                    secondaryUnit = "m² (+10% wastage)"
                )
            }
            "concrete" -> {
                val (volM3, bags) = AreaVolumeMath.estimateConcrete(
                    lengthM = getM("length", 3.0),
                    widthM = getM("width", 2.0),
                    thicknessM = getM("thickness", 0.1)
                )
                CalculationResult(
                    primaryLabel = "Concrete volume",
                    primaryValue = AreaVolumeMath.formatNumber(volM3, 2),
                    primaryUnit = "m³",
                    secondaryLabel = "25kg Premix Bags",
                    secondaryValue = bags.toString(),
                    secondaryUnit = "bags (~108/m³)"
                )
            }
            else -> { // water tank
                val litres = AreaVolumeMath.estimateTank(
                    isCylindrical = true,
                    dim1M = getM("radius", 1.0),
                    dim2M = getM("height", 2.0)
                )
                CalculationResult(
                    primaryLabel = "Capacity",
                    primaryValue = AreaVolumeMath.formatNumber(litres, 0),
                    primaryUnit = "Litres"
                )
            }
        }
    }
}
