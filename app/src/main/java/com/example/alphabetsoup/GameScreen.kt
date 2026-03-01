package com.example.alphabetsoup

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

const val EMPTY_CELL_SYMBOL = "/"

// ── Mutable bridge written by PuzzleBoard, read by GameScreen on back ─────────

private class GameProgress(
    var cells: List<Int>            = emptyList(),
    var pencilMarks: List<Set<Int>> = emptyList(),
    var history: List<List<Int>>    = emptyList(),
    var isSolved: Boolean           = false
)

// ── Screen entry point ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(size: Int, difficulty: Difficulty, onBack: () -> Unit) {
    var gameState by remember { mutableStateOf<GameState?>(null) }

    var elapsedSeconds by remember { mutableStateOf(0L) }
    var timerActive    by remember { mutableStateOf(false) }
    var showHelp       by remember { mutableStateOf(false) }

    // Bridge object: PuzzleBoard writes its state here every recomposition so
    // GameScreen can persist it when the user navigates back.
    val progress = remember { GameProgress() }

    if (showHelp) {
        val lastLetter = 'A' + size - 2
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("How to play") },
            text  = {
                Text(
                    "Fill the ${size}×${size} grid with the letters A–$lastLetter.\n\n" +
                    "Every row, column, and both diagonals must contain each letter " +
                    "exactly once — plus exactly one empty cell.\n\n" +
                    "The hints around the grid show the first letter visible when " +
                    "looking in from that side. If the nearest cell is empty, the " +
                    "hint shows the letter behind it.\n\n" +
                    "Tap a cell to select it, then pick a value below the grid. " +
                    "Use pencil mode to note down possibilities without committing."
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("Got it") }
            }
        )
    }

    LaunchedEffect(size) {
        val saved = GameSave.get(size)
        if (saved != null) {
            // Restore an in-progress game.
            progress.cells        = saved.cells
            progress.pencilMarks  = saved.pencilMarks
            progress.history      = saved.history
            elapsedSeconds        = saved.elapsedSeconds
            gameState             = saved.gameState
        } else {
            // Generate a fresh puzzle.
            val state = withContext(Dispatchers.Default) {
                PuzzleGenerator.generateGame(size, difficulty)
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
            TopAppBar(
                title = { Text("Alphabet Soup – ${size}×${size}") },
                navigationIcon = {
                    IconButton(onClick = {
                        val gs = gameState
                        if (gs != null && !progress.isSolved) {
                            GameSave.put(
                                SavedGame(
                                    size           = size,
                                    difficulty     = difficulty,
                                    gameState      = gs,
                                    cells          = progress.cells,
                                    pencilMarks    = progress.pencilMarks,
                                    elapsedSeconds = elapsedSeconds,
                                    history        = progress.history
                                )
                            )
                        }
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Text(
                            text  = "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text     = "%d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
                        style    = MaterialTheme.typography.titleMedium,
                        color    = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        val state = gameState
        if (state == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                WavyLoadingIndicator()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cooking a fresh soup, just for you...",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            PuzzleBoard(
                gameState      = state,
                modifier       = Modifier.padding(innerPadding),
                elapsedSeconds = elapsedSeconds,
                progress       = progress,
                onSolved       = { timerActive = false },
                onBack         = onBack,
                difficulty     = difficulty
            )
        }
    }
}

// ── Puzzle board with player interaction ─────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PuzzleBoard(
    gameState: GameState,
    modifier: Modifier,
    elapsedSeconds: Long,
    progress: GameProgress,
    onSolved: () -> Unit,
    onBack: () -> Unit,
    difficulty: Difficulty
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
    // Each entry is a snapshot of cells taken before a commit/clear action.
    val history = remember(gameState) {
        val deque = ArrayDeque<List<Int>>()
        progress.history.forEach { deque.addLast(it) }
        deque
    }

    fun saveSnapshot() { history.addLast(cells.toList()) }
    fun undo() {
        val snap = history.removeLastOrNull() ?: return
        snap.forEachIndexed { i, v -> cells[i] = v }
        selectedCell = null
    }

    fun tap(r: Int, c: Int) {
        val idx = r * size + c
        if (pencilMode) {
            // Pencil mode: only UNSET cells can hold candidates.
            if (cells[idx] == CELL_UNSET) {
                selectedCell = if (selectedCell == idx) null else idx
            }
        } else {
            // Normal mode: tap to select any cell; tap again to deselect.
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
            var msg = "I have devoured a whole bowl of Alphabet Soup ($size x $size, ${difficulty.label}) - and it only took me"
            msg += if (minutes == 0L) {
                // Special case for sub-1-minute times: "only 45 seconds!"
                " $seconds seconds! Can you do better?"
            } else {
                " $minutes minutes and $seconds seconds! Can you do better?"
            }
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

    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(8.dp)) {
        val isLandscape = maxWidth > maxHeight

        // In landscape the grid gets half the width; height is the full available height.
        val cellSize = if (isLandscape)
            minOf(maxWidth / (2 * (size + 2)), maxHeight / (size + 2)).coerceAtMost(56.dp)
        else
            (maxWidth / (size + 2)).coerceAtMost(56.dp)

        val sel = selectedCell

        // ── Shared content blocks ─────────────────────────────────────────
        val grid: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    Spacer(Modifier.size(cellSize))
                    for (c in 0 until size) HintCell(gameState.hints.colTop[c], gameState, cellSize)
                    Spacer(Modifier.size(cellSize))
                }
                for (r in 0 until size) {
                    Row {
                        HintCell(gameState.hints.rowLeft[r], gameState, cellSize)
                        for (c in 0 until size) {
                            val idx = r * size + c
                            GridCell(
                                value      = cells[idx],
                                size       = size,
                                cellSize   = cellSize,
                                marks      = pencilMarks[idx],
                                isSelected = selectedCell == idx,
                                onDiagonal = r == c || r + c == size - 1,
                                gameState  = gameState,
                                onClick    = { tap(r, c) }
                            )
                        }
                        HintCell(gameState.hints.rowRight[r], gameState, cellSize)
                    }
                }
                Row {
                    Spacer(Modifier.size(cellSize))
                    for (c in 0 until size) HintCell(gameState.hints.colBottom[c], gameState, cellSize)
                    Spacer(Modifier.size(cellSize))
                }
            }
        }

        val controls: @Composable () -> Unit = {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Value picker ───────────────────────────────────────────
                FlowRow(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement   = Arrangement.spacedBy(6.dp)
                ) {
                    allCandidates.forEach { candidate ->
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
                // ── Controls row: undo + pencil toggle ────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick  = { undo() },
                        enabled  = history.isNotEmpty()
                    ) { Text("Undo") }

                    FilterChip(
                        selected = pencilMode,
                        onClick  = { pencilMode = !pencilMode },
                        label = { Text(if (pencilMode) "Pencil mode: ON" else "Pencil mode: OFF") }
                    )
                }
            }
        }

        // ── Orientation-aware layout ──────────────────────────────────────
        if (isLandscape) {
            Row(
                modifier             = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment    = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) { grid() }
                Box(
                    modifier         = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) { controls() }
            }
        } else {
            Column(
                modifier            = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                grid()
                Spacer(Modifier.height(16.dp))
                controls()
            }
        }
    }
}

// ── Wavy loading indicator ────────────────────────────────────────────────────

@Composable
private fun WavyLoadingIndicator(dotCount: Int = 5) {
    val transition = rememberInfiniteTransition(label = "wavy")
    val color = MaterialTheme.colorScheme.primary
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(40.dp)
    ) {
        repeat(dotCount) { index ->
            val offsetY by transition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0f at 0 using FastOutSlowInEasing
                        -18f at 250 using FastOutSlowInEasing
                        0f at 500
                        // stays at 0 from 500–900 (rest between bounces)
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * 120)
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .offset(y = offsetY.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

// ── Individual cells ──────────────────────────────────────────────────────────

@Composable
private fun HintCell(hintValue: Int?, gameState: GameState, cellSize: Dp) {
    Box(
        modifier = Modifier.size(cellSize),
        contentAlignment = Alignment.Center
    ) {
        if (hintValue != null) {
            Text(
                text       = gameState.letterChar(hintValue).toString(),
                fontWeight = FontWeight.Bold,
                fontSize   = (cellSize.value * 0.40f).sp,
                color      = MaterialTheme.colorScheme.primary
            )
        }
    }
}

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
    val diagTint = MaterialTheme.colorScheme.tertiary
    val bgColor  = if (onDiagonal) lerp(baseColor, diagTint, 0.12f) else baseColor
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                      else            MaterialTheme.colorScheme.outline
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = Modifier
            .size(cellSize)
            .padding(2.dp)
            .background(bgColor, shape)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Pencil marks are only shown while the cell has no committed value
            value == CELL_UNSET && marks.isNotEmpty() ->
                PencilMarksContent(marks, size, cellSize, gameState)

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

// ── Pencil-mark mini grid inside a cell ───────────────────────────────────────

@Composable
private fun PencilMarksContent(marks: Set<Int>, size: Int, cellSize: Dp, gameState: GameState) {
    // All N candidates occupy fixed slots so a mark's position never shifts.
    // Candidates: EMPTY_CELL_SYMBOL (CELL_EMPTY) followed by letters A, B, …
    val cols       = when { size <= 4 -> 2; size <= 7 -> 3; else -> 4 }
    val candidates = listOf(CELL_EMPTY) + (1 until size).toList()
    val fontSize   = (cellSize.value * 0.22f).sp

    Column(
        modifier            = Modifier.fillMaxSize().padding(1.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        candidates.chunked(cols).forEach { rowCands ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowCands.forEach { cand ->
                    Text(
                        text       = if (cand in marks) {
                            if (cand == CELL_EMPTY) EMPTY_CELL_SYMBOL else gameState.letterChar(cand).toString()
                        } else "",
                        fontSize   = fontSize,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.primary,
                        modifier   = Modifier.weight(1f),
                        textAlign  = TextAlign.Center
                    )
                }
                // Pad last row if it has fewer items than cols
                repeat(cols - rowCands.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
