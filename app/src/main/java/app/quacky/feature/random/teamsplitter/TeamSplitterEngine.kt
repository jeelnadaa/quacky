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
        seed: Long? = null,
        previousTeams: List<Team>? = null
    ): List<Team> {
        val activePlayers = players.filter { !it.isExcluded }
        if (activePlayers.isEmpty() || teamCount <= 0) return emptyList()

        val actualTeamsCount = minOf(teamCount, activePlayers.size)
        val pool = activePlayers.toMutableList()

        var attempts = 0
        var result: List<Team>

        do {
            val teamBuckets = List(actualTeamsCount) { mutableListOf<Player>() }
            if (seed != null) {
                pool.shuffle(java.util.Random(seed + attempts))
            } else {
                pool.shuffle()
            }

            pool.forEachIndexed { index, player ->
                teamBuckets[index % actualTeamsCount].add(player)
            }

            result = teamBuckets.mapIndexed { index, members ->
                Team(
                    name = "Team ${index + 1}",
                    members = members
                )
            }
            attempts++
        } while (attempts < 8 && previousTeams != null && activePlayers.size > 2 && isSameGrouping(previousTeams, result))

        return result
    }

    private fun isSameGrouping(a: List<Team>, b: List<Team>): Boolean {
        if (a.size != b.size) return false
        val aSets = a.map { it.members.map { p -> p.id }.toSet() }.toSet()
        val bSets = b.map { it.members.map { p -> p.id }.toSet() }.toSet()
        return aSets == bSets
    }
}
