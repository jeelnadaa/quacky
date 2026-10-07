package app.quacky.feature.random

import app.quacky.feature.random.randomnumber.RngEngine
import app.quacky.feature.random.teamsplitter.Player
import app.quacky.feature.random.teamsplitter.TeamSplitterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RngAndTeamTest {

    @Test
    fun `seeded RNG is completely deterministic and reproducible`() {
        val seed = "Tournament-2026-Final"
        val run1 = RngEngine.generateIntegers(
            min = 1,
            max = 1000,
            count = 10,
            allowDuplicates = false,
            seed = seed,
            sequenceIndex = 0
        )
        val run2 = RngEngine.generateIntegers(
            min = 1,
            max = 1000,
            count = 10,
            allowDuplicates = false,
            seed = seed,
            sequenceIndex = 0
        )

        assertEquals("Same seed must produce identical outputs", run1, run2)
        assertEquals(10, run1.size)
    }

    @Test
    fun `split into teams distributes players evenly`() {
        val players = listOf(
            Player("1", "Alice"),
            Player("2", "Bob"),
            Player("3", "Charlie"),
            Player("4", "David"),
            Player("5", "Eva"),
            Player("6", "Frank")
        )

        val teams = TeamSplitterEngine.splitIntoTeams(
            players = players,
            teamCount = 2,
            seed = 12345L
        )

        assertEquals(2, teams.size)
        assertEquals(3, teams[0].members.size)
        assertEquals(3, teams[1].members.size)
        val allAssigned = (teams[0].members + teams[1].members).map { it.name }.toSet()
        assertEquals(6, allAssigned.size)
    }
}
