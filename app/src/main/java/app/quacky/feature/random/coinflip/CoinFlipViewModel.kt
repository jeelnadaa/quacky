package app.quacky.feature.random.coinflip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.capability.DeviceCapabilities
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.SecureRandom
import javax.inject.Inject

data class CoinFlipUiState(
    val coinCount: Int = 1,
    val headsLabel: String = "Heads",
    val tailsLabel: String = "Tails",
    val currentResults: List<Boolean> = listOf(true), // true = Heads, false = Tails
    val isFlipping: Boolean = false,
    val headsCount: Int = 0,
    val tailsCount: Int = 0,
    val hasAccelerometer: Boolean = false
)

@HiltViewModel
class CoinFlipViewModel @Inject constructor(
    private val capabilities: DeviceCapabilities,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val secureRandom = SecureRandom()

    private val _coinCount = MutableStateFlow(1)
    private val _headsLabel = MutableStateFlow("Heads")
    private val _tailsLabel = MutableStateFlow("Tails")
    private val _currentResults = MutableStateFlow(listOf(true))
    private val _isFlipping = MutableStateFlow(false)
    private val _headsCount = MutableStateFlow(0)
    private val _tailsCount = MutableStateFlow(0)

    val uiState: StateFlow<CoinFlipUiState> = combine(
        _coinCount,
        _headsLabel,
        _tailsLabel,
        _currentResults,
        _isFlipping
    ) { count, hLabel, tLabel, results, flipping ->
        CoinFlipUiState(
            coinCount = count,
            headsLabel = hLabel,
            tailsLabel = tLabel,
            currentResults = results,
            isFlipping = flipping,
            headsCount = _headsCount.value,
            tailsCount = _tailsCount.value,
            hasAccelerometer = capabilities.hasAccelerometer
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CoinFlipUiState())

    fun setCoinCount(count: Int) {
        _coinCount.value = count.coerceIn(1, 200)
    }

    fun setCustomLabels(heads: String, tails: String) {
        _headsLabel.value = if (heads.isBlank()) "Heads" else heads
        _tailsLabel.value = if (tails.isBlank()) "Tails" else tails
    }

    fun flip() {
        if (_isFlipping.value) return
        viewModelScope.launch {
            _isFlipping.value = true
            val count = _coinCount.value

            // Animation flicker
            for (i in 0..5) {
                _currentResults.value = List(count) { secureRandom.nextBoolean() }
                delay(50)
            }

            val finalResults = List(count) { secureRandom.nextBoolean() }
            _currentResults.value = finalResults
            _isFlipping.value = false

            val newHeads = finalResults.count { it }
            val newTails = finalResults.count { !it }
            _headsCount.value += newHeads
            _tailsCount.value += newTails

            val summary = finalResults.map { if (it) _headsLabel.value else _tailsLabel.value }
            historyRepository.addEntry(
                toolId = ToolRegistry.COIN_FLIP.id,
                type = "flip",
                title = "Flipped $count coin${if (count > 1) "s" else ""}",
                subtitle = summary.take(6).joinToString(", ") + if (summary.size > 6) "..." else "",
                payloadJson = JSONObject().apply {
                    put("count", count)
                    put("heads", newHeads)
                    put("tails", newTails)
                }.toString()
            )
        }
    }

    fun resetSession() {
        _headsCount.value = 0
        _tailsCount.value = 0
        _coinCount.value = 1
        _currentResults.value = listOf(true)
    }
}
