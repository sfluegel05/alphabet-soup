package com.example.alphabetsoup

/**
 * Counts valid solutions for a given hint set (up to [maxCount]).
 *
 * Cell values used internally:
 *   CELL_UNSET (-1) – not yet placed
 *   CELL_EMPTY (0)  – empty cell
 *   1..N-1          – letter index
 *
 * Constraints checked:
 *  • Each row    : every value 0..N-1 appears exactly once.
 *  • Each column : every value 0..N-1 appears exactly once.
 *  • Main diagonal (r==c)         : same.
 *  • Anti-diagonal (r+c==N-1)     : same.
 *  • Row hints   : first/last visible letter matches hint (when row complete).
 *  • Column hints: first/last visible letter matches hint (when column complete).
 *  - Second hints  : second / second to last visible letter matches hint (when row complete).
 */
object PuzzleSolver {

    fun countSolutions(size: Int, hints: GameHints, secondHints: GameHints, maxCount: Int = 2, useDiagonals: Boolean = true): Int {
        val grid = Array(size) { IntArray(size) { CELL_UNSET } }
        var count = 0

        // ── constraint: value not yet present in the row/col/diagonal ──────────
        fun canPlace(row: Int, col: Int, value: Int): Boolean {
            for (c in 0 until size)  if (c != col && grid[row][c] == value) return false
            for (r in 0 until size)  if (r != row && grid[r][col] == value) return false
            if (useDiagonals) {
                if (row == col)
                    for (i in 0 until size) if (i != col && grid[i][i] == value) return false
                if (row + col == size - 1)
                    for (i in 0 until size) if (i != row && grid[i][size - 1 - i] == value) return false
            }
            // check if value complies with hints
            // sort entered so far for each direction:
            // primary hint: value must match hint if value is first (all before are explicitly marked as empty)
            // value must NOT match hint if value third or later or behind another letter

            fun primaryHintValid(hint: Int, posInRow: Int, gridRow: IntArray): Boolean {
                if (value == CELL_EMPTY) return true
                return if (value == hint) {
                    if (posInRow == 0) true
                    else if (posInRow == 1 && (gridRow[0] == CELL_EMPTY || gridRow[0] == CELL_UNSET)) true
                    else false
                } else {
                    if (posInRow == 0) false
                    else if (posInRow == 1 && gridRow[0] == CELL_EMPTY) false
                    else true
                }
            }
            fun secondaryHintValid(hint: Int, posInRow: Int, gridRow: IntArray): Boolean {
                if (value == CELL_EMPTY) return true
                return if (value == hint) {
                    // matches hint iff it is the second visible letter
                    // second cell and first not empty, third cell and either first or second empty / unset
                    if (posInRow == 1 && gridRow[0] != CELL_EMPTY) true
                    else if (posInRow == 2 && (gridRow[0] == CELL_EMPTY || gridRow[0] == CELL_UNSET || gridRow[1] == CELL_EMPTY || gridRow[1] == CELL_UNSET)) true
                    else false
                } else {
                    if (posInRow == 1 && gridRow[0] != CELL_EMPTY && gridRow[0] != CELL_UNSET) false
                    else if (posInRow == 2 && (gridRow[0] == CELL_EMPTY || gridRow[1] == CELL_EMPTY)) false
                    else true
                }
            }
            data class SingleHint(val hint: Int?, val secondHint: Int?, val posInRow: Int, val gridRow: IntArray)

            val hintsPerDirection: List<SingleHint> = listOf(
                SingleHint(hints.rowLeft[row], secondHints.rowLeft[row], col, grid[row]),
                SingleHint(hints.rowRight[row], secondHints.rowRight[row], size - col - 1, grid[row].reversedArray()),
                SingleHint(hints.colTop[col], secondHints.colTop[col], row, IntArray(size) { r -> grid[r][col] }),
                SingleHint(hints.colBottom[col], secondHints.colBottom[col], size - row - 1, IntArray(size) { r -> grid[size - 1 - r][col] })
            )
            for (p in hintsPerDirection) {
                if (p.hint != null) {
                    if (!primaryHintValid(p.hint, p.posInRow, p.gridRow)) return false
                }
                if (p.secondHint != null) {
                    if (!secondaryHintValid(p.secondHint, p.posInRow, p.gridRow)) return false
                }
            }

            return true
        }

        // Minimum Remaining Values (MRV) + Forward Checking
        // initialize remainingValues for each cell as all possible values (0..N-1)
        var remainingValues = Array(size * size) { i ->
            (0 until size).toMutableSet()
        }
        // only add values that pass canPlace() based on hints
        for (pos in 0 until size * size) {
            val row = pos / size
            val col = pos % size
            remainingValues[pos].removeIf { value -> !canPlace(row, col, value) }
        }

        fun toIndex(row: Int, col: Int) = row * size + col

        fun selectNext(): Int {
            var minCount = Int.MAX_VALUE
            var bestCell = 0
            for (pos in 0 until size * size) {
                val r = pos / size
                val c = pos % size
                if (grid[r][c] == CELL_UNSET) {
                    val count = remainingValues[toIndex(r, c)].size
                    if (count < minCount) {
                        minCount = count
                        bestCell = pos
                    }
                }
            }
            return bestCell
        }

        fun forwardCheck(row: Int, col: Int, value: Int) {
            // remove from remainingValues if we can't place the value there anymore
            // check this value for all cells in the same row, column, and diagonals
            // check other values for row / column
            for (i in 0 until size) {
                for (v in remainingValues[toIndex(row, i)].toList()) {
                    if (!canPlace(row, i, v)) remainingValues[toIndex(row, i)].remove(v)
                }
                for (v in remainingValues[toIndex(i, col)].toList()) {
                    if (!canPlace(i, col, v)) remainingValues[toIndex(i, col)].remove(v)
                }
                if (useDiagonals) {
                    if (row == col && !canPlace(i, i, value)) remainingValues[toIndex(i, i)].remove(value)
                    if (row + col == size - 1 && !canPlace(i, size - 1 - i, value)) remainingValues[toIndex(i, size - 1 - i)].remove(value)
                }
            }
        }


        // ── backtracking ────────────────────────────────────────────────────────
        fun backtrack(pos: Int, nFilled: Int): Boolean {
            if (nFilled == size * size) {
                count++
                return count < maxCount   // false = stop (reached limit)
            }
            val row = pos / size
            val col = pos % size

            for (value in 0 until size) {   // 0 = EMPTY, 1..N-1 = letters
                if (value !in remainingValues[pos]) continue
                if (!canPlace(row, col, value)) continue

                val remainingValuesBefore = Array(size * size) { i ->
                    remainingValues[i].toMutableSet()
                }
                grid[row][col] = value
                // update remainingValues
                forwardCheck(row, col, value)
                // get position with minimum remaining values for next step
                val nextPos = selectNext()

                if (!backtrack(nextPos, nFilled + 1)) {
                    grid[row][col] = CELL_UNSET
                    remainingValues = remainingValuesBefore
                    return false            // propagate stop signal
                }

                grid[row][col] = CELL_UNSET
                remainingValues = remainingValuesBefore
            }
            return true
        }

        val firstPos = selectNext()
        backtrack(firstPos, 0)
        return count
    }
}
