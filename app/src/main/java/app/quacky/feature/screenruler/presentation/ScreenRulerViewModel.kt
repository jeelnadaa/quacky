package app.quacky.feature.screenruler.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.capability.DeviceCapabilities
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.data.repository.HistoryRepository
import app.quacky.feature.screenruler.domain.RulerMath
import app.quacky.feature.screenruler.domain.RulerUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CalibrationMethod {
    CREDIT_CARD,
    KNOWN_LENGTH
}

data class ScreenRulerUiState(
    val calibrationFactor: Float = 1.0f,
    val isCalibrated: Boolean = false,
    val isMetricsPlausible: Boolean = true,
    val isLocked: Boolean = false,
    val isFlipped: Boolean = false,
    val showGrid: Boolean = false,
    val selectedUnit: RulerUnit = RulerUnit.MM,
    val marker1Fraction: Float = 0.25f,
    val marker2Fraction: Float = 0.65f,
    val showCalibrationDialog: Boolean = false,
    val calibrationMethod: CalibrationMethod = CalibrationMethod.CREDIT_CARD,
    val tempCalibrationFactor: Float = 1.0f,
    val showSaveMeasurementDialog: Boolean = false,
    val saveSuccessMessage: String? = null,
    val showInfoTipDialog: Boolean = false
)

@HiltViewModel
class ScreenRulerViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val historyRepository: HistoryRepository,
    private val capabilities: DeviceCapabilities
) : ViewModel() {

    private val isMetricsPlausible = capabilities.isDisplayMetricsPlausible

    private val _isFlipped = MutableStateFlow(false)
    private val _showGrid = MutableStateFlow(false)
    private val _selectedUnit = MutableStateFlow(RulerUnit.MM)
    private val _marker1Fraction = MutableStateFlow(0.25f)
    private val _marker2Fraction = MutableStateFlow(0.65f)
    private val _showCalibrationDialog = MutableStateFlow(false)
    private val _calibrationMethod = MutableStateFlow(CalibrationMethod.CREDIT_CARD)
    private val _tempCalibrationFactor = MutableStateFlow(1.0f)
    private val _showSaveMeasurementDialog = MutableStateFlow(false)
    private val _saveSuccessMessage = MutableStateFlow<String?>(null)
    private val _showInfoTipDialog = MutableStateFlow(false)

    // Intermediate state groups to keep combine parameters <= 5
    private data class RulerDisplayConfig(
        val isFlipped: Boolean,
        val showGrid: Boolean,
        val selectedUnit: RulerUnit,
        val marker1Fraction: Float,
        val marker2Fraction: Float
    )

    private data class RulerDialogConfig(
        val showCalibrationDialog: Boolean,
        val calibrationMethod: CalibrationMethod,
        val tempCalibrationFactor: Float,
        val showSaveMeasurementDialog: Boolean,
        val saveSuccessMessage: String?,
        val showInfoTipDialog: Boolean
    )

    private val displayConfig = combine(
        _isFlipped,
        _showGrid,
        _selectedUnit,
        _marker1Fraction,
        _marker2Fraction
    ) { flipped, grid, unit, m1, m2 ->
        RulerDisplayConfig(flipped, grid, unit, m1, m2)
    }

    private val dialogConfig = combine(
        _showCalibrationDialog,
        _calibrationMethod,
        _tempCalibrationFactor,
        _showSaveMeasurementDialog,
        _saveSuccessMessage
    ) { showCal, method, tempFactor, showSave, saveMsg ->
        // combine with info tip
        Pair(showCal, Pair(method, Pair(tempFactor, Pair(showSave, saveMsg))))
    }

    val uiState: StateFlow<ScreenRulerUiState> = combine(
        preferences.rulerCalibrationFactor,
        preferences.isRulerCalibrated,
        displayConfig,
        dialogConfig,
        _showInfoTipDialog
    ) { factor, isCalibrated, display, dialog, showTip ->
        val isLocked = !isMetricsPlausible && !isCalibrated
        val (showCal, rest1) = dialog
        val (method, rest2) = rest1
        val (tempFactor, rest3) = rest2
        val (showSave, saveMsg) = rest3

        ScreenRulerUiState(
            calibrationFactor = factor,
            isCalibrated = isCalibrated,
            isMetricsPlausible = isMetricsPlausible,
            isLocked = isLocked,
            isFlipped = display.isFlipped,
            showGrid = display.showGrid,
            selectedUnit = display.selectedUnit,
            marker1Fraction = display.marker1Fraction,
            marker2Fraction = display.marker2Fraction,
            showCalibrationDialog = showCal,
            calibrationMethod = method,
            tempCalibrationFactor = tempFactor,
            showSaveMeasurementDialog = showSave,
            saveSuccessMessage = saveMsg,
            showInfoTipDialog = showTip
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScreenRulerUiState(
            isMetricsPlausible = isMetricsPlausible,
            isLocked = !isMetricsPlausible
        )
    )

    init {
        viewModelScope.launch {
            preferences.recordToolUsed("screen_ruler")
        }
    }

    fun toggleFlipped() {
        _isFlipped.value = !_isFlipped.value
    }

    fun toggleGrid() {
        _showGrid.value = !_showGrid.value
    }

    fun setSelectedUnit(unit: RulerUnit) {
        _selectedUnit.value = unit
    }

    fun updateMarker1Fraction(fraction: Float) {
        _marker1Fraction.value = fraction.coerceIn(0f, 1f)
    }

    fun updateMarker2Fraction(fraction: Float) {
        _marker2Fraction.value = fraction.coerceIn(0f, 1f)
    }

    fun openCalibration() {
        _tempCalibrationFactor.value = uiState.value.calibrationFactor
        _showCalibrationDialog.value = true
    }

    fun closeCalibration() {
        _showCalibrationDialog.value = false
    }

    fun setCalibrationMethod(method: CalibrationMethod) {
        _calibrationMethod.value = method
    }

    fun updateTempCalibrationFactor(factor: Float) {
        _tempCalibrationFactor.value = factor.coerceIn(0.5f, 2.0f)
    }

    fun saveCalibration() {
        val factor = _tempCalibrationFactor.value
        viewModelScope.launch {
            preferences.setRulerCalibration(factor)
            _showCalibrationDialog.value = false
        }
    }

    fun resetCalibration() {
        viewModelScope.launch {
            preferences.resetRulerCalibration()
            _tempCalibrationFactor.value = 1.0f
        }
    }

    fun openSaveMeasurementDialog() {
        _showSaveMeasurementDialog.value = true
    }

    fun closeSaveMeasurementDialog() {
        _showSaveMeasurementDialog.value = false
    }

    fun setShowInfoTip(show: Boolean) {
        _showInfoTipDialog.value = show
    }

    fun saveMeasurement(label: String, distanceMm: Float, formatted: String) {
        viewModelScope.launch {
            val title = if (label.isNotBlank()) {
                "$formatted ($label)"
            } else {
                formatted
            }
            historyRepository.addEntry(
                toolId = "screen_ruler",
                type = "MEASUREMENT",
                title = title,
                subtitle = "%.2f mm".format(distanceMm),
                payloadJson = "{\"distanceMm\":$distanceMm,\"formatted\":\"$formatted\",\"label\":\"$label\"}"
            )
            _showSaveMeasurementDialog.value = false
            _saveSuccessMessage.value = "Measurement saved to history"
        }
    }

    fun clearSaveSuccess() {
        _saveSuccessMessage.value = null
    }
}
