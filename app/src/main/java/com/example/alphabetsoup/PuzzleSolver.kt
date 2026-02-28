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
            return true
        }

        // ── hint checks (called only when the row/column is fully placed) ──────
        fun firstNonEmpty(arr: IntArray) = arr.firstOrNull { it != CELL_EMPTY }
        fun lastNonEmpty(arr: IntArray)  = arr.lastOrNull  { it != CELL_EMPTY }

        fun checkRowHints(row: Int): Boolean {
            val r  = grid[row]
            val lh = hints.rowLeft[row]
            val rh = hints.rowRight[row]
            return (lh == null || firstNonEmpty(r) == lh) &&
                   (rh == null || lastNonEmpty(r)  == rh)
        }

        fun checkColHints(col: Int): Boolean {
            val c  = IntArray(size) { r -> grid[r][col] }
            val th = hints.colTop[col]
            val bh = hints.colBottom[col]
            return (th == null || firstNonEmpty(c) == th) &&
                   (bh == null || lastNonEmpty(c)  == bh)
        }

        // ── backtracking ────────────────────────────────────────────────────────
        fun backtrack(pos: Int): Boolean {
            if (pos == size * size) {
                count++
                return count < maxCount   // false = stop (reached limit)
            }
            val row = pos / size
            val col = pos % size

            for (value in 0 until size) {   // 0 = EMPTY, 1..N-1 = letters
                if (!canPlace(row, col, value)) continue

                grid[row][col] = value

                val rowOk = col < size - 1 || checkRowHints(row)
                val colOk = row < size - 1 || checkColHints(col)

                if (rowOk && colOk && !backtrack(pos + 1)) {
                    grid[row][col] = CELL_UNSET
                    return false            // propagate stop signal
                }

                grid[row][col] = CELL_UNSET
            }
            return true
        }

        backtrack(0)
        return count
    }
}
