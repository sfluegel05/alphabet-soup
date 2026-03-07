package com.example.alphabetsoup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.alphabetsoup.ui.theme.AlphabetSoupTheme

private sealed class Screen {
    object Welcome    : Screen()
    object Statistics : Screen()
    data class Game(val size: Int, val difficulty: Difficulty, val useDiagonals: Boolean, val useSecondHints: Boolean) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SolveHistory.init(this)
        setContent {
            AlphabetSoupTheme {
                // Encoded as Int so rememberSaveable survives configuration changes.
                // 0 = Welcome; -1 = Statistics
                // Game = size*1000 + difficulty.ordinal*100 + (if useDiagonals 10 else 0) + (if useSecondHints 1 else 0)
                // min Game value = 4*1000 = 4000, so 0/-1 are unambiguous.
                var encodedScreen by rememberSaveable { mutableStateOf(0) }
                val screen: Screen = when {
                    encodedScreen == 0  -> Screen.Welcome
                    encodedScreen == -1 -> Screen.Statistics
                    else                -> Screen.Game(
                        size           = encodedScreen / 1000,
                        difficulty     = Difficulty.entries[(encodedScreen / 100) % 10],
                        useDiagonals   = (encodedScreen / 10) % 10 == 1,
                        useSecondHints = encodedScreen % 10 == 1
                    )
                }
                when (val s = screen) {
                    is Screen.Statistics -> StatisticsScreen(onBack = { encodedScreen = 0 })
                    is Screen.Welcome -> Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        WelcomeScreen(
                            modifier      = Modifier.padding(innerPadding),
                            resumableGame = GameSave.getLastSaved(),
                            onStats       = { encodedScreen = -1 },
                            onResume      = {
                                val saved = GameSave.getLastSaved()!!
                                encodedScreen = saved.size * 1000 + saved.difficulty.ordinal * 100 + (if (saved.useDiagonals) 10 else 0) + (if (saved.useSecondHints) 1 else 0)
                            },
                            onNewGame     = { size, difficulty, useDiagonals, useSecondHints ->
                                GameSave.clear(size)
                                GameSave.recordDifficulty(difficulty)
                                GameSave.recordUseDiagonals(useDiagonals)
                                GameSave.recordUseSecondHints(useSecondHints)
                                encodedScreen = size * 1000 + difficulty.ordinal * 100 + (if (useDiagonals) 10 else 0) + (if (useSecondHints) 1 else 0)
                            }
                        )
                    }
                    is Screen.Game -> GameScreen(
                        size           = s.size,
                        difficulty     = s.difficulty,
                        useDiagonals   = s.useDiagonals,
                        useSecondHints = s.useSecondHints,
                        onBack         = { encodedScreen = 0 }
                    )
                }
            }
        }
    }
}
