package app.quacky.feature.compass.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.feature.compass.domain.CompassMath
import app.quacky.feature.compass.domain.MagneticFieldStatus
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
import kotlin.math.abs
import kotlin.math.roundToInt

data class CompassUiState(
    val azimuth: Float = 0f,
    val cardinal: String = "N",
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val isFlat: Boolean = true,
    val isTrueNorth: Boolean = false,
    val declination: Float = 0f,
    val lockedHeading: Float? = null,
    val courseDeviation: Float? = null,
    val magneticMagnitude: Float = 45f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 45f,
    val fieldStatus: MagneticFieldStatus = MagneticFieldStatus.NORMAL_AMBIENT,
    val magneticHistory: List<Float> = emptyList(),
    val sensorAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val needsCalibration: Boolean = false
)

@HiltViewModel
class CompassViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferences
) : ViewModel(), SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _uiState = MutableStateFlow(CompassUiState())
    val uiState: StateFlow<CompassUiState> = _uiState.asStateFlow()

    private val _onCardinalCrossed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val onCardinalCrossed: SharedFlow<Unit> = _onCardinalCrossed.asSharedFlow()

    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var smoothedHeading = 0f
    private var lastCardinalQuadrant = -1
    private val historyPoints = ArrayDeque<Float>(30)

    init {
        viewModelScope.launch {
            preferences.recordToolUsed("compass")
        }
        startListening()
    }

    fun startListening() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val alpha = 0.8f
                gravityValues[0] = gravityValues[0] * alpha + event.values[0] * (1f - alpha)
                gravityValues[1] = gravityValues[1] * alpha + event.values[1] * (1f - alpha)
                gravityValues[2] = gravityValues[2] * alpha + event.values[2] * (1f - alpha)
                hasGravity = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val alpha = 0.8f
                geomagneticValues[0] = geomagneticValues[0] * alpha + event.values[0] * (1f - alpha)
                geomagneticValues[1] = geomagneticValues[1] * alpha + event.values[1] * (1f - alpha)
                geomagneticValues[2] = geomagneticValues[2] * alpha + event.values[2] * (1f - alpha)
                hasGeomagnetic = true

                val magnitude = CompassMath.computeMagneticFieldMagnitude(
                    geomagneticValues[0],
                    geomagneticValues[1],
                    geomagneticValues[2]
                )
                if (historyPoints.size >= 25) {
                    historyPoints.removeFirst()
                }
                historyPoints.addLast(magnitude)
            }
        }

        if (hasGravity && hasGeomagnetic) {
            val orientation = CompassMath.computeOrientation(gravityValues, geomagneticValues)
            if (orientation != null) {
                var rawAzimuth = orientation.azimuthDegrees
                if (_uiState.value.isTrueNorth) {
                    rawAzimuth = CompassMath.applyDeclination(rawAzimuth, _uiState.value.declination)
                }

                smoothedHeading = CompassMath.smoothAngle(smoothedHeading, rawAzimuth, alpha = 0.82f)
                val roundedHeading = (smoothedHeading * 10f).roundToInt() / 10f
                val cardinal = CompassMath.toCardinal(roundedHeading)

                // Check cardinal crossing (N=0, E=90, S=180, W=270)
                val currentQuadrant = ((roundedHeading + 45f) / 90f).toInt() % 4
                val isExactCardinal = abs(roundedHeading % 90f) < 1.5f || abs((roundedHeading % 90f) - 90f) < 1.5f
                if (isExactCardinal && currentQuadrant != lastCardinalQuadrant) {
                    _onCardinalCrossed.tryEmit(Unit)
                    lastCardinalQuadrant = currentQuadrant
                } else if (!isExactCardinal) {
                    lastCardinalQuadrant = currentQuadrant
                }

                val locked = _uiState.value.lockedHeading
                val deviation = if (locked != null) {
                    CompassMath.shortestAngularDelta(target = locked, current = roundedHeading)
                } else {
                    null
                }

                val currentMag = historyPoints.lastOrNull() ?: 45f
                val fieldStatus = CompassMath.classifyFieldStrength(currentMag)

                _uiState.value = _uiState.value.copy(
                    azimuth = roundedHeading,
                    cardinal = cardinal,
                    pitch = (orientation.pitchDegrees * 10f).roundToInt() / 10f,
                    roll = (orientation.rollDegrees * 10f).roundToInt() / 10f,
                    isFlat = orientation.isFlat,
                    courseDeviation = deviation,
                    magneticMagnitude = (currentMag * 10f).roundToInt() / 10f,
                    magX = (geomagneticValues[0] * 10f).roundToInt() / 10f,
                    magY = (geomagneticValues[1] * 10f).roundToInt() / 10f,
                    magZ = (geomagneticValues[2] * 10f).roundToInt() / 10f,
                    fieldStatus = fieldStatus,
                    magneticHistory = historyPoints.toList()
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val needsCalib = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                    accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
            _uiState.value = _uiState.value.copy(
                sensorAccuracy = accuracy,
                needsCalibration = needsCalib
            )
        }
    }

    fun toggleTrueNorth() {
        val nextMode = !_uiState.value.isTrueNorth
        _uiState.value = _uiState.value.copy(isTrueNorth = nextMode)
    }

    fun setDeclination(degrees: Float) {
        _uiState.value = _uiState.value.copy(declination = degrees)
    }

    fun toggleHeadingLock() {
        val currentLocked = _uiState.value.lockedHeading
        if (currentLocked == null) {
            _uiState.value = _uiState.value.copy(
                lockedHeading = _uiState.value.azimuth,
                courseDeviation = 0f
            )
        } else {
            _uiState.value = _uiState.value.copy(
                lockedHeading = null,
                courseDeviation = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
