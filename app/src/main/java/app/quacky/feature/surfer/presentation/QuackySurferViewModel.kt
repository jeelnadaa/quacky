package app.quacky.feature.surfer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.feature.surfer.domain.SurferEngine
import app.quacky.feature.surfer.model.GameStatus
import app.quacky.feature.surfer.model.SurferGameState
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuackySurferViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {

    private val engine = SurferEngine()

    private val _state = MutableStateFlow(SurferGameState())
    val state: StateFlow<SurferGameState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SurferEngine.GameEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<SurferEngine.GameEvent> = _events.asSharedFlow()

    private var gameLoopJob: Job? = null

    init {
        viewModelScope.launch {
            val high = preferences.surferHighScore.first()
            val totalCoins = preferences.surferTotalCoins.first()
            _state.value = _state.value.copy(
                highScore = high,
                totalCoins = totalCoins
            )
        }
    }

    fun startGame() {
        gameLoopJob?.cancel()
        val currentHigh = _state.value.highScore
        val currentCoins = _state.value.totalCoins
        val newState = engine.startNewGame(currentHigh, currentCoins)
        _state.value = newState

        launchGameLoop()
    }

    fun switchLeft() {
        val (updated, changed) = engine.switchLane(_state.value, toRight = false)
        _state.value = updated
        if (changed) {
            _events.tryEmit(SurferEngine.GameEvent.LANE_SWITCH)
        }
    }

    fun switchRight() {
        val (updated, changed) = engine.switchLane(_state.value, toRight = true)
        _state.value = updated
        if (changed) {
            _events.tryEmit(SurferEngine.GameEvent.LANE_SWITCH)
        }
    }

    fun jump() {
        val (updated, jumped) = engine.jump(_state.value)
        _state.value = updated
        if (jumped) {
            _events.tryEmit(SurferEngine.GameEvent.JUMP)
        }
    }

    fun slide() {
        val (updated, slided) = engine.slide(_state.value)
        _state.value = updated
        if (slided) {
            _events.tryEmit(SurferEngine.GameEvent.SLIDE)
        }
    }

    fun pause() {
        if (_state.value.status == GameStatus.PLAYING) {
            gameLoopJob?.cancel()
            _state.value = _state.value.copy(status = GameStatus.PAUSED)
        }
    }

    fun resume() {
        if (_state.value.status == GameStatus.PAUSED) {
            _state.value = _state.value.copy(status = GameStatus.PLAYING)
            launchGameLoop()
        }
    }

    fun restart() {
        startGame()
    }

    private fun launchGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTimeNs = System.nanoTime()

            while (isActive && _state.value.status == GameStatus.PLAYING) {
                val now = System.nanoTime()
                val deltaMs = ((now - lastTimeNs) / 1_000_000L).coerceIn(4L, 50L)
                lastTimeNs = now

                val step = engine.tick(_state.value, deltaMs)
                _state.value = step.state

                for (event in step.events) {
                    _events.tryEmit(event)
                }

                if (step.state.status == GameStatus.GAME_OVER) {
                    onGameOver(step.state)
                    break
                }

                delay(16) // ~60 FPS
            }
        }
    }

    private fun onGameOver(finalState: SurferGameState) {
        viewModelScope.launch {
            val isNewRecord = preferences.recordSurferGameResult(
                score = finalState.score,
                coins = finalState.coins,
                distance = finalState.distanceMeters.toInt()
            )
            val updatedHigh = preferences.surferHighScore.first()
            val updatedCoins = preferences.surferTotalCoins.first()

            _state.value = _state.value.copy(
                isNewHighScore = isNewRecord,
                highScore = updatedHigh,
                totalCoins = updatedCoins
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}
