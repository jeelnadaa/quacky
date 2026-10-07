package app.quacky.feature.random.pickerwheel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.local.db.dao.WheelDao
import app.quacky.data.local.db.entity.SavedWheelEntity
import app.quacky.data.local.db.entity.WheelOptionEntity
import app.quacky.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class WheelItem(
    val id: String,
    val label: String,
    val weight: Int = 1
)

data class PickerWheelUiState(
    val options: List<WheelItem> = listOf(
        WheelItem("1", "Option A"),
        WheelItem("2", "Option B"),
        WheelItem("3", "Option C"),
        WheelItem("4", "Option D")
    ),
    val currentAngle: Float = 0f,
    val isSpinning: Boolean = false,
    val winner: WheelItem? = null,
    val savedWheels: List<SavedWheelEntity> = emptyList()
)

@HiltViewModel
class PickerWheelViewModel @Inject constructor(
    private val wheelDao: WheelDao,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val secureRandom = SecureRandom()

    private val _options = MutableStateFlow(
        listOf(
            WheelItem("1", "Option A"),
            WheelItem("2", "Option B"),
            WheelItem("3", "Option C"),
            WheelItem("4", "Option D")
        )
    )
    private val _currentAngle = MutableStateFlow(0f)
    private val _isSpinning = MutableStateFlow(false)
    private val _winner = MutableStateFlow<WheelItem?>(null)

    val savedWheels: StateFlow<List<SavedWheelEntity>> = wheelDao.getAllWheelsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<PickerWheelUiState> = combine(
        _options,
        _currentAngle,
        _isSpinning,
        _winner
    ) { opts, angle, spinning, win ->
        PickerWheelUiState(
            options = opts,
            currentAngle = angle,
            isSpinning = spinning,
            winner = win,
            savedWheels = savedWheels.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PickerWheelUiState())

    fun addOption(label: String) {
        if (label.isBlank() || _options.value.size >= 100) return
        val item = WheelItem(id = "${System.currentTimeMillis()}", label = label.trim())
        _options.value = _options.value + item
    }

    fun removeOption(id: String) {
        if (_options.value.size <= 1) return
        _options.value = _options.value.filter { it.id != id }
    }

    fun editOption(id: String, newLabel: String) {
        if (newLabel.isBlank()) return
        _options.value = _options.value.map {
            if (it.id == id) it.copy(label = newLabel.trim()) else it
        }
    }

    fun spin() {
        if (_isSpinning.value || _options.value.isEmpty()) return
        viewModelScope.launch {
            _isSpinning.value = true
            _winner.value = null

            // Pick winner based on weights
            val totalWeight = _options.value.sumOf { it.weight }
            var r = secureRandom.nextInt(totalWeight)
            var chosen = _options.value.first()
            for (opt in _options.value) {
                if (r < opt.weight) {
                    chosen = opt
                    break
                }
                r -= opt.weight
            }

            val spinDegrees = 360f * 5 + secureRandom.nextFloat() * 360f
            _currentAngle.value = (_currentAngle.value + spinDegrees) % 360f
            _winner.value = chosen
            _isSpinning.value = false

            historyRepository.addEntry(
                toolId = ToolRegistry.PICKER_WHEEL.id,
                type = "spin",
                title = "Winner: ${chosen.label}",
                subtitle = "From ${_options.value.size} options",
                payloadJson = JSONObject().apply {
                    put("winner", chosen.label)
                    put("optionsCount", _options.value.size)
                }.toString()
            )
        }
    }

    fun eliminateWinnerAndSpinAgain() {
        val currentWinner = _winner.value
        if (currentWinner != null) {
            removeOption(currentWinner.id)
            spin()
        }
    }

    fun saveCurrentWheel(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val wheelId = wheelDao.insertWheel(SavedWheelEntity(name = name.trim()))
            val optionEntities = _options.value.map {
                WheelOptionEntity(wheelId = wheelId, label = it.label, weight = it.weight)
            }
            wheelDao.insertOptions(optionEntities)
        }
    }

    fun reset() {
        _options.value = listOf(
            WheelItem("1", "Option A"),
            WheelItem("2", "Option B"),
            WheelItem("3", "Option C"),
            WheelItem("4", "Option D")
        )
        _currentAngle.value = 0f
        _isSpinning.value = false
        _winner.value = null
    }
}
