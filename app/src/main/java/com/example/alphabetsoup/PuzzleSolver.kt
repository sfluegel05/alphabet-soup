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
 */
object PuzzleSolver {

    fun countSolutions(size: Int, hints: GameHints, maxCount: Int = 2): Int {
        val grid = Array(size) { IntArray(size) { CELL_UNSET } }
        var count = 0

        // ── constraint: value not yet present in the row/col/diagonal ──────────
        fun canPlace(row: Int, col: Int, value: Int): Boolean {
            for (c in 0 until col)  if (grid[row][c] == value) return false
            for (r in 0 until row)  if (grid[r][col] == value) return false
            if (row == col)
                for (i in 0 until row) if (grid[i][i] == value) return false
            if (row + col == size - 1)
                for (i in 0 until row) if (grid[i][size - 1 - i] == value) return false
            // check if value complies with hints (outermost cell: match hint or empty,
            // 2nd cell: if outer is empty, match hint)
            if (col == 0) {
                val leftHint = hints.rowLeft[row]
                if (leftHint != null && value != leftHint && value != CELL_EMPTY) return false
            }
            if (col == 1) {
                val leftHint = hints.rowLeft[row]
                if (leftHint != null && grid[0][col] == CELL_EMPTY && value != leftHint) return false
            }
            if (row == 0) {
                val topHint = hints.colTop[col]
                if (topHint != null && value != topHint && value != CELL_EMPTY) return false
            }
            if (row == 1) {
                val topHint = hints.colTop[col]
                if (topHint != null && grid[row][0] == CELL_EMPTY && value != topHint) return false
            }
            if (col == size - 1) {
                val rightHint = hints.rowRight[row]
                if (rightHint != null && value != rightHint && value != CELL_EMPTY) return false
            }
            if (col == size - 2) {
                val rightHint = hints.rowRight[row]
                if (rightHint != null && grid[row][size - 1] == CELL_EMPTY && value != rightHint) return false
            }
            if (row == size - 1) {
                val bottomHint = hints.colBottom[col]
                if (bottomHint != null && value != bottomHint && value != CELL_EMPTY) return false
            }
            if (row == size - 2) {
                val bottomHint = hints.colBottom[col]
                if (bottomHint != null && grid[size - 1][col] == CELL_EMPTY && value != bottomHint) return false
            }
            return true
        }

        // Minimum Remaining Values (MRV) + Forward Checking
        var remainingValues = Array(size * size) { i ->
            (0 until size).toMutableSet()
        }

        fun toIndex(row: Int, col: Int) = row * size + col

        for (rowcol in 0 until size) {
            // remove impossible values based on hints (outermost cell can only be hint or empty, all other cells (except 2nd) can't be hint)
            val leftHint = hints.rowLeft[rowcol]
            remainingValues[toIndex(rowcol, 0)].removeIf { leftHint != null && it != leftHint && it != CELL_EMPTY }
            for (c in 2 until size) remainingValues[toIndex(rowcol, c)].removeIf { leftHint != null && it == leftHint }
            val rightHint = hints.rowRight[rowcol]
            remainingValues[toIndex(rowcol, size - 1)].removeIf { rightHint != null && it != rightHint && it != CELL_EMPTY }
            for (c in 0 until size - 2) remainingValues[toIndex(rowcol, c)].removeIf { rightHint != null && it == rightHint }
            val topHint = hints.colTop[rowcol]
            remainingValues[toIndex(0, rowcol)].removeIf { topHint != null && it != topHint && it != CELL_EMPTY }
            for (r in 2 until size) remainingValues[toIndex(r, rowcol)].removeIf { topHint != null && it == topHint }
            val bottomHint = hints.colBottom[rowcol]
            remainingValues[toIndex(size - 1, rowcol)].removeIf { bottomHint != null && it != bottomHint && it != CELL_EMPTY }
            for (r in 0 until size - 2) remainingValues[toIndex(r, rowcol)].removeIf { bottomHint != null && it == bottomHint }
        }

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
            // remove value from remainingValues of cells in the same row, column, and diagonals
            for (c in 0 until size)  remainingValues[toIndex(row, c)].remove(value)
            for (r in 0 until size)  remainingValues[toIndex(r, col)].remove(value)
            if (row == col)
                for (i in 0 until size) remainingValues[toIndex(i, i)].remove(value)
            if (row + col == size - 1)
                for (i in 0 until size) remainingValues[toIndex(i, size - 1 - i)].remove(value)
            // update based on hints: if outermost cell is empty, next must match hint
            if (value == CELL_EMPTY) {
                if (col == 0) {
                    val leftHint = hints.rowLeft[row]
                    if (leftHint != null) {
                        remainingValues[toIndex(row, col + 1)].removeIf { it != leftHint }
                    }
                }
                if (col == size - 1) {
                    val rightHint = hints.rowRight[row]
                    if (rightHint != null) {
                        remainingValues[toIndex(row, col - 1)].removeIf { it != rightHint }
                    }
                }
                if (row == 0) {
                    val topHint = hints.colTop[col]
                    if (topHint != null) {
                        remainingValues[toIndex(row + 1, col)].removeIf { it != topHint }
                    }
                }
                if (row == size - 1) {
                    val bottomHint = hints.colBottom[col]
                    if (bottomHint != null) {
                        remainingValues[toIndex(row - 1, col)].removeIf { it != bottomHint }
                    }
                }
            }
            // cells next to outermost cells: if they don't comply with the hint, the outermost
            // cells must comply with the hint
            if (col == 1) {
                val leftHint = hints.rowLeft[row]
                if (leftHint != null && value != leftHint) {
                    remainingValues[toIndex(row, 0)].removeIf { it != leftHint }
                }
            }
            if (col == size - 2) {
                val rightHint = hints.rowRight[row]
                if (rightHint != null && value != rightHint) {
                    remainingValues[toIndex(row, size - 1)].removeIf { it != rightHint }
                }
            }
            if (row == 1) {
                val topHint = hints.colTop[col]
                if (topHint != null && value != topHint) {
                    remainingValues[toIndex(0, col)].removeIf { it != topHint }
                }
            }
            if (row == size - 2) {
                val bottomHint = hints.colBottom[col]
                if (bottomHint != null && value != bottomHint) {
                    remainingValues[toIndex(size - 1, col)].removeIf { it != bottomHint }
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
