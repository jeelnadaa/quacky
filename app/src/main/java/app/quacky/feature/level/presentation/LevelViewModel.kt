package app.quacky.feature.level.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.feature.level.domain.LevelMath
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class LevelUiState(
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val tiltAngle: Float = 0f,
    val isLevel: Boolean = false,
    val isFlatMode: Boolean = true,
    val isHeld: Boolean = false,
    val zeroOffsetPitch: Float = 0f,
    val zeroOffsetRoll: Float = 0f
)

@HiltViewModel
class LevelViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferences
) : ViewModel(), SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _uiState = MutableStateFlow(LevelUiState())
    val uiState: StateFlow<LevelUiState> = _uiState.asStateFlow()

    private val _onLevelReached = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val onLevelReached: SharedFlow<Unit> = _onLevelReached.asSharedFlow()

    private var smoothedX = 0f
    private var smoothedY = 0f
    private var smoothedZ = 9.8f
    private var wasLevelLastTick = false

    init {
        viewModelScope.launch {
            preferences.recordToolUsed("spirit_level")
        }
        startListening()
    }

    fun startListening() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || _uiState.value.isHeld) return

        // Low-pass filter for rock-steady readings
        val alpha = 0.82f
        smoothedX = smoothedX * alpha + event.values[0] * (1f - alpha)
        smoothedY = smoothedY * alpha + event.values[1] * (1f - alpha)
        smoothedZ = smoothedZ * alpha + event.values[2] * (1f - alpha)

        val rawPitch = LevelMath.computePitch(smoothedX, smoothedY, smoothedZ)
        val rawRoll = LevelMath.computeRoll(smoothedX, smoothedY, smoothedZ)
        val tilt = LevelMath.computeTiltAngle(smoothedX, smoothedY, smoothedZ)
        val isFlat = LevelMath.isFlatSurface(smoothedZ)

        val adjustedPitch = roundToOneDecimal(rawPitch - _uiState.value.zeroOffsetPitch)
        val adjustedRoll = roundToOneDecimal(rawRoll - _uiState.value.zeroOffsetRoll)
        val isCurrentlyLevel = LevelMath.isLevel(adjustedPitch, adjustedRoll, thresholdDegrees = 0.4f)

        if (isCurrentlyLevel && !wasLevelLastTick) {
            _onLevelReached.tryEmit(Unit)
        }
        wasLevelLastTick = isCurrentlyLevel

        _uiState.value = _uiState.value.copy(
            pitch = adjustedPitch,
            roll = adjustedRoll,
            tiltAngle = roundToOneDecimal(tilt),
            isLevel = isCurrentlyLevel,
            isFlatMode = isFlat
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun toggleHold() {
        _uiState.value = _uiState.value.copy(isHeld = !_uiState.value.isHeld)
    }

    fun calibrateZero() {
        val current = _uiState.value
        _uiState.value = current.copy(
            zeroOffsetPitch = current.pitch + current.zeroOffsetPitch,
            zeroOffsetRoll = current.roll + current.zeroOffsetRoll
        )
    }

    fun resetZero() {
        _uiState.value = _uiState.value.copy(
            zeroOffsetPitch = 0f,
            zeroOffsetRoll = 0f
        )
    }

    private fun roundToOneDecimal(value: Float): Float {
        return (value * 10f).roundToInt() / 10f
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
