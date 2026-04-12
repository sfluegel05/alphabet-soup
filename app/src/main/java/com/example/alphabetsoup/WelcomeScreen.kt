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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compose.AlphabetSoupTheme
import kotlin.text.ifEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    resumableGame: SavedGame?,
    onResume: () -> Unit,
    onNewGame: (Int, Difficulty, Boolean, Boolean, Boolean) -> Unit,
    onStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSize        by remember { mutableStateOf(5) }
    var difficulty          by remember { mutableStateOf(GameSave.lastDifficulty) }
    var useDiagonals        by remember { mutableStateOf(GameSave.lastUseDiagonals) }
    var useSecondHints      by remember { mutableStateOf(GameSave.lastUseSecondHints) }
    var useSubGrids         by remember { mutableStateOf(GameSave.lastUseSubGrids) }
    var showDiagonalHelp    by remember { mutableStateOf(false) }
    var showSecondHintHelp  by remember { mutableStateOf(false) }
    var showSubGridHelp     by remember { mutableStateOf(false) }

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
    if (showSubGridHelp) {
        AlertDialog(
            onDismissRequest = { showSubGridHelp = false },
            title = { Text("Sub-grids") },
            text  = { Text("When enabled, the grid is divided into ${selectedSize} irregularly shaped regions. Each region must also contain each letter exactly once — just like in Sudoku.") },
            confirmButton = { TextButton(onClick = { showSubGridHelp = false }) { Text("Tasty!") } }
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
            val settings_string = listOfNotNull(
                DifficultySetting.emoji(resumableGame.difficulty).ifEmpty { null },
                DiagonalsSetting.emoji(resumableGame.useDiagonals).ifEmpty { null },
                SecondHintsSetting.emoji(resumableGame.useSecondHints).ifEmpty { null },
                SubGridsSetting.emoji(resumableGame.useSubGrids).ifEmpty { null }).joinToString(" ")
            Button(
                onClick  = onResume,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(

                            text     = "Resume ${s}×${s}  ·  ${settings_string}  ·  (%d:%02d)".format(mins, secs),
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
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            for (size in 4..6) {
                SizeButton(size = size, selected = selectedSize == size, onClick = { selectedSize = size })
            }
        }

        Spacer(modifier = Modifier.height(0.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            for (size in 7..8) {
                SizeButton(size = size, selected = selectedSize == size, onClick = { selectedSize = size })
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
            DifficultySetting.options.forEach { option ->
                FilterChip(
                    selected = difficulty == option.value,
                    onClick  = { difficulty = option.value },
                    label    = { Text(option.displayName) }
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
                label    = { Text("${DiagonalsSetting.displayName(true)}: ${if (useDiagonals) "ON" else "OFF"}") }
            )
            TextButton(onClick = { showDiagonalHelp = true }) { Text("?") }
        }

        Spacer(modifier = Modifier.height(0.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = useSecondHints,
                onClick  = { useSecondHints = !useSecondHints },
                label    = { Text("${SecondHintsSetting.displayName(true)}: ${if (useSecondHints) "ON" else "OFF"}") }
            )
            TextButton(onClick = { showSecondHintHelp = true }) { Text("?") }
        }

        Spacer(modifier = Modifier.height(0.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = useSubGrids,
                onClick  = { useSubGrids = !useSubGrids },
                label    = { Text("${SubGridsSetting.displayName(true)}: ${if (useSubGrids) "ON" else "OFF"}") }
            )
            TextButton(onClick = { showSubGridHelp = true }) { Text("?") }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick  = { onNewGame(selectedSize, difficulty, useDiagonals, useSecondHints, useSubGrids) },
            //modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Place your order!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onStats) {
            Text("View Statistics")
        }

        val context = LocalContext.current
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sfluegel05/alphabet-soup/issues")))
        }) {
            Text("Complaints? Suggestions?")
        }

        Spacer(modifier = Modifier.height(8.dp))

        val versionName = remember {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }
        Text(
            text = "v$versionName",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        } // Column
    } // BoxWithConstraints
}

@Composable
private fun SizeButton(size: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(text = "${size}×${size}", fontSize = 16.sp) },
        //modifier = Modifier.width(80.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    AlphabetSoupTheme {
        WelcomeScreen(resumableGame = null, onResume = {}, onNewGame = { _, _, _, _, _ -> }, onStats = {})
    }
}
