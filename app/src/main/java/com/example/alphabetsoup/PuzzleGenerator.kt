package com.example.alphabetsoup

import kotlin.random.Random

private enum class HintSide { ROW_LEFT, ROW_RIGHT, COL_TOP, COL_BOTTOM }
private data class HintPos(val side: HintSide, val index: Int)

object PuzzleGenerator {

    /**
     * Generates a complete puzzle of size [size]:
     *  1. Random valid solution (row/col/diagonal uniqueness).
     *  2. Full 4×N hint set.
     *  3. Hints removed one-by-one as long as the solution remains unique.
     */
    fun generateGame(size: Int, random: Random = Random.Default): GameState {
        val solution      = generateSolution(size, random)
        val fullHints     = calculateHints(size, solution)
        val reducedHints  = removeHints(size, fullHints, random)
        val playerGrid    = Array(size) { IntArray(size) { CELL_UNSET } }
        return GameState(size, solution, playerGrid, reducedHints)
    }

    // ── 1. Solution generation ────────────────────────────────────────────────

    private fun generateSolution(size: Int, random: Random): Array<IntArray> {
        val grid = Array(size) { IntArray(size) { CELL_UNSET } }

        fun canPlace(row: Int, col: Int, value: Int): Boolean {
            for (c in 0 until col)  if (grid[row][c] == value) return false
            for (r in 0 until row)  if (grid[r][col] == value) return false
            if (row == col)
                for (i in 0 until row) if (grid[i][i] == value) return false
            if (row + col == size - 1)
                for (i in 0 until row) if (grid[i][size - 1 - i] == value) return false
            return true
        }

        fun backtrack(pos: Int): Boolean {
            if (pos == size * size) return true
            val row = pos / size
            val col = pos % size
            for (value in (0 until size).shuffled(random)) {
                if (canPlace(row, col, value)) {
                    grid[row][col] = value
                    if (backtrack(pos + 1)) return true
                    grid[row][col] = CELL_UNSET
                }
            }
            return false
        }

        check(backtrack(0)) { "Could not generate a valid solution for size $size" }
        return grid
    }

    // ── 2. Hint calculation ───────────────────────────────────────────────────

    private fun calculateHints(size: Int, solution: Array<IntArray>): GameHints {
        val rowLeft   = Array<Int?>(size) { r -> solution[r].firstOrNull { it != CELL_EMPTY } }
        val rowRight  = Array<Int?>(size) { r -> solution[r].lastOrNull  { it != CELL_EMPTY } }
        val colTop    = Array<Int?>(size) { c ->
            (0 until size).firstNotNullOfOrNull { r -> solution[r][c].takeIf { it != CELL_EMPTY } }
        }
        val colBottom = Array<Int?>(size) { c ->
            (size - 1 downTo 0).firstNotNullOfOrNull { r -> solution[r][c].takeIf { it != CELL_EMPTY } }
        }
        return GameHints(rowLeft, rowRight, colTop, colBottom)
    }

    // ── 3. Hint removal ───────────────────────────────────────────────────────

    private fun removeHints(size: Int, hints: GameHints, random: Random): GameHints {
        // Collect all non-null hint positions, then shuffle for random removal order.
        val positions = mutableListOf<HintPos>()
        for (i in 0 until size) {
            if (hints.rowLeft[i]   != null) positions += HintPos(HintSide.ROW_LEFT,   i)
            if (hints.rowRight[i]  != null) positions += HintPos(HintSide.ROW_RIGHT,  i)
            if (hints.colTop[i]    != null) positions += HintPos(HintSide.COL_TOP,    i)
            if (hints.colBottom[i] != null) positions += HintPos(HintSide.COL_BOTTOM, i)
        }
        positions.shuffle(random)

        // Work on mutable copies.
        val current = GameHints(
            rowLeft   = hints.rowLeft.copyOf(),
            rowRight  = hints.rowRight.copyOf(),
            colTop    = hints.colTop.copyOf(),
            colBottom = hints.colBottom.copyOf()
        )

        for (pos in positions) {
            val saved = current.get(pos)
            current.set(pos, null)
            if (PuzzleSolver.countSolutions(size, current, maxCount = 2) != 1) {
                current.set(pos, saved)   // restore: removing this hint breaks uniqueness
            }
        }
        return current
    }
}

// ── Helpers to read/write a single hint position ──────────────────────────────

private fun GameHints.get(pos: HintPos): Int? = when (pos.side) {
    HintSide.ROW_LEFT   -> rowLeft[pos.index]
    HintSide.ROW_RIGHT  -> rowRight[pos.index]
    HintSide.COL_TOP    -> colTop[pos.index]
    HintSide.COL_BOTTOM -> colBottom[pos.index]
}

private fun GameHints.set(pos: HintPos, value: Int?) { when (pos.side) {
    HintSide.ROW_LEFT   -> rowLeft[pos.index]   = value
    HintSide.ROW_RIGHT  -> rowRight[pos.index]  = value
    HintSide.COL_TOP    -> colTop[pos.index]    = value
    HintSide.COL_BOTTOM -> colBottom[pos.index] = value
} }
