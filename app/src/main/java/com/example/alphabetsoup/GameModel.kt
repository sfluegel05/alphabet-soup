package com.example.alphabetsoup

import android.content.Context
import com.sfluegel.puzzleutils.GameSetting
import com.sfluegel.puzzleutils.PersistentHistory
import com.sfluegel.puzzleutils.SaveStore
import com.sfluegel.puzzleutils.SettingOption

enum class Difficulty(val extraHintQuota: Double) {
    // extra hint quota gives the number of extra hints multiplied by game size, rounded down
    // e.g. quota 0.4 gives 1.6=1 extra hint for size 4, 2.0=2 extra hints for size 5, 3.2=3 extra hints for size  8
    Easy(0.7), Medium(0.5), Hard(0.0)
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
    val history: List<Pair<List<Int>, List<Set<Int>>>>
)

data class SolveRecord(
    val timestamp: Long,           // ms since epoch
    val size: Int,
    val difficulty: Difficulty,
    val useDiagonals: Boolean,
    val useSecondHints: Boolean,
    val elapsedSeconds: Long
)

/** Persistent solve history, backed by SharedPreferences. */
object SolveHistory : PersistentHistory<SolveRecord>("solve_history") {
    override fun serialize(record: SolveRecord) =
        "${record.timestamp},${record.size},${record.difficulty.ordinal},${record.useDiagonals},${record.useSecondHints},${record.elapsedSeconds}"

    override fun deserialize(s: String): SolveRecord? = try {
        val p = s.split(",")
        SolveRecord(p[0].toLong(), p[1].toInt(), Difficulty.entries[p[2].toInt()],
            p[3].toBoolean(), p[4].toBoolean(), p[5].toLong())
    } catch (_: Exception) { null }

    override fun timestampOf(record: SolveRecord) = record.timestamp
}

// ── Settings ──────────────────────────────────────────────────────────────────

object DifficultySetting : GameSetting<Difficulty>(
    name = "Difficulty",
    options = listOf(
        SettingOption(Difficulty.Easy,   label = "Mild",    emoji = "🌿"),
        SettingOption(Difficulty.Medium, label = "Regular", emoji = "🌶️"),
        SettingOption(Difficulty.Hard,   label = "Spicy",   emoji = "🔥"),
    ),
    defaultIndex = 1
)

object DiagonalsSetting : GameSetting<Boolean>(
    name = "Diagonals",
    options = listOf(
        SettingOption(true,  label = "Cross-aint",  emoji = "✚"),
        SettingOption(false, label = "No diagonals", emoji = ""),
    ),
    defaultIndex = 0
)

object SecondHintsSetting : GameSetting<Boolean>(
    name = "Second hints",
    options = listOf(
        SettingOption(true,  label = "2nd helpings", emoji = "2️⃣"),
        SettingOption(false, label = "1st hints",    emoji = ""),
    ),
    defaultIndex = 1
)

// ── Save slots ────────────────────────────────────────────────────────────────

/** In-memory save slots, one per grid size. Tracks the most-recently saved slot. */
object GameSave : SaveStore<Int, SavedGame>({ it.size }) {
    var lastDifficulty: Difficulty = DifficultySetting.default.value
        private set
    var lastUseDiagonals: Boolean = DiagonalsSetting.default.value
        private set
    var lastUseSecondHints: Boolean = SecondHintsSetting.default.value
        private set

    fun recordDifficulty(d: Difficulty) { lastDifficulty = d }
    fun recordUseDiagonals(v: Boolean)   { lastUseDiagonals   = v }
    fun recordUseSecondHints(v: Boolean) { lastUseSecondHints = v }
}
