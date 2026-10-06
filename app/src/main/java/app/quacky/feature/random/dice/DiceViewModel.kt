package app.quacky.feature.random.dice

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

enum class D20RollMode {
    NORMAL,
    ADVANTAGE,
    DISADVANTAGE
}

data class RollRecord(
    val expression: String,
    val individualValues: List<Int>,
    val modifier: Int,
    val total: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class DiceUiState(
    val numberOfDice: Int = 1,
    val sides: Int = 6,
    val modifier: Int = 0,
    val d20Mode: D20RollMode = D20RollMode.NORMAL,
    val currentValues: List<Int> = listOf(6),
    val currentTotal: Int = 6,
    val isRolling: Boolean = false,
    val hasAccelerometer: Boolean = false,
    val history: List<RollRecord> = emptyList()
)

private data class DiceConfig(
    val numberOfDice: Int,
    val sides: Int,
    val modifier: Int,
    val d20Mode: D20RollMode
)

private data class DiceState(
    val currentValues: List<Int>,
    val currentTotal: Int,
    val isRolling: Boolean
)

@HiltViewModel
class DiceViewModel @Inject constructor(
    private val capabilities: DeviceCapabilities,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val secureRandom = SecureRandom()

    private val _numberOfDice = MutableStateFlow(1)
    private val _sides = MutableStateFlow(6)
    private val _modifier = MutableStateFlow(0)
    private val _d20Mode = MutableStateFlow(D20RollMode.NORMAL)
    private val _currentValues = MutableStateFlow(listOf(6))
    private val _currentTotal = MutableStateFlow(6)
    private val _isRolling = MutableStateFlow(false)
    private val _history = MutableStateFlow<List<RollRecord>>(emptyList())

    private val configFlow = combine(
        _numberOfDice,
        _sides,
        _modifier,
        _d20Mode
    ) { nDice, sides, mod, mode ->
        DiceConfig(nDice, sides, mod, mode)
    }

    private val stateFlow = combine(
        _currentValues,
        _currentTotal,
        _isRolling
    ) { vals, total, rolling ->
        DiceState(vals, total, rolling)
    }

    val uiState: StateFlow<DiceUiState> = combine(
        configFlow,
        stateFlow
    ) { config, state ->
        DiceUiState(
            numberOfDice = config.numberOfDice,
            sides = config.sides,
            modifier = config.modifier,
            d20Mode = config.d20Mode,
            currentValues = state.currentValues,
            currentTotal = state.currentTotal,
            isRolling = state.isRolling,
            hasAccelerometer = capabilities.hasAccelerometer,
            history = _history.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiceUiState())

    fun setNumberOfDice(count: Int) {
        _numberOfDice.value = count.coerceIn(1, 10)
    }

    fun setSides(s: Int) {
        _sides.value = s.coerceIn(2, 1000)
    }

    fun setModifier(m: Int) {
        _modifier.value = m
    }

    fun setD20Mode(mode: D20RollMode) {
        _d20Mode.value = mode
    }

    fun roll() {
        if (_isRolling.value) return
        viewModelScope.launch {
            _isRolling.value = true
            val count = _numberOfDice.value
            val sides = _sides.value
            val mod = _modifier.value
            val mode = _d20Mode.value

            // Flicker animation (numbers rapidly cycle)
            for (i in 0..6) {
                _currentValues.value = List(count) { secureRandom.nextInt(sides) + 1 }
                delay(40)
            }

            // Settle on final values
            val finalValues = if (sides == 20 && count == 1 && mode != D20RollMode.NORMAL) {
                val r1 = secureRandom.nextInt(20) + 1
                val r2 = secureRandom.nextInt(20) + 1
                val chosen = if (mode == D20RollMode.ADVANTAGE) maxOf(r1, r2) else minOf(r1, r2)
                listOf(chosen)
            } else {
                List(count) { secureRandom.nextInt(sides) + 1 }
            }

            val sum = finalValues.sum() + mod
            _currentValues.value = finalValues
            _currentTotal.value = sum
            _isRolling.value = false

            val expr = "${count}d${sides}${if (mod > 0) "+$mod" else if (mod < 0) "$mod" else ""}"
            val record = RollRecord(
                expression = expr,
                individualValues = finalValues,
                modifier = mod,
                total = sum
            )
            _history.value = listOf(record) + _history.value.take(99)

            // Save to generic history
            historyRepository.addEntry(
                toolId = ToolRegistry.DICE.id,
                type = "roll",
                title = "$expr = $sum",
                subtitle = "Rolled: ${finalValues.joinToString(", ")}",
                payloadJson = JSONObject().apply {
                    put("expression", expr)
                    put("total", sum)
                }.toString()
            )
        }
    }
}
