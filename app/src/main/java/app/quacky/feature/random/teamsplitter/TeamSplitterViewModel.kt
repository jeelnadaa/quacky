package app.quacky.feature.random.teamsplitter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.local.db.dao.GroupDao
import app.quacky.data.local.db.entity.GroupMemberEntity
import app.quacky.data.local.db.entity.SavedGroupEntity
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
import javax.inject.Inject

data class TeamSplitterUiState(
    val players: List<Player> = emptyList(),
    val teamCount: Int = 2,
    val teams: List<Team> = emptyList(),
    val savedGroups: List<SavedGroupEntity> = emptyList()
)

@HiltViewModel
class TeamSplitterViewModel @Inject constructor(
    private val groupDao: GroupDao,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _players = MutableStateFlow(
        listOf(
            Player("1", "Alex"),
            Player("2", "Jordan"),
            Player("3", "Taylor"),
            Player("4", "Morgan")
        )
    )
    private val _teamCount = MutableStateFlow(2)
    private val _teams = MutableStateFlow<List<Team>>(emptyList())

    val savedGroups: StateFlow<List<SavedGroupEntity>> = groupDao.getAllGroupsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<TeamSplitterUiState> = combine(
        _players,
        _teamCount,
        _teams
    ) { pList, count, tList ->
        TeamSplitterUiState(
            players = pList,
            teamCount = count,
            teams = tList,
            savedGroups = savedGroups.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TeamSplitterUiState())

    init {
        split()
    }

    fun addPlayer(name: String) {
        if (name.isBlank()) return
        val player = Player(id = "${System.currentTimeMillis()}", name = name.trim())
        _players.value = _players.value + player
        split()
    }

    fun removePlayer(id: String) {
        _players.value = _players.value.filter { it.id != id }
        split()
    }

    fun setTeamCount(count: Int) {
        _teamCount.value = count.coerceIn(2, 10)
        split()
    }

    fun split() {
        val teams = TeamSplitterEngine.splitIntoTeams(
            players = _players.value,
            teamCount = _teamCount.value
        )
        _teams.value = teams

        if (teams.isNotEmpty()) {
            viewModelScope.launch {
                val summary = teams.joinToString(" | ") { "${it.name}: ${it.members.joinToString(", ") { p -> p.name }}" }
                historyRepository.addEntry(
                    toolId = ToolRegistry.TEAM_SPLITTER.id,
                    type = "split",
                    title = "Split into ${_teamCount.value} teams",
                    subtitle = summary,
                    payloadJson = JSONObject().apply {
                        put("teamCount", _teamCount.value)
                        put("summary", summary)
                    }.toString()
                )
            }
        }
    }

    fun reset() {
        _players.value = listOf(
            Player("1", "Alex"),
            Player("2", "Jordan"),
            Player("3", "Taylor"),
            Player("4", "Morgan")
        )
        _teamCount.value = 2
        split()
    }

    fun saveGroup(groupName: String) {
        if (groupName.isBlank()) return
        viewModelScope.launch {
            val groupId = groupDao.insertGroup(SavedGroupEntity(name = groupName.trim()))
            val members = _players.value.map {
                GroupMemberEntity(groupId = groupId, name = it.name, skillRating = 3)
            }
            groupDao.insertMembers(members)
        }
    }
}
