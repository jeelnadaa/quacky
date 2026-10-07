package app.quacky.feature.surfer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class SurferUiStatus {
    READY,
    COUNTDOWN,
    RUNNING,
    PAUSED,
    CRASHED,
    GAME_OVER
}

data class SurferUiState(
    val status: SurferUiStatus = SurferUiStatus.READY,
    val countdown: Int = 3,
    val score: Int = 0,
    val breadcrumbs: Int = 0,
    val distanceMeters: Float = 0f,
    val highScore: Int = 0,
    val totalCoins: Int = 0,
    val maxDistance: Int = 0,
    val isNewHighScore: Boolean = false,
    val crashReason: String = "",
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showSwipeHints: Boolean = true,
    val isEngineSupported: Boolean = true,
    val isReadOnlyResult: Boolean = false
)

sealed interface SurferGameHapticEvent {
    object Tick : SurferGameHapticEvent
    object Click : SurferGameHapticEvent
    object Heavy : SurferGameHapticEvent
}

@HiltViewModel
class QuackySurferViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SurferUiState())
    val state: StateFlow<SurferUiState> = _state.asStateFlow()

    private val _hapticEvents = MutableSharedFlow<SurferGameHapticEvent>(extraBufferCapacity = 16)
    val hapticEvents: SharedFlow<SurferGameHapticEvent> = _hapticEvents.asSharedFlow()

    private var countdownJob: Job? = null
    private var crashJob: Job? = null

    init {
        viewModelScope.launch {
            val high = preferences.surferHighScore.first()
            val totalCoins = preferences.surferTotalCoins.first()
            val maxDist = preferences.surferMaxDistance.first()
            val sound = preferences.isSoundEnabled.first()
            val haptic = preferences.isHapticsEnabled.first()

            _state.value = _state.value.copy(
                highScore = high,
                totalCoins = totalCoins,
                maxDistance = maxDist,
                soundEnabled = sound,
                hapticsEnabled = haptic
            )
        }
    }

    fun startCountdown(onCountdownComplete: () -> Unit) {
        if (_state.value.status != SurferUiStatus.READY) return
        countdownJob?.cancel()
        _state.value = _state.value.copy(
            status = SurferUiStatus.COUNTDOWN,
            countdown = 3
        )

        countdownJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _state.value = _state.value.copy(countdown = i)
                if (_state.value.hapticsEnabled) {
                    _hapticEvents.tryEmit(SurferGameHapticEvent.Tick)
                }
                delay(1000L)
            }
            _state.value = _state.value.copy(status = SurferUiStatus.RUNNING)
            onCountdownComplete()
        }
    }

    fun onStatsUpdate(distance: Float, breadcrumbs: Int, score: Int) {
        if (_state.value.status == SurferUiStatus.RUNNING) {
            _state.value = _state.value.copy(
                distanceMeters = distance,
                breadcrumbs = breadcrumbs,
                score = score
            )
        }
    }

    fun onBreadcrumbCollected(count: Int) {
        if (_state.value.hapticsEnabled) {
            _hapticEvents.tryEmit(SurferGameHapticEvent.Tick)
        }
    }

    fun onCrash(reason: String, distance: Float, breadcrumbs: Int, score: Int) {
        if (_state.value.status == SurferUiStatus.CRASHED || _state.value.status == SurferUiStatus.GAME_OVER) return

        _state.value = _state.value.copy(
            status = SurferUiStatus.CRASHED,
            crashReason = reason,
            distanceMeters = distance,
            breadcrumbs = breadcrumbs,
            score = score
        )

        if (_state.value.hapticsEnabled) {
            _hapticEvents.tryEmit(SurferGameHapticEvent.Heavy)
        }

        crashJob?.cancel()
        crashJob = viewModelScope.launch {
            // Wait 0.9s for crash tumble animation to finish
            delay(900L)

            val isNewRecord = preferences.recordSurferGameResult(
                score = score,
                coins = breadcrumbs,
                distance = distance.toInt()
            )

            val updatedHigh = preferences.surferHighScore.first()
            val updatedCoins = preferences.surferTotalCoins.first()
            val updatedMaxDist = preferences.surferMaxDistance.first()

            // Save to Quacky History
            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())
            historyRepository.addEntry(
                toolId = ToolRegistry.QUACKY_SURFER.id,
                type = "surfer_run",
                title = "Run · ${distance.toInt()} m · $score pts",
                subtitle = "$breadcrumbs breadcrumbs · $dateStr",
                payloadJson = JSONObject().apply {
                    put("score", score)
                    put("distance", distance.toInt())
                    put("breadcrumbs", breadcrumbs)
                }.toString()
            )

            _state.value = _state.value.copy(
                status = SurferUiStatus.GAME_OVER,
                isNewHighScore = isNewRecord,
                highScore = updatedHigh,
                totalCoins = updatedCoins,
                maxDistance = updatedMaxDist
            )
        }
    }

    fun pause() {
        if (_state.value.status == SurferUiStatus.RUNNING) {
            _state.value = _state.value.copy(status = SurferUiStatus.PAUSED)
        }
    }

    fun resume() {
        if (_state.value.status == SurferUiStatus.PAUSED) {
            _state.value = _state.value.copy(status = SurferUiStatus.RUNNING)
        }
    }

    fun playAgain(resetRenderer: () -> Unit) {
        countdownJob?.cancel()
        crashJob?.cancel()
        resetRenderer()
        _state.value = _state.value.copy(
            status = SurferUiStatus.READY,
            countdown = 3,
            score = 0,
            breadcrumbs = 0,
            distanceMeters = 0f,
            isNewHighScore = false,
            crashReason = ""
        )
    }

    fun toggleSound() {
        viewModelScope.launch {
            val next = !_state.value.soundEnabled
            preferences.setSoundEnabled(next)
            _state.value = _state.value.copy(soundEnabled = next)
        }
    }

    fun toggleHaptics() {
        viewModelScope.launch {
            val next = !_state.value.hapticsEnabled
            preferences.setHapticsEnabled(next)
            _state.value = _state.value.copy(hapticsEnabled = next)
        }
    }

    fun toggleSwipeHints() {
        _state.value = _state.value.copy(showSwipeHints = !_state.value.showSwipeHints)
    }

    fun resetHighScore() {
        viewModelScope.launch {
            preferences.resetSurferHighScore()
            _state.value = _state.value.copy(
                highScore = 0,
                maxDistance = 0
            )
        }
    }

    fun setEngineSupported(supported: Boolean) {
        _state.value = _state.value.copy(isEngineSupported = supported)
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        crashJob?.cancel()
    }
}
