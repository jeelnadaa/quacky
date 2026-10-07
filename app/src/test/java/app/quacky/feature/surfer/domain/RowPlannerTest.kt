package app.quacky.feature.surfer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RowPlannerTest {

    @Test
    fun `planNextRows generates valid sequences across 10000 steps at varied speeds`() {
        val planner = RowPlanner(Random(42))
        val speeds = listOf(10.0f, 16.0f, 22.0f)

        for (speed in speeds) {
            var dist = 0.0f
            var lastRow: RowPlanner.Row? = null
            var rowsGenerated = 0

            while (rowsGenerated < 3500) {
                val newRows = planner.planNextRows(dist, speed, lastRow)
                assertTrue("New rows must not be empty", newRows.isNotEmpty())
                assertTrue(
                    "Rows at speed $speed starting at dist $dist must be valid",
                    planner.validate(newRows, lastRow, speed)
                )

                // First 45 m contain only coins
                if (dist < 45.0f) {
                    for (r in newRows) {
                        if (r.distance < 45.0f) {
                            assertTrue(
                                "First 45m must only contain coins or empty cells",
                                r.cells.all {
                                    it == RowPlanner.CellType.EMPTY ||
                                        it == RowPlanner.CellType.COIN ||
                                        it == RowPlanner.CellType.HIGH_COIN
                                }
                            )
                        }
                    }
                }

                lastRow = newRows.last()
                dist = lastRow.distance + SurferTuning.calculateRowGap(speed)
                rowsGenerated += newRows.size
            }
        }
    }

    @Test
    fun `jump physics matches tuning specification`() {
        val v0 = SurferTuning.JUMP_V0
        val g = SurferTuning.GRAVITY
        val apex = (v0 * v0) / (2f * g)
        val airTime = (2f * v0) / g

        assertEquals(1.61f, apex, 0.02f)
        assertEquals(0.68f, airTime, 0.02f)
    }

    @Test
    fun `scoring matches tuning specification`() {
        val score = SurferTuning.calculateScore(distanceMeters = 100.8f, breadcrumbs = 5)
        assertEquals(100 + 5 * 20, score)
    }
}
