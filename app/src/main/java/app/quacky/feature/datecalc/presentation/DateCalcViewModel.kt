package app.quacky.feature.datecalc.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.local.db.dao.CountdownDao
import app.quacky.data.local.db.entity.SavedCountdownEntity
import app.quacky.data.repository.HistoryRepository
import app.quacky.feature.datecalc.domain.AgeResult
import app.quacky.feature.datecalc.domain.DateCalculator
import app.quacky.feature.datecalc.domain.DateDifferenceResult
import app.quacky.feature.datecalc.domain.DayInfoResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class DateTab {
    AGE,
    DIFFERENCE,
    ADD_SUBTRACT,
    DAY_INFO
}

data class DateCalcUiState(
    val selectedTab: DateTab = DateTab.AGE,
    val birthDate: LocalDate = LocalDate.now().minusYears(20),
    val diffStartDate: LocalDate = LocalDate.now().minusMonths(1),
    val diffEndDate: LocalDate = LocalDate.now(),
    val includeEndDate: Boolean = false,
    val addSubStartDate: LocalDate = LocalDate.now(),
    val addYears: Int = 0,
    val addMonths: Int = 1,
    val addDays: Int = 0,
    val isAdd: Boolean = true,
    val infoDate: LocalDate = LocalDate.now(),
    val savedCountdowns: List<SavedCountdownEntity> = emptyList(),
    val ageResult: AgeResult = DateCalculator.calculateAge(LocalDate.now().minusYears(20)),
    val diffResult: DateDifferenceResult = DateCalculator.calculateDifference(LocalDate.now().minusMonths(1), LocalDate.now()),
    val addSubResultDate: LocalDate = LocalDate.now().plusMonths(1),
    val dayInfoResult: DayInfoResult = DateCalculator.getDayInfo(LocalDate.now())
)

@HiltViewModel
class DateCalcViewModel @Inject constructor(
    private val countdownDao: CountdownDao,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(DateTab.AGE)
    val selectedTab: StateFlow<DateTab> = _selectedTab.asStateFlow()

    private val _birthDate = MutableStateFlow(LocalDate.now().minusYears(25))
    private val _diffStartDate = MutableStateFlow(LocalDate.now().minusMonths(3))
    private val _diffEndDate = MutableStateFlow(LocalDate.now())
    private val _includeEndDate = MutableStateFlow(false)

    private val _addSubStartDate = MutableStateFlow(LocalDate.now())
    private val _addYears = MutableStateFlow(0)
    private val _addMonths = MutableStateFlow(1)
    private val _addDays = MutableStateFlow(0)
    private val _isAdd = MutableStateFlow(true)

    private val _infoDate = MutableStateFlow(LocalDate.now())

    val savedCountdowns: StateFlow<List<SavedCountdownEntity>> = countdownDao.getAllCountdownsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<DateCalcUiState> = combine(
        _selectedTab,
        _birthDate,
        _diffStartDate,
        _diffEndDate,
        _includeEndDate
    ) { tab, birth, dStart, dEnd, incEnd ->
        val age = DateCalculator.calculateAge(birth)
        val diff = DateCalculator.calculateDifference(dStart, dEnd, incEnd)
        val addSubResult = DateCalculator.addOrSubtract(
            _addSubStartDate.value,
            _addYears.value,
            _addMonths.value,
            days = _addDays.value,
            isAdd = _isAdd.value
        )
        val dayInfo = DateCalculator.getDayInfo(_infoDate.value)

        DateCalcUiState(
            selectedTab = tab,
            birthDate = birth,
            diffStartDate = dStart,
            diffEndDate = dEnd,
            includeEndDate = incEnd,
            addSubStartDate = _addSubStartDate.value,
            addYears = _addYears.value,
            addMonths = _addMonths.value,
            addDays = _addDays.value,
            isAdd = _isAdd.value,
            infoDate = _infoDate.value,
            ageResult = age,
            diffResult = diff,
            addSubResultDate = addSubResult,
            dayInfoResult = dayInfo
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DateCalcUiState()
    )

    fun selectTab(tab: DateTab) {
        _selectedTab.value = tab
    }

    fun setBirthDate(date: LocalDate) {
        _birthDate.value = date
    }

    fun setDiffStartDate(date: LocalDate) {
        _diffStartDate.value = date
    }

    fun setDiffEndDate(date: LocalDate) {
        _diffEndDate.value = date
    }

    fun setIncludeEndDate(include: Boolean) {
        _includeEndDate.value = include
    }

    fun setAddSubStartDate(date: LocalDate) {
        _addSubStartDate.value = date
    }

    fun setAddOffsets(years: Int, months: Int, days: Int, isAdd: Boolean) {
        _addYears.value = years
        _addMonths.value = months
        _addDays.value = days
        _isAdd.value = isAdd
    }

    fun setInfoDate(date: LocalDate) {
        _infoDate.value = date
    }

    fun saveCountdown(title: String, targetDate: LocalDate) {
        viewModelScope.launch {
            countdownDao.insert(
                SavedCountdownEntity(
                    title = title,
                    targetEpochDay = targetDate.toEpochDay()
                )
            )
        }
    }

    fun deleteCountdown(id: Long) {
        viewModelScope.launch {
            countdownDao.delete(id)
        }
    }

    fun saveCalculationToHistory() {
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        val state = uiState.value
        val (title, subtitle, payload) = when (state.selectedTab) {
            DateTab.AGE -> {
                val age = state.ageResult
                Triple(
                    "Age from ${state.birthDate.format(formatter)}",
                    "${age.years} years, ${age.months} months, ${age.days} days",
                    JSONObject().apply {
                        put("type", "age")
                        put("birthDate", state.birthDate.toString())
                        put("years", age.years)
                    }.toString()
                )
            }
            DateTab.DIFFERENCE -> {
                val diff = state.diffResult
                Triple(
                    "${state.diffStartDate.format(formatter)} → ${state.diffEndDate.format(formatter)}",
                    "${diff.totalDays} total days (${diff.businessDays} business days)",
                    JSONObject().apply {
                        put("type", "difference")
                        put("start", state.diffStartDate.toString())
                        put("end", state.diffEndDate.toString())
                        put("totalDays", diff.totalDays)
                    }.toString()
                )
            }
            DateTab.ADD_SUBTRACT -> {
                Triple(
                    "Date Calculation: ${if (state.isAdd) "+" else "-"}${state.addMonths} months",
                    "Result: ${state.addSubResultDate.format(formatter)} (${state.addSubResultDate.dayOfWeek})",
                    JSONObject().apply {
                        put("type", "add_subtract")
                        put("result", state.addSubResultDate.toString())
                    }.toString()
                )
            }
            DateTab.DAY_INFO -> {
                Triple(
                    "Day info for ${state.infoDate.format(formatter)}",
                    "${state.dayInfoResult.dayOfWeek}, Day ${state.dayInfoResult.dayOfYear}, Q${state.dayInfoResult.quarter}",
                    JSONObject().apply {
                        put("type", "day_info")
                        put("date", state.infoDate.toString())
                    }.toString()
                )
            }
        }

        viewModelScope.launch {
            historyRepository.addEntry(
                toolId = ToolRegistry.DATE_CALC.id,
                type = "calculation",
                title = title,
                subtitle = subtitle,
                payloadJson = payload
            )
        }
    }
}
