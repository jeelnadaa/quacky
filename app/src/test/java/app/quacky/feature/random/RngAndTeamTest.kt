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
    fun `snake draft balances team skill totals evenly`() {
        val players = listOf(
            Player("1", "Pro 1", skill = 5),
            Player("2", "Pro 2", skill = 5),
            Player("3", "Mid 1", skill = 3),
            Player("4", "Mid 2", skill = 3),
            Player("5", "Novice 1", skill = 1),
            Player("6", "Novice 2", skill = 1)
        )

        val teams = TeamSplitterEngine.splitIntoTeams(
            players = players,
            teamCount = 2,
            balanceBySkill = true,
            seed = 12345L
        )

        assertEquals(2, teams.size)
        assertEquals(3, teams[0].members.size)
        assertEquals(3, teams[1].members.size)
        // With snake draft:
        // Team 1 gets Pro (5), Mid (3), Novice (1) = 9
        // Team 2 gets Pro (5), Mid (3), Novice (1) = 9
        val diff = kotlin.math.abs(teams[0].totalSkill - teams[1].totalSkill)
        assertTrue("Skill difference between teams must be minimal", diff <= 1)
    }
}
