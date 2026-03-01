package com.example.alphabetsoup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alphabetsoup.ui.theme.AlphabetSoupTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    resumableGame: SavedGame?,
    onResume: () -> Unit,
    onNewGame: (Int, Difficulty, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var difficulty          by remember { mutableStateOf(GameSave.lastDifficulty) }
    var useDiagonals        by remember { mutableStateOf(GameSave.lastUseDiagonals) }
    var useSecondHints      by remember { mutableStateOf(GameSave.lastUseSecondHints) }
    var showDiagonalHelp    by remember { mutableStateOf(false) }
    var showSecondHintHelp  by remember { mutableStateOf(false) }

    if (showDiagonalHelp) {
        AlertDialog(
            onDismissRequest = { showDiagonalHelp = false },
            title = { Text("Cross-aint") },
            text  = { Text("When enabled, both main diagonals must also contain each letter exactly once.") },
            confirmButton = { TextButton(onClick = { showDiagonalHelp = false }) { Text("Mhm!") } }
        )
    }
    if (showSecondHintHelp) {
        AlertDialog(
            onDismissRequest = { showSecondHintHelp = false },
            title = { Text("Second Helpings") },
            text  = { Text("When enabled, you get additional hints showing the second visible letter from that direction.") },
            confirmButton = { TextButton(onClick = { showSecondHintHelp = false }) { Text("Yummy!") } }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Text(
            text = "Alphabet Soup",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Can you eat it all?",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ── Resume button (only shown when there's an in-progress game) ───────
        if (resumableGame != null) {
            val s = resumableGame.size
            val mins = resumableGame.elapsedSeconds / 60
            val secs = resumableGame.elapsedSeconds % 60
            Button(
                onClick  = onResume,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text     = "Resume ${s}×${s}  ·  ${resumableGame.difficulty.label}  ·  (%d:%02d)".format(mins, secs),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── New-game size picker ──────────────────────────────────────────────
        Text(
            text = "Take a seat and choose a bowl:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            for (size in 4..6) {
                SizeButton(size = size, onClick = { onNewGame(size, difficulty, useDiagonals, useSecondHints) })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            for (size in 7..8) {
                SizeButton(size = size, onClick = { onNewGame(size, difficulty, useDiagonals, useSecondHints) })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Difficulty picker ─────────────────────────────────────────────────
        Text(
            text = "Pick your sauce:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Difficulty.entries.forEach { d ->
                FilterChip(
                    selected = difficulty == d,
                    onClick  = { difficulty = d },
                    label    = { Text(d.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Diagonal rules toggle ─────────────────────────────────────────────
        Text(
            text = "Anything else?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = useDiagonals,
                onClick  = { useDiagonals = !useDiagonals },
                label    = { Text(if (useDiagonals) "Cross-aint: ON" else "Cross-aint: OFF") }
            )
            TextButton(onClick = { showDiagonalHelp = true }) { Text("?") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = useSecondHints,
                onClick  = { useSecondHints = !useSecondHints },
                label    = { Text(if (useSecondHints) "Second helpings: ON" else "Second helpings: OFF") }
            )
            TextButton(onClick = { showSecondHintHelp = true }) { Text("?") }
        }

        } // Column
    } // BoxWithConstraints
}

@Composable
private fun SizeButton(size: Int, onClick: () -> Unit) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.width(80.dp)
    ) {
        Text(text = "${size}×${size}", fontSize = 16.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    AlphabetSoupTheme {
        WelcomeScreen(resumableGame = null, onResume = {}, onNewGame = { _, _, _, _ -> })
    }
}
