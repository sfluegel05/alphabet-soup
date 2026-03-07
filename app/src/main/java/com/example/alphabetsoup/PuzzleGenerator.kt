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
    fun generateGame(size: Int, difficulty: Difficulty = Difficulty.Medium, useDiagonals: Boolean = true, useSecondHints: Boolean = false, random: Random = Random.Default): GameState {
        var solutionUnique = false
        var fullHints: GameHints
        var secondHints: GameHints = GameHints(
            rowLeft = Array(size) { null },
            rowRight = Array(size) { null },
            colTop = Array(size) { null },
            colBottom = Array(size) { null }
        )
        var solution: Array<IntArray>
        println("Generating solution...")
        solution      = generateSolution(size, useDiagonals, random)
        fullHints     = calculateHints(size, solution)
        if (useSecondHints) {
            val allSecond = calculate2ndHints(size, solution)
            val count = when {
                size <= 5 -> random.nextInt(2) + 1   // 1..2
                size <= 7 -> random.nextInt(3) + 2   // 2..4
                else      -> random.nextInt(3) + 3   // 3..5
            }
            secondHints = selectSecondHints(allSecond, count, random)
        }
        if (PuzzleSolver.countSolutions(size, fullHints, secondHints, maxCount = 2, useDiagonals = useDiagonals) == 1) {
            solutionUnique = true
        }
        var attempts = 1
        while (!solutionUnique && attempts < 20) {
            println("Generating solution...")
            solution  = generateSolution(size, useDiagonals, random)
            fullHints = calculateHints(size, solution)
            if (useSecondHints) {
                val allSecond = calculate2ndHints(size, solution)
                val count = when {
                    size <= 5 -> random.nextInt(2) + 1
                    size <= 7 -> random.nextInt(3) + 2
                    else      -> random.nextInt(3) + 3
                }
                secondHints = selectSecondHints(allSecond, count, random)
            }
            if (PuzzleSolver.countSolutions(size, fullHints, secondHints, maxCount = 2, useDiagonals = useDiagonals) == 1) {
                solutionUnique = true
            }
            attempts++
        }
        if (!solutionUnique) {
            throw IllegalStateException("Failed to generate a unique solution after $attempts attempts")
        }
        println("Removing hints...")
        val reducedHints = removeHints(size, fullHints, secondHints, useDiagonals, random)
        // round down to next integer
        val reAddCount = (size * difficulty.extraHintQuota).toInt()
        val finalHints    = reAddHints(fullHints, reducedHints, reAddCount, random)
        val playerGrid    = Array(size) { IntArray(size) { CELL_UNSET } }
        println("Done!")
        return GameState(size, solution, playerGrid, finalHints, useDiagonals, secondHints)
    }

    // ── 1. Solution generation ────────────────────────────────────────────────

    private fun generateSolution(size: Int, useDiagonals: Boolean, random: Random): Array<IntArray> {
        val grid = Array(size) { IntArray(size) { CELL_UNSET } }

        fun canPlace(row: Int, col: Int, value: Int): Boolean {
            for (c in 0 until col)  if (grid[row][c] == value) return false
            for (r in 0 until row)  if (grid[r][col] == value) return false
            if (useDiagonals) {
                if (row == col)
                    for (i in 0 until row) if (grid[i][i] == value) return false
                if (row + col == size - 1)
                    for (i in 0 until row) if (grid[i][size - 1 - i] == value) return false
            }
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
        // first non-empty from left/right/top/bottom for each row/col
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

    private fun calculate2ndHints(size: Int, solution: Array<IntArray>): GameHints {
        // second non-empty per direction
        val rowLeft   = Array<Int?>(size) { r ->
            solution[r].filter { it != CELL_EMPTY }.drop(1).firstOrNull()
        }
        val rowRight  = Array<Int?>(size) { r ->
            solution[r].filter { it != CELL_EMPTY }.dropLast(1).lastOrNull()
        }
        val colTop    = Array<Int?>(size) { c ->
            (0 until size).map { r -> solution[r][c] }.filter { it != CELL_EMPTY }.drop(1).firstOrNull()
        }
        val colBottom = Array<Int?>(size) { c ->
            (size - 1 downTo 0).map { r -> solution[r][c] }.filter { it != CELL_EMPTY }.dropLast(1).lastOrNull()
        }
        return GameHints(rowLeft, rowRight, colTop, colBottom)
    }

    // ── 2b. Secondary hint subset selection ──────────────────────────────────

    /** Randomly pick [count] positions from [allSecondHints], return a sparse GameHints with only those. */
    private fun selectSecondHints(allSecondHints: GameHints, count: Int, random: Random): GameHints {
        val size = allSecondHints.rowLeft.size
        val positions = mutableListOf<HintPos>()
        for (i in 0 until size) {
            if (allSecondHints.rowLeft[i]   != null) positions += HintPos(HintSide.ROW_LEFT,   i)
            if (allSecondHints.rowRight[i]  != null) positions += HintPos(HintSide.ROW_RIGHT,  i)
            if (allSecondHints.colTop[i]    != null) positions += HintPos(HintSide.COL_TOP,    i)
            if (allSecondHints.colBottom[i] != null) positions += HintPos(HintSide.COL_BOTTOM, i)
        }
        positions.shuffle(random)
        val result = GameHints(
            rowLeft   = Array(size) { null },
            rowRight  = Array(size) { null },
            colTop    = Array(size) { null },
            colBottom = Array(size) { null }
        )
        for (pos in positions.take(count.coerceAtMost(positions.size))) {
            result.set(pos, allSecondHints.get(pos))
        }
        return result
    }

    // ── 3. Hint removal ───────────────────────────────────────────────────────

    private fun removeHints(size: Int, hints: GameHints, secondHints: GameHints, useDiagonals: Boolean, random: Random): GameHints {
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
            if (PuzzleSolver.countSolutions(size, current, secondHints, maxCount = 2, useDiagonals = useDiagonals) != 1) {
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
    var totalHints = 0
    var totalSecondHints = 0
    for (i in 0 until 20) {
        val game = PuzzleGenerator.generateGame(7, difficulty = Difficulty.Easy, useSecondHints = true)
        val hintsCount = game.hints.rowLeft.count { it != null } + game.hints.rowRight.count { it != null } + game.hints.colTop.count { it != null } + game.hints.colBottom.count { it != null }
        val secondHintsCount = game.secondHints.rowLeft.count { it != null } + game.secondHints.rowRight.count { it != null } + game.secondHints.colTop.count { it != null } + game.secondHints.colBottom.count { it != null }
        totalHints += hintsCount
        totalSecondHints += secondHintsCount
        println("Number of Hints: ${game.hints.rowLeft.count { it != null } + game.hints.rowRight.count { it != null } + game.hints.colTop.count { it != null } + game.hints.colBottom.count { it != null }}")
        println("Number of second hints: ${game.secondHints.rowLeft.count { it != null } + game.secondHints.rowRight.count { it != null } + game.secondHints.colTop.count { it != null } + game.secondHints.colBottom.count { it != null }}")
        println("Solution:")
        for (r in 0 until game.size) {
            for (c in 0 until game.size) {
                print(game.letterChar(game.solution[r][c]) + " ")
            }
            println()
        }
    }
    println("Average hints: ${totalHints / 20.0}")
    println("Average second hints: ${totalSecondHints / 20.0}")

}