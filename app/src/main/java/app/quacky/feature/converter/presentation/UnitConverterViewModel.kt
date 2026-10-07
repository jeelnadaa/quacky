package app.quacky.feature.converter.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.feature.converter.domain.UnitConverterEngine
import app.quacky.feature.converter.model.UnitCategory
import app.quacky.feature.converter.model.UnitItem
import app.quacky.feature.converter.model.UnitRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConvertedUnitResult(
    val unit: UnitItem,
    val displayValue: String,
    val numericValue: Double
)

data class UnitConverterUiState(
    val category: UnitCategory = UnitCategory.LENGTH,
    val availableUnits: List<UnitItem> = UnitRegistry.LENGTH_UNITS,
    val fromUnit: UnitItem = UnitRegistry.LENGTH_UNITS.first { it.id == "m" },
    val toUnit: UnitItem = UnitRegistry.LENGTH_UNITS.first { it.id == "ft" },
    val inputString: String = "1",
    val outputString: String = "3.28084",
    val allBreakdown: List<ConvertedUnitResult> = emptyList()
)

@HiltViewModel
class UnitConverterViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitConverterUiState())
    val uiState: StateFlow<UnitConverterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.recordToolUsed("unit_converter")
        }
        recalculate()
    }

    fun selectCategory(category: UnitCategory) {
        val units = UnitRegistry.getUnitsForCategory(category)
        val defaultFrom = units.getOrNull(0) ?: return
        val defaultTo = units.getOrNull(1) ?: defaultFrom

        _uiState.value = _uiState.value.copy(
            category = category,
            availableUnits = units,
            fromUnit = defaultFrom,
            toUnit = defaultTo
        )
        recalculate()
    }

    fun selectFromUnit(unit: UnitItem) {
        _uiState.value = _uiState.value.copy(fromUnit = unit)
        recalculate()
    }

    fun selectToUnit(unit: UnitItem) {
        _uiState.value = _uiState.value.copy(toUnit = unit)
        recalculate()
    }

    fun swapUnits() {
        val current = _uiState.value
        _uiState.value = current.copy(
            fromUnit = current.toUnit,
            toUnit = current.fromUnit
        )
        recalculate()
    }

    fun onDigitClick(digit: String) {
        val current = _uiState.value.inputString
        val next = if (current == "0") digit else current + digit
        if (next.length <= 15) {
            _uiState.value = _uiState.value.copy(inputString = next)
            recalculate()
        }
    }

    fun onDotClick() {
        val current = _uiState.value.inputString
        if (!current.contains('.')) {
            val next = if (current.isEmpty()) "0." else "$current."
            _uiState.value = _uiState.value.copy(inputString = next)
            recalculate()
        }
    }

    fun onToggleSign() {
        val current = _uiState.value.inputString
        if (current == "0" || current.isEmpty()) return
        val next = if (current.startsWith('-')) current.substring(1) else "-$current"
        _uiState.value = _uiState.value.copy(inputString = next)
        recalculate()
    }

    fun onBackspace() {
        val current = _uiState.value.inputString
        val next = if (current.length <= 1 || (current.length == 2 && current.startsWith('-'))) {
            "0"
        } else {
            current.dropLast(1)
        }
        _uiState.value = _uiState.value.copy(inputString = next)
        recalculate()
    }

    fun onClear() {
        _uiState.value = _uiState.value.copy(inputString = "0")
        recalculate()
    }

    fun setInputString(raw: String) {
        val clean = raw.trim().replace(',', '.')
        if (clean.isEmpty()) {
            _uiState.value = _uiState.value.copy(inputString = "0")
        } else {
            _uiState.value = _uiState.value.copy(inputString = clean)
        }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val parsedInput = state.inputString.toDoubleOrNull() ?: 0.0

        val convertedTarget = UnitConverterEngine.convert(parsedInput, state.fromUnit, state.toUnit)
        val formattedTarget = UnitConverterEngine.formatDisplay(convertedTarget)

        val breakdown = state.availableUnits.map { unit ->
            val result = UnitConverterEngine.convert(parsedInput, state.fromUnit, unit)
            ConvertedUnitResult(
                unit = unit,
                displayValue = UnitConverterEngine.formatDisplay(result),
                numericValue = result
            )
        }

        _uiState.value = state.copy(
            outputString = formattedTarget,
            allBreakdown = breakdown
        )
    }
}
