package com.example.alphabetsoup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alphabetsoup.ui.theme.AlphabetSoupTheme

@Composable
fun WelcomeScreen(
    resumableGame: SavedGame?,
    onResume: () -> Unit,
    onNewGame: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
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
                    text     = "Resume ${s}×${s}  (%d:%02d)".format(mins, secs),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── New-game size picker ──────────────────────────────────────────────
        Text(
            text = "Start a new game:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            for (size in 4..6) {
                SizeButton(size = size, onClick = { onNewGame(size) })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            for (size in 7..8) {
                SizeButton(size = size, onClick = { onNewGame(size) })
            }
        }
    }
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
        WelcomeScreen(resumableGame = null, onResume = {}, onNewGame = {})
    }
}
