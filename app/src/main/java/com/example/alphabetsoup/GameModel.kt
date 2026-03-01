package com.example.alphabetsoup

enum class Difficulty(val label: String) {
    Easy("Mild"), Medium("Regular"), Hard("Spicy")
}

/** A cell that the player has left blank (valid puzzle move). */
const val CELL_EMPTY = 0

/** Sentinel used in the player grid for cells not yet touched. */
const val CELL_UNSET = -1

/**
 * Hints displayed around the grid.
 * null  = this hint was removed (not shown to the player).
 * non-null = letter index: 1 → 'A', 2 → 'B', …
 *
 * rowLeft[r]   – first visible letter from the left  in row r
 * rowRight[r]  – first visible letter from the right in row r
 * colTop[c]    – first visible letter from the top    in column c
 * colBottom[c] – first visible letter from the bottom in column c
 */
class GameHints(
    val rowLeft: Array<Int?>,
    val rowRight: Array<Int?>,
    val colTop: Array<Int?>,
    val colBottom: Array<Int?>
)

/**
 * Full state of one puzzle.
 *
 * @param size     N — the grid is N×N, using N-1 distinct letters.
 * @param solution solution[r][c] → CELL_EMPTY (0) or letter index (1..N-1).
 * @param playerGrid same encoding but starts as all CELL_UNSET.
 * @param hints    possibly-reduced hint set shown to the player.
 */
class GameState(
    val size: Int,
    val solution: Array<IntArray>,
    val playerGrid: Array<IntArray>,
    val hints: GameHints,
    val useDiagonals: Boolean = true,
    val secondHints: GameHints = GameHints(
        rowLeft   = Array(size) { null },
        rowRight  = Array(size) { null },
        colTop    = Array(size) { null },
        colBottom = Array(size) { null }
    )
) {
    /** Convert a letter index (1..size-1) to its display character. */
    fun letterChar(index: Int): Char = 'A' + (index - 1)
}

/** Snapshot of an in-progress game that can be restored later. */
data class SavedGame(
    val size: Int,
    val difficulty: Difficulty,
    val useDiagonals: Boolean,
    val useSecondHints: Boolean,
    val gameState: GameState,
    val cells: List<Int>,
    val pencilMarks: List<Set<Int>>,
    val elapsedSeconds: Long,
    val history: List<List<Int>>
)

/** In-memory save slots, one per grid size. Tracks the most-recently saved slot. */
object GameSave {
    private val saves    = HashMap<Int, SavedGame>()
    private var lastSize: Int? = null
    var lastDifficulty: Difficulty = Difficulty.Medium
        private set
    var lastUseDiagonals: Boolean = true
        private set
    var lastUseSecondHints: Boolean = false
        private set

    fun put(game: SavedGame) {
        saves[game.size] = game
        lastSize = game.size
    }
    fun get(size: Int): SavedGame?  = saves[size]
    fun getLastSaved(): SavedGame?  = lastSize?.let { saves[it] }
    fun clear(size: Int) {
        saves.remove(size)
        if (lastSize == size) lastSize = null
    }
    fun recordDifficulty(d: Difficulty) { lastDifficulty = d }
    fun recordUseDiagonals(v: Boolean)   { lastUseDiagonals   = v }
    fun recordUseSecondHints(v: Boolean) { lastUseSecondHints = v }
}
