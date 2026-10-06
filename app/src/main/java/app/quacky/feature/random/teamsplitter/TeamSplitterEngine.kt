package app.quacky.feature.random.teamsplitter

import java.util.SplittableRandom

data class Player(
    val id: String,
    val name: String,
    val skill: Int = 3,
    val isExcluded: Boolean = false
)

data class Team(
    val name: String,
    val members: List<Player>,
    val totalSkill: Int = members.sumOf { it.skill }
)

object TeamSplitterEngine {

    /**
     * Splits players into teams using snake draft balancing when balanceBySkill is true.
     */
    fun splitIntoTeams(
        players: List<Player>,
        teamCount: Int,
        balanceBySkill: Boolean = true,
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

        if (balanceBySkill) {
            // Sort highest skill first
            pool.sortByDescending { it.skill }

            // Snake draft distribution:
            // Round 1: 0, 1, 2...
            // Round 2: ...2, 1, 0
            var teamIndex = 0
            var direction = 1
            for (player in pool) {
                teamBuckets[teamIndex].add(player)
                teamIndex += direction
                if (teamIndex >= actualTeamsCount) {
                    teamIndex = actualTeamsCount - 1
                    direction = -1
                } else if (teamIndex < 0) {
                    teamIndex = 0
                    direction = 1
                }
            }
        } else {
            // Simple round-robin
            pool.forEachIndexed { index, player ->
                teamBuckets[index % actualTeamsCount].add(player)
            }
        }

        return teamBuckets.mapIndexed { index, members ->
            Team(
                name = "Team ${index + 1}",
                members = members,
                totalSkill = members.sumOf { it.skill }
            )
        }
    }
}
