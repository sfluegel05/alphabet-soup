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
     *  4. For easier difficulties, some removed hints are restored.
     */
    fun generateGame(size: Int, difficulty: Difficulty = Difficulty.Medium, random: Random = Random.Default): GameState {
        var solutionUnique = false
        var fullHints: GameHints
        var solution: Array<IntArray>
        println("Generating solution...")
        solution      = generateSolution(size, random)
        println("Calculating hints...")
        fullHints     = calculateHints(size, solution)
        if (PuzzleSolver.countSolutions(size, fullHints, maxCount = 2) == 1) {
            solutionUnique = true
        }
        while (!solutionUnique) {
            println("Generating solution...")
            solution      = generateSolution(size, random)
            println("Calculating hints...")
            fullHints     = calculateHints(size, solution)
            if (PuzzleSolver.countSolutions(size, fullHints, maxCount = 2) == 1) {
                solutionUnique = true
            }
        }
        println("Removing hints...")
        val reducedHints = removeHints(size, fullHints, random)
        val reAddCount = when (difficulty) {
            Difficulty.Hard   -> 0
            Difficulty.Medium -> if (size <= 5) 1 else 2
            Difficulty.Easy   -> if (size <= 5) 2 else if (size == 6) 3 else 4
        }
        val finalHints    = reAddHints(fullHints, reducedHints, reAddCount, random)
        val playerGrid    = Array(size) { IntArray(size) { CELL_UNSET } }
        println("Done!")
        return GameState(size, solution, playerGrid, finalHints)
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

    // ── 4. Hint re-addition (for easier difficulties) ─────────────────────────

    private fun reAddHints(
        fullHints: GameHints,
        reducedHints: GameHints,
        count: Int,
        random: Random
    ): GameHints {
        if (count == 0) return reducedHints
        val size = fullHints.rowLeft.size

        // Collect positions that were removed (null in reduced, non-null in full).
        val removedPositions = mutableListOf<HintPos>()
        for (i in 0 until size) {
            if (reducedHints.rowLeft[i]   == null && fullHints.rowLeft[i]   != null) removedPositions += HintPos(HintSide.ROW_LEFT,   i)
            if (reducedHints.rowRight[i]  == null && fullHints.rowRight[i]  != null) removedPositions += HintPos(HintSide.ROW_RIGHT,  i)
            if (reducedHints.colTop[i]    == null && fullHints.colTop[i]    != null) removedPositions += HintPos(HintSide.COL_TOP,    i)
            if (reducedHints.colBottom[i] == null && fullHints.colBottom[i] != null) removedPositions += HintPos(HintSide.COL_BOTTOM, i)
        }
        removedPositions.shuffle(random)

        val result = GameHints(
            rowLeft   = reducedHints.rowLeft.copyOf(),
            rowRight  = reducedHints.rowRight.copyOf(),
            colTop    = reducedHints.colTop.copyOf(),
            colBottom = reducedHints.colBottom.copyOf()
        )
        for (pos in removedPositions.take(count.coerceAtMost(removedPositions.size))) {
            result.set(pos, fullHints.get(pos))
        }
        return result
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


fun main() {
    val game = PuzzleGenerator.generateGame(8)
    println("Hints:")
    for (r in 0 until game.size) {
        val left  = game.hints.rowLeft[r]?.let { game.letterChar(it) } ?: '.'
        val right = game.hints.rowRight[r]?.let { game.letterChar(it) } ?: '.'
        println("  $left . . $right")
    }
    for (c in 0 until game.size) {
        val top    = game.hints.colTop[c]?.let { game.letterChar(it) } ?: '.'
        val bottom = game.hints.colBottom[c]?.let { game.letterChar(it) } ?: '.'
        println("  $top   $bottom")
    }
    println("Solution:")
    for (r in 0 until game.size) {
        for (c in 0 until game.size) {
            print(game.letterChar(game.solution[r][c]) + " ")
        }
        println()
    }

}