package app.quacky.feature.random.teamsplitter

import java.util.SplittableRandom

data class Player(
    val id: String,
    val name: String,
    val isExcluded: Boolean = false
)

data class Team(
    val name: String,
    val members: List<Player>
)

object TeamSplitterEngine {

    /**
     * Splits players into teams evenly using randomized distribution.
     */
    fun splitIntoTeams(
        players: List<Player>,
        teamCount: Int,
        seed: Long? = null
    ): List<Team> {
        val activePlayers = players.filter { !it.isExcluded }
        if (activePlayers.isEmpty() || teamCount <= 0) return emptyList()

        val actualTeamsCount = minOf(teamCount, activePlayers.size)
        val teamBuckets = List(actualTeamsCount) { mutableListOf<Player>() }

        val pool = activePlayers.toMutableList()

        if (seed != null) {
            val rng = SplittableRandom(seed)
            pool.shuffle(java.util.Random(seed))
        } else {
            pool.shuffle()
        }

        // Evenly distribute players across teams
        pool.forEachIndexed { index, player ->
            teamBuckets[index % actualTeamsCount].add(player)
        }

        return teamBuckets.mapIndexed { index, members ->
            Team(
                name = "Team ${index + 1}",
                members = members
            )
        }
    }
}
