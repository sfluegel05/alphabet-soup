package com.example.alphabetsoup

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sfluegel.puzzleutils.PencilMarksGrid
import com.sfluegel.puzzleutils.PuzzleBoard
import com.sfluegel.puzzleutils.PuzzleGridCell
import com.sfluegel.puzzleutils.PuzzleHintCell
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
    var notEmptyMarks: List<Boolean> = emptyList(),
    var history: List<Triple<List<Int>, List<Set<Int>>, List<Boolean>>> = emptyList(),
    var isSolved: Boolean           = false
)

// ── Screen entry point ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(size: Int, difficulty: Difficulty, useDiagonals: Boolean, useSecondHints: Boolean, useSubGrids: Boolean, onBack: () -> Unit) {
    val context          = LocalContext.current
    var gameState        by remember { mutableStateOf<GameState?>(null) }
    var elapsedSeconds   by remember { mutableStateOf(0L) }
    var timerActive      by remember { mutableStateOf(false) }
    var showSolvedDialog by remember { mutableStateOf(false) }
    // Plain array so updates from SideEffect don't trigger recomposition.
    val resetFnHolder    = remember { arrayOf<() -> Unit>({}) }
    // Bridge object: PuzzleBoard writes its state here every recomposition so
    // GameScreen can persist it when the user navigates back.
    val progress         = remember { GameProgress() }

    val helpText = buildString {
        val lastLetter = 'A' + size - 2
        append("Fill the ${size}×${size} grid with the letters A–$lastLetter.\n\n")
        append("Every row and column must contain each letter exactly once - leaving one cell empty.\n")
        if (useDiagonals) append("Special ingredient: diagonals also contain each letter once.\n\n") else append("\n")
        append("The hints around the grid show the first letter visible when looking in from that side. " +
               "If the nearest cell is empty, the hint shows the letter behind it.\n")
        if (useSecondHints) append("Second helpings: smaller hints show the second visible letter from that direction.\n\n") else append("\n")
        if (useSubGrids) append("Sub-grids are outlined in the grid — each region must also contain each letter exactly once.\n\n") else append("\n")
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
                    useSubGrids    = useSubGrids,
                    gameState      = gs,
                    cells          = progress.cells,
                    pencilMarks    = progress.pencilMarks,
                    notEmptyMarks  = progress.notEmptyMarks,
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
            progress.cells        = saved.cells
            progress.pencilMarks  = saved.pencilMarks
            progress.notEmptyMarks = saved.notEmptyMarks
            progress.history      = saved.history
            elapsedSeconds        = saved.elapsedSeconds
            gameState             = saved.gameState
        } else {
            // Generate a fresh puzzle.
            val state = withContext(Dispatchers.Default) {
                PuzzleGenerator.generateGame(size, difficulty, useDiagonals, useSecondHints, useSubGrids)
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
                            useSubGrids    = useSubGrids,
                            gameState      = gs,
                            cells          = progress.cells,
                            pencilMarks    = progress.pencilMarks,
                            notEmptyMarks  = progress.notEmptyMarks,
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
            // ── Solved dialog ─────────────────────────────────────────────────
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
                        TextButton(onClick = ::shareBrag) { Text("Brag about this") }
                        TextButton(onClick = onBack)      { Text("Back to Menu") }
                    }
                )
            }

            // ── Puzzle board ──────────────────────────────────────────────────
            val solution   = List(size * size) { idx -> state.solution[idx / size][idx % size] }
            val candidates = listOf(CELL_EMPTY) + (1 until size).toList()

            PuzzleBoard(
                size                  = size,
                solution              = solution,
                candidates            = candidates,
                unsetValue            = CELL_UNSET,
                initialCells          = progress.cells,
                initialPencilMarks    = progress.pencilMarks,
                initialNotEmptyMarks  = progress.notEmptyMarks,
                initialHistory        = progress.history,
                modifier              = Modifier.padding(innerPadding),
                onSolved              = {
                    timerActive = false
                    GameSave.clear(size)
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
                    showSolvedDialog = true
                },
                onProgressUpdate      = { cells, marks, notEmpty, hist, isSolved ->
                    progress.cells         = cells
                    progress.pencilMarks   = marks
                    progress.notEmptyMarks = notEmpty
                    progress.history       = hist
                    progress.isSolved      = isSolved
                },
                onRegisterReset       = { fn -> resetFnHolder[0] = fn },
                candidateLabel        = { candidate ->
                    if (candidate == CELL_EMPTY) EMPTY_CELL_SYMBOL
                    else state.letterChar(candidate).toString()
                },
                gridContent           = { availableWidth, availableHeight, cells, pencilMarks, notEmptyMarks, selectedCells, onSelectionChange ->
                    AlphabetSoupGrid(
                        state             = state,
                        size              = size,
                        availableWidth    = availableWidth,
                        availableHeight   = availableHeight,
                        cells             = cells,
                        pencilMarks       = pencilMarks,
                        selectedCells     = selectedCells,
                        onSelectionChange = onSelectionChange
                    )
                }
            )
        }
    }
}

// ── Alphabet Soup grid: hint border + cells ───────────────────────────────────

@Composable
private fun AlphabetSoupGrid(
    state: GameState,
    size: Int,
    availableWidth: Dp,
    availableHeight: Dp,
    cells: List<Int>,
    pencilMarks: List<Set<Int>>,
    selectedCells: Set<Int>,
    onSelectionChange: (Set<Int>) -> Unit
) {
    val cellSize = minOf(availableWidth / (size + 2), availableHeight / (size + 2))
    fun hint(v: Int?) = v?.let { state.letterChar(it).toString() }

    val subGrids = state.subGrids
    val borderColor = MaterialTheme.colorScheme.onSurface

    Box {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Spacer(Modifier.size(cellSize))
                for (c in 0 until size) PuzzleHintCell(hint(state.hints.colTop[c]), cellSize, hint(state.secondHints.colTop[c]))
                Spacer(Modifier.size(cellSize))
            }
            for (r in 0 until size) {
                Row {
                    PuzzleHintCell(hint(state.hints.rowLeft[r]), cellSize, hint(state.secondHints.rowLeft[r]))
                    for (c in 0 until size) {
                        val idx = r * size + c
                        GridCell(
                            value      = cells[idx],
                            size       = size,
                            cellSize   = cellSize,
                            marks      = pencilMarks[idx],
                            isSelected = idx in selectedCells,
                            onDiagonal = state.useDiagonals && (r == c || r + c == size - 1),
                            subGrid    = subGrids?.get(r)?.get(c),
                            gameState  = state,
                            onClick    = {
                                onSelectionChange(
                                    if (idx in selectedCells) emptySet() else setOf(idx)
                                )
                            }
                        )
                    }
                    PuzzleHintCell(hint(state.hints.rowRight[r]), cellSize, hint(state.secondHints.rowRight[r]))
                }
            }
            Row {
                Spacer(Modifier.size(cellSize))
                for (c in 0 until size) PuzzleHintCell(hint(state.hints.colBottom[c]), cellSize, hint(state.secondHints.colBottom[c]))
                Spacer(Modifier.size(cellSize))
            }
        }

        // Draw thick borders between cells that belong to different sub-grids
        if (subGrids != null) {
            Canvas(Modifier.matchParentSize()) {
                val cp = cellSize.toPx()
                val strokeW = 3.dp.toPx()
                for (r in 0 until size) {
                    for (c in 0 until size) {
                        // Vertical border to the right of (r, c)
                        if (c < size - 1 && subGrids[r][c] != subGrids[r][c + 1]) {
                            drawLine(
                                color       = borderColor,
                                start       = Offset((c + 2) * cp, (r + 1) * cp),
                                end         = Offset((c + 2) * cp, (r + 2) * cp),
                                strokeWidth = strokeW
                            )
                        }
                        // Horizontal border below (r, c)
                        if (r < size - 1 && subGrids[r][c] != subGrids[r + 1][c]) {
                            drawLine(
                                color       = borderColor,
                                start       = Offset((c + 1) * cp, (r + 2) * cp),
                                end         = Offset((c + 2) * cp, (r + 2) * cp),
                                strokeWidth = strokeW
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Game-specific cell wrapper ────────────────────────────────────────────────

// Soft pastel tints used as sub-grid colour overlays (cycles for >8 sub-grids)
private val SUB_GRID_TINTS = listOf(
    Color(0x14E57373), Color(0x1481C784), Color(0x1464B5F6), Color(0x14FFD54F),
    Color(0x14BA68C8), Color(0x1426C6DA), Color(0x14FF8A65), Color(0x14A1887F)
)

@Composable
private fun GridCell(
    value: Int,
    size: Int,
    cellSize: Dp,
    marks: Set<Int>,
    isSelected: Boolean,
    onDiagonal: Boolean,
    subGrid: Int?,
    gameState: GameState,
    onClick: () -> Unit
) {
    val baseColor = when (value) {
        CELL_UNSET -> MaterialTheme.colorScheme.surface
        CELL_EMPTY -> MaterialTheme.colorScheme.surfaceVariant
        else       -> MaterialTheme.colorScheme.primaryContainer
    }
    var bgColor = if (onDiagonal) lerp(baseColor, MaterialTheme.colorScheme.tertiary, 0.12f) else baseColor
    if (subGrid != null) {
        val tint = SUB_GRID_TINTS[subGrid % SUB_GRID_TINTS.size]
        bgColor = Color(
            red   = bgColor.red   * (1 - tint.alpha) + tint.red   * tint.alpha,
            green = bgColor.green * (1 - tint.alpha) + tint.green * tint.alpha,
            blue  = bgColor.blue  * (1 - tint.alpha) + tint.blue  * tint.alpha,
            alpha = bgColor.alpha
        )
    }

    PuzzleGridCell(cellSize = cellSize, isSelected = isSelected, backgroundColor = bgColor, onClick = onClick) {
        when {
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
