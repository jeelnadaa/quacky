package app.quacky.feature.surfer.domain

import kotlin.random.Random

/**
 * Procedural fair pattern generator and validator for Quacky Surfer 3D.
 * Generates rows of obstacles and breadcrumbs according to strict fairness,
 * reachability, and spacing constraints specified in the rebuild document.
 */
class RowPlanner(
    private val random: Random = Random.Default
) {
    enum class CellType {
        EMPTY,
        COIN,
        HIGH_COIN,
        BARRIER,
        DUCT,
        TRAIN
    }

    data class Row(
        val distance: Float,
        val cells: List<CellType> // exactly 3 lanes: L, C, R
    )

    data class Template(
        val id: String,
        val rowPatterns: List<String>, // e.g. [".c.", ".c."]
        val minDistance: Float = 0f
    )

    companion object {
        val TEMPLATES = listOf(
            Template("coin_center", listOf(".c.", ".c.", ".c.", ".c.", ".c."), 0f),
            Template("coin_left", listOf("c..", "c..", "c..", "c.."), 0f),
            Template("coin_right", listOf("..c", "..c", "..c", "..c"), 0f),
            Template("coin_zigzag", listOf("c..", ".c.", "..c", ".c.", "c.."), 0f),
            Template("coin_stairs", listOf("c..", "c..", ".c.", ".c.", "..c", "..c"), 0f),
            Template("barrier_center", listOf("...", ".B.", "..."), 0f),
            Template("barrier_pair", listOf("B..", "..B", "..."), 150f),
            Template("slalom", listOf("B..", ".B.", "..B", ".B.", "B.."), 150f),
            Template("duct_center", listOf(".D.", ".c.", "..."), 0f),
            Template("duct_pair", listOf("D..", "..D", ".c."), 150f),
            Template("train_left_coins", listOf("Tc.", ".c.", ".c.", "..."), 150f),
            Template("train_right_coins", listOf(".cT", ".c.", ".c.", "..."), 150f),
            Template("train_center_sides", listOf(".T.", "c.c", "c.c", "..."), 150f),
            Template("two_trains_gap", listOf("T.T", ".c.", ".c.", "..."), 400f),
            Template("barrier_wall", listOf("BBB", "...", "c.c"), 400f),
            Template("duct_wall", listOf("DDD", "...", ".c."), 400f),
            Template("train_barrier_mix", listOf("T.B", "...", ".c."), 400f),
            Template("duct_then_barrier", listOf(".D.", "...", "B..", ".c."), 400f)
        )

        private val COIN_ONLY_TEMPLATE_IDS = setOf(
            "coin_center", "coin_left", "coin_right", "coin_zigzag", "coin_stairs"
        )
    }

    /**
     * Plan next template matching distance requirements and fairness constraints.
     */
    fun planNextRows(
        startDistance: Float,
        speed: Float,
        lastEmittedRow: Row?
    ): List<Row> {
        val rowGap = SurferTuning.calculateRowGap(speed)
        val availableTemplates = if (startDistance < 45.0f) {
            TEMPLATES.filter { it.id in COIN_ONLY_TEMPLATE_IDS }
        } else {
            TEMPLATES.filter { it.minDistance <= startDistance }.let { list ->
                if (startDistance < 25.0f) {
                    list.filter { !it.rowPatterns.any { p -> p.contains('T') } }
                } else list
            }
        }

        // Try up to 8 times with a randomized candidate
        for (attempt in 0 until 8) {
            val candidateTemplate = availableTemplates[random.nextInt(availableTemplates.size)]
            val candidateRows = instantiateTemplate(candidateTemplate, startDistance, rowGap)
            if (validate(candidateRows, lastEmittedRow, speed)) {
                return candidateRows
            }
        }

        // Guaranteed fair fallback
        val fallback = TEMPLATES.first { it.id == "coin_center" }
        return instantiateTemplate(fallback, startDistance, rowGap)
    }

    private fun instantiateTemplate(
        template: Template,
        startDistance: Float,
        rowGap: Float
    ): List<Row> {
        val result = mutableListOf<Row>()
        var d = startDistance
        for (pattern in template.rowPatterns) {
            val cells = pattern.map { charToCell(it) }
            result.add(Row(distance = d, cells = cells))
            d += rowGap
        }
        return result
    }

    private fun charToCell(ch: Char): CellType = when (ch) {
        'c' -> CellType.COIN
        'h' -> CellType.HIGH_COIN
        'B' -> CellType.BARRIER
        'D' -> CellType.DUCT
        'T' -> CellType.TRAIN
        else -> CellType.EMPTY
    }

    /**
     * Mandatory fairness validator as specified in Section 8.4.
     */
    fun validate(
        candidateRows: List<Row>,
        lastEmittedRow: Row?,
        speed: Float
    ): Boolean {
        val allRows = if (lastEmittedRow != null) listOf(lastEmittedRow) + candidateRows else candidateRows
        if (allRows.isEmpty()) return true

        var reachableLanes = setOf(0, 1, 2)
        var previousWasActionRow = false
        var previousRowDist = allRows.first().distance

        // Track active trains per lane with their end distance (11m footprint)
        val activeTrainEndDist = FloatArray(3) { -1f }

        for (i in allRows.indices) {
            val row = allRows[i]
            val rowDist = row.distance
            val timeGap = if (i > 0) (rowDist - previousRowDist) / speed else 1.0f

            // Register train fronts
            for (lane in 0 until 3) {
                if (row.cells[lane] == CellType.TRAIN) {
                    activeTrainEndDist[lane] = rowDist + SurferTuning.Colliders.TRAIN_LENGTH
                }
            }

            // Compute effective lane states
            val laneState = Array(3) { lane ->
                if (activeTrainEndDist[lane] > rowDist) {
                    CellType.TRAIN
                } else {
                    row.cells[lane]
                }
            }

            val hasFreeLane = laneState.any { it == CellType.EMPTY || it == CellType.COIN || it == CellType.HIGH_COIN }
            val barrierCount = laneState.count { it == CellType.BARRIER }
            val ductCount = laneState.count { it == CellType.DUCT }
            val trainCount = laneState.count { it == CellType.TRAIN }

            // Check passability
            val isJumpRow = !hasFreeLane && barrierCount > 0 && ductCount == 0 && (barrierCount + trainCount == 3)
            val isSlideRow = !hasFreeLane && ductCount > 0 && barrierCount == 0 && (ductCount + trainCount == 3)
            val isPassable = hasFreeLane || isJumpRow || isSlideRow

            if (!isPassable) return false

            // Consecutive action rows are strictly forbidden
            val isActionRow = isJumpRow || isSlideRow
            if (isActionRow && previousWasActionRow) return false

            // Calculate allowed lanes to enter
            val allowedLanes = mutableSetOf<Int>()
            for (lane in 0 until 3) {
                val st = laneState[lane]
                if (st == CellType.EMPTY || st == CellType.COIN || st == CellType.HIGH_COIN ||
                    (isJumpRow && st == CellType.BARRIER) ||
                    (isSlideRow && st == CellType.DUCT)
                ) {
                    allowedLanes.add(lane)
                }
            }

            // Maximum lateral shift based on time gap
            val maxShift = if (timeGap < 0.5f) 1 else 2
            val nextReachable = mutableSetOf<Int>()
            for (curr in allowedLanes) {
                if (reachableLanes.any { prev -> kotlin.math.abs(curr - prev) <= maxShift }) {
                    nextReachable.add(curr)
                }
            }

            if (nextReachable.isEmpty()) return false

            reachableLanes = nextReachable
            previousWasActionRow = isActionRow
            previousRowDist = rowDist
        }

        return true
    }
}
