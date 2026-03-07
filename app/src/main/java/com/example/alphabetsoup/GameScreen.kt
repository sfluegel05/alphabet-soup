package com.example.alphabetsoup

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfluegel.puzzleutils.PencilMarksGrid
import com.sfluegel.puzzleutils.PuzzleGridCell
import com.sfluegel.puzzleutils.PuzzleHintCell
import com.sfluegel.puzzleutils.PuzzleLayout
import com.sfluegel.puzzleutils.PuzzleTopAppBar
import com.sfluegel.puzzleutils.WavyLoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

const val EMPTY_CELL_SYMBOL = "/"

// ── Mutable bridge written by PuzzleBoard, read by GameScreen on back ─────────

private class GameProgress(
    var cells: List<Int>            = emptyList(),
    var pencilMarks: List<Set<Int>> = emptyList(),
    var history: List<Pair<List<Int>, List<Set<Int>>>> = emptyList(),
    var isSolved: Boolean           = false
)

// ── Screen entry point ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(size: Int, difficulty: Difficulty, useDiagonals: Boolean, useSecondHints: Boolean, onBack: () -> Unit) {
    val context        = LocalContext.current
    var gameState      by remember { mutableStateOf<GameState?>(null) }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var timerActive    by remember { mutableStateOf(false) }
    // Plain array so updates from SideEffect don't trigger recomposition.
    val resetFnHolder  = remember { arrayOf<() -> Unit>({}) }
    // Bridge object: PuzzleBoard writes its state here every recomposition so
    // GameScreen can persist it when the user navigates back.
    val progress       = remember { GameProgress() }

    val helpText = buildString {
        val lastLetter = 'A' + size - 2
        append("Fill the ${size}×${size} grid with the letters A–$lastLetter.\n\n")
        append("Every row and column must contain each letter exactly once - leaving one cell empty.\n")
        if (useDiagonals) append("Special ingredient: diagonals also contain each letter once.\n\n") else append("\n")
        append("The hints around the grid show the first letter visible when looking in from that side. " +
               "If the nearest cell is empty, the hint shows the letter behind it.\n")
        if (useSecondHints) append("Second helpings: smaller hints show the second visible letter from that direction.\n\n") else append("\n")
        append("Tap a cell to select it, then pick a value below the grid. Use pencil mode to stir the soup.")
    }

    // Auto-save whenever this composable leaves composition (back nav OR rotation).
    DisposableEffect(Unit) {
        onDispose {
            val gs = gameState
            if (gs != null && !progress.isSolved) {
                GameSave.put(SavedGame(
                    size           = size,
                    difficulty     = difficulty,
                    useDiagonals   = useDiagonals,
                    useSecondHints = useSecondHints,
                    gameState      = gs,
                    cells          = progress.cells,
                    pencilMarks    = progress.pencilMarks,
                    elapsedSeconds = elapsedSeconds,
                    history        = progress.history
                ))
            }
        }
    }

    LaunchedEffect(size) {
        val saved = GameSave.get(size)
        if (saved != null) {
            // Restore an in-progress game.
            progress.cells       = saved.cells
            progress.pencilMarks = saved.pencilMarks
            progress.history     = saved.history
            elapsedSeconds       = saved.elapsedSeconds
            gameState            = saved.gameState
        } else {
            // Generate a fresh puzzle.
            val state = withContext(Dispatchers.Default) {
                PuzzleGenerator.generateGame(size, difficulty, useDiagonals, useSecondHints)
            }
            gameState = state
        }
        timerActive = true
    }

    // Tick once per second; cancels and restarts when timerActive changes.
    LaunchedEffect(timerActive) {
        if (timerActive) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    Scaffold(
        topBar = {
            PuzzleTopAppBar(
                title             = "Alphabet Soup – ${DifficultySetting.emoji(difficulty)} ${size}×${size}",
                onBack            = {
                    val gs = gameState
                    if (gs != null && !progress.isSolved) {
                        GameSave.put(SavedGame(
                            size           = size,
                            difficulty     = difficulty,
                            useDiagonals   = useDiagonals,
                            useSecondHints = useSecondHints,
                            gameState      = gs,
                            cells          = progress.cells,
                            pencilMarks    = progress.pencilMarks,
                            elapsedSeconds = elapsedSeconds,
                            history        = progress.history
                        ))
                    }
                    onBack()
                },
                elapsedSeconds    = elapsedSeconds,
                onReset           = if (gameState != null) resetFnHolder[0] else null,
                resetConfirmTitle = "Reheat soup?",
                resetConfirmText  = "Eat the same soup again. This will clear all your progress.",
                helpTitle         = "How to play",
                helpText          = helpText,
            )
        }
    ) { innerPadding ->
        val state = gameState
        if (state == null) {
            Column(
                modifier            = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                WavyLoadingIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text      = "Cooking a fresh soup, just for you...",
                    fontSize  = 16.sp,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            PuzzleBoard(
                gameState       = state,
                modifier        = Modifier.padding(innerPadding),
                elapsedSeconds  = elapsedSeconds,
                progress        = progress,
                onSolved        = {
                    timerActive = false
                    SolveHistory.add(
                        SolveRecord(
                            timestamp      = System.currentTimeMillis(),
                            size           = size,
                            difficulty     = difficulty,
                            useDiagonals   = useDiagonals,
                            useSecondHints = useSecondHints,
                            elapsedSeconds = elapsedSeconds
                        ), context
                    )
                },
                onBack          = onBack,
                difficulty      = difficulty,
                onRegisterReset = { fn -> resetFnHolder[0] = fn }
            )
        }
    }
}

// ── Puzzle board with player interaction ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PuzzleBoard(
    gameState: GameState,
    modifier: Modifier,
    elapsedSeconds: Long,
    progress: GameProgress,
    onSolved: () -> Unit,
    onBack: () -> Unit,
    difficulty: Difficulty,
    onRegisterReset: (() -> Unit) -> Unit
) {
    val size    = gameState.size
    val context = LocalContext.current

    // Flat list: index = r * size + c; values: CELL_UNSET / CELL_EMPTY / 1..N-1
    // Restored from progress when available (i.e. returning to a saved game).
    val cells = remember(gameState) {
        val initial = if (progress.cells.size == size * size) progress.cells
                      else List(size * size) { CELL_UNSET }
        mutableStateListOf(*initial.toTypedArray())
    }

    // Pencil marks: one Set<Int> per cell.
    val pencilMarks = remember(gameState) {
        val initial = if (progress.pencilMarks.size == size * size) progress.pencilMarks
                      else List<Set<Int>>(size * size) { emptySet() }
        mutableStateListOf(*initial.toTypedArray())
    }

    var pencilMode   by remember { mutableStateOf(false) }
    var selectedCell by remember { mutableStateOf<Int?>(null) }  // flat index

    // Values shown in the picker (EMPTY_CELL_SYMBOL first, then letters A…)
    val allCandidates = remember(size) { listOf(CELL_EMPTY) + (1 until size).toList() }

    // ── Undo history ───────────────────────────────────────────────────────
    val history = remember(gameState) {
        val deque = ArrayDeque<Pair<List<Int>, List<Set<Int>>>>()
        progress.history.forEach { deque.addLast(it) }
        deque
    }

    fun saveSnapshot() { history.addLast(cells.toList() to pencilMarks.toList()) }
    fun undo() {
        val (snapCells, snapMarks) = history.removeLastOrNull() ?: return
        snapCells.forEachIndexed { i, v -> cells[i] = v }
        snapMarks.forEachIndexed { i, v -> pencilMarks[i] = v }
        selectedCell = null
    }

    fun tap(r: Int, c: Int) {
        val idx = r * size + c
        if (pencilMode) {
            if (cells[idx] == CELL_UNSET) selectedCell = if (selectedCell == idx) null else idx
        } else {
            selectedCell = if (selectedCell == idx) null else idx
        }
    }

    // Normal mode: commit a value, or clear it if the cell already has that value.
    fun commitValue(candidate: Int) {
        val idx = selectedCell ?: return
        saveSnapshot()
        cells[idx] = if (cells[idx] == candidate) CELL_UNSET else candidate
    }

    fun toggleCandidate(candidate: Int) {
        val idx = selectedCell ?: return
        saveSnapshot()
        val current = pencilMarks[idx]
        pencilMarks[idx] = if (candidate in current) current - candidate else current + candidate
    }

    val isSolved by remember {
        derivedStateOf {
            cells.indices.all { idx ->
                cells[idx] == gameState.solution[idx / size][idx % size]
            }
        }
    }

    // Keep the progress bridge in sync so GameScreen can persist it on back.
    SideEffect {
        progress.cells       = cells.toList()
        progress.pencilMarks = pencilMarks.toList()
        progress.history     = history.toList()
        progress.isSolved    = isSolved
        onRegisterReset {
            cells.indices.forEach       { i -> cells[i]       = CELL_UNSET }
            pencilMarks.indices.forEach { i -> pencilMarks[i] = emptySet() }
            history.clear()
            selectedCell = null
        }
    }

    var showSolvedDialog by remember { mutableStateOf(false) }

    // Stop the timer the instant the puzzle is solved, then show the dialog.
    LaunchedEffect(isSolved) {
        if (isSolved) {
            GameSave.clear(gameState.size)   // no need to resume a finished game
            onSolved()
            showSolvedDialog = true
        }
    }

    if (showSolvedDialog) {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60

        fun shareBrag() {
            var msg = "I have devoured a whole bowl of Alphabet Soup ($size x $size, ${DifficultySetting.emoji(difficulty)}) - and it only took me"
            msg += if (minutes == 0L) " $seconds seconds! Can you do better?"
                   else               " $minutes minutes and $seconds seconds! Can you do better?"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg)
            }
            context.startActivity(Intent.createChooser(intent, null))
        }

        AlertDialog(
            onDismissRequest = { showSolvedDialog = false },
            title = { Text("Delicious!") },
            text  = {
                Text(
                    "You managed to find all the letters and make them your dinner!\n\n" +
                    "Your time: ${minutes}m ${seconds}s"
                )
            },
            confirmButton = {
                TextButton(onClick = { shareBrag() }) { Text("Brag about this") }
                TextButton(onClick = onBack)           { Text("Back to Menu") }
            }
        )
    }

    PuzzleLayout(
        modifier = modifier,
        grid = { availableWidth, availableHeight ->
            val cellSize = minOf(availableWidth / (size + 2), availableHeight / (size + 2))
            fun hint(v: Int?) = v?.let { gameState.letterChar(it).toString() }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    Spacer(Modifier.size(cellSize))
                    for (c in 0 until size) PuzzleHintCell(hint(gameState.hints.colTop[c]), cellSize, hint(gameState.secondHints.colTop[c]))
                    Spacer(Modifier.size(cellSize))
                }
                for (r in 0 until size) {
                    Row {
                        PuzzleHintCell(hint(gameState.hints.rowLeft[r]), cellSize, hint(gameState.secondHints.rowLeft[r]))
                        for (c in 0 until size) {
                            val idx = r * size + c
                            GridCell(
                                value      = cells[idx],
                                size       = size,
                                cellSize   = cellSize,
                                marks      = pencilMarks[idx],
                                isSelected = selectedCell == idx,
                                onDiagonal = gameState.useDiagonals && (r == c || r + c == size - 1),
                                gameState  = gameState,
                                onClick    = { tap(r, c) }
                            )
                        }
                        PuzzleHintCell(hint(gameState.hints.rowRight[r]), cellSize, hint(gameState.secondHints.rowRight[r]))
                    }
                }
                Row {
                    Spacer(Modifier.size(cellSize))
                    for (c in 0 until size) PuzzleHintCell(hint(gameState.hints.colBottom[c]), cellSize, hint(gameState.secondHints.colBottom[c]))
                    Spacer(Modifier.size(cellSize))
                }
            }
        },
        controls = {
            val sel = selectedCell
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Value picker ───────────────────────────────────────────
                val half = (allCandidates.size + 1) / 2
                val pickerRows = if (size > 5) listOf(allCandidates.take(half), allCandidates.drop(half))
                                 else          listOf(allCandidates)
                pickerRows.forEach { rowCandidates ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        rowCandidates.forEach { candidate ->
                            val isSelected = if (sel != null && pencilMode) candidate in pencilMarks[sel]
                                             else if (sel != null)          cells[sel] == candidate
                                             else                           false
                            FilterChip(
                                selected = isSelected,
                                onClick  = {
                                    if (pencilMode) toggleCandidate(candidate)
                                    else            commitValue(candidate)
                                },
                                label = {
                                    Text(
                                        if (candidate == CELL_EMPTY) EMPTY_CELL_SYMBOL
                                        else gameState.letterChar(candidate).toString()
                                    )
                                },
                                enabled = sel != null
                            )
                        }
                    }
                }
                // ── Controls row: undo + clear + pencil toggle ────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { undo() },
                        enabled = history.isNotEmpty()
                    ) { Text("Undo") }

                    val clearEnabled = sel != null && (cells[sel] != CELL_UNSET || pencilMarks[sel].isNotEmpty())
                    TextButton(
                        onClick = {
                            val idx = sel ?: return@TextButton
                            if (cells[idx] != CELL_UNSET) {
                                saveSnapshot()
                                cells[idx] = CELL_UNSET
                            } else {
                                saveSnapshot()
                                pencilMarks[idx] = emptySet()
                            }
                        },
                        enabled = clearEnabled
                    ) { Text("Clear") }

                    FilterChip(
                        selected = pencilMode,
                        onClick  = { pencilMode = !pencilMode },
                        label    = { Text(if (pencilMode) "Pencil mode: ON" else "Pencil mode: OFF") }
                    )
                }
            }
        }
    )
}

// ── Game-specific cell wrapper ────────────────────────────────────────────────

@Composable
private fun GridCell(
    value: Int,
    size: Int,
    cellSize: Dp,
    marks: Set<Int>,
    isSelected: Boolean,
    onDiagonal: Boolean,
    gameState: GameState,
    onClick: () -> Unit
) {
    val baseColor = when (value) {
        CELL_UNSET -> MaterialTheme.colorScheme.surface
        CELL_EMPTY -> MaterialTheme.colorScheme.surfaceVariant
        else       -> MaterialTheme.colorScheme.primaryContainer
    }
    val bgColor = if (onDiagonal) lerp(baseColor, MaterialTheme.colorScheme.tertiary, 0.12f) else baseColor

    PuzzleGridCell(cellSize = cellSize, isSelected = isSelected, backgroundColor = bgColor, onClick = onClick) {
        when {
            // Pencil marks are only shown while the cell has no committed value
            value == CELL_UNSET && marks.isNotEmpty() -> {
                val candidates = listOf(CELL_EMPTY) + (1 until size).toList()
                val labels = candidates.map { c ->
                    if (c == CELL_EMPTY) EMPTY_CELL_SYMBOL else gameState.letterChar(c).toString()
                }
                val cols = when { size <= 4 -> 2; size <= 7 -> 3; else -> 4 }
                PencilMarksGrid(labels = labels, marked = marks, cols = cols, cellSize = cellSize)
            }

            value == CELL_UNSET -> { /* blank – awaiting input */ }

            value == CELL_EMPTY -> Text(
                text     = EMPTY_CELL_SYMBOL,
                fontSize = (cellSize.value * 0.50f).sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> Text(
                text       = ('A' + value - 1).toString(),
                fontWeight = FontWeight.Bold,
                fontSize   = (cellSize.value * 0.45f).sp,
                color      = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
