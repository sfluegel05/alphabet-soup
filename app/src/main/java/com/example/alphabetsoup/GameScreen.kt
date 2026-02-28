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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// ── Screen entry point ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(size: Int, onBack: () -> Unit) {
    var gameState by remember { mutableStateOf<GameState?>(null) }

    var elapsedSeconds by remember { mutableStateOf(0L) }
    var timerActive    by remember { mutableStateOf(false) }

    LaunchedEffect(size) {
        val state = withContext(Dispatchers.Default) {
            PuzzleGenerator.generateGame(size)
        }
        gameState = state
        timerActive = true          // start counting once the puzzle is on screen
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            PuzzleBoard(
                gameState      = state,
                modifier       = Modifier.padding(innerPadding),
                elapsedSeconds = elapsedSeconds,
                onSolved       = { timerActive = false },
                onBack         = onBack
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
    onSolved: () -> Unit,
    onBack: () -> Unit
) {
    val size    = gameState.size
    val context = LocalContext.current

    // Flat list: index = r * size + c; values: CELL_UNSET / CELL_EMPTY / 1..N-1
    val cells = remember(gameState) {
        mutableStateListOf(*Array(size * size) { CELL_UNSET })
    }

    // Pencil marks: one Set<Int> per cell.
    // Holds candidate values the player isn't sure about yet:
    //   CELL_EMPTY (0) = "maybe this cell is the empty one"
    //   1..N-1         = "maybe this letter goes here"
    // Contents are preserved even when a cell gets a committed value,
    // but only displayed while the cell is still CELL_UNSET.
    val pencilMarks = remember(gameState) {
        mutableStateListOf(*Array<Set<Int>>(size * size) { emptySet() })
    }

    var pencilMode   by remember { mutableStateOf(false) }
    var selectedCell by remember { mutableStateOf<Int?>(null) }  // flat index

    // Values shown in the picker (○ first, then letters A…)
    val allCandidates = remember(size) { listOf(CELL_EMPTY) + (1 until size).toList() }

    // ── Undo history ───────────────────────────────────────────────────────
    // Each entry is a snapshot of cells taken before a commit/clear action.
    val history = remember(gameState) { ArrayDeque<List<Int>>() }

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

    var showSolvedDialog by remember { mutableStateOf(false) }

    // Stop the timer the instant the puzzle is solved, then show the dialog.
    LaunchedEffect(isSolved) {
        if (isSolved) {
            onSolved()
            showSolvedDialog = true
        }
    }

    if (showSolvedDialog) {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60

        fun shareBrag() {
            var msg = "I have devoured a whole bowl of Alphabet Soup - and it only took me"
            msg += if (minutes == 0L) {
                // Special case for sub-1-minute times: "only 45 seconds!"
                "$seconds seconds! Can you do better?"
            } else {
                "$minutes minutes and $seconds seconds! Can you do better?"
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
                    "Congratulations — you managed to solve the puzzle!\n\n" +
                    "Your time: ${minutes}m ${seconds}s"
                )
            },
            confirmButton = {
                TextButton(onClick = { shareBrag() }) { Text("Brag about this") }
                TextButton(onClick = onBack)           { Text("Back to Menu") }
            },
            dismissButton = {
                TextButton(onClick = { showSolvedDialog = false }) { Text("Admire the Soup") }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Grid with surrounding hints ────────────────────────────────────
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cellSize = (maxWidth / (size + 2)).coerceAtMost(56.dp)

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

        Spacer(Modifier.height(16.dp))

        // ── Letter legend ──────────────────────────────────────────────────
        Text(
            text  = "Letters: " + (1 until size).joinToString(" ") { gameState.letterChar(it).toString() },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        // ── Controls row: undo + pencil toggle ────────────────────────────
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
                onClick  = {
                    pencilMode = !pencilMode
                    selectedCell = null       // clear selection when switching modes
                },
                label = { Text(if (pencilMode) "Pencil mode: ON" else "Pencil mode: OFF") }
            )
        }

        // ── Value picker (shown whenever any cell is selected) ─────────────
        // Normal mode: chips commit/clear the cell's value (one active at most).
        // Pencil mode: chips toggle candidates (multiple may be active).
        val sel = selectedCell
        if (sel != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text  = if (pencilMode) "Mark candidates:" else "Enter value:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement   = Arrangement.spacedBy(6.dp)
            ) {
                allCandidates.forEach { candidate ->
                    val isSelected = if (pencilMode) candidate in pencilMarks[sel]
                                     else            cells[sel] == candidate
                    FilterChip(
                        selected = isSelected,
                        onClick  = {
                            if (pencilMode) toggleCandidate(candidate)
                            else            commitValue(candidate)
                        },
                        label = {
                            Text(
                                if (candidate == CELL_EMPTY) "○"
                                else gameState.letterChar(candidate).toString()
                            )
                        }
                    )
                }
            }
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
    gameState: GameState,
    onClick: () -> Unit
) {
    val bgColor = when (value) {
        CELL_UNSET -> MaterialTheme.colorScheme.surface
        CELL_EMPTY -> MaterialTheme.colorScheme.surfaceVariant
        else       -> MaterialTheme.colorScheme.primaryContainer
    }
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
                text     = "·",
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
    // Candidates: ○ (CELL_EMPTY) followed by letters A, B, …
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
                            if (cand == CELL_EMPTY) "○" else gameState.letterChar(cand).toString()
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
