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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.alphabetsoup.ui.theme.AlphabetSoupTheme

private sealed class Screen {
    object Welcome : Screen()
    data class Game(val size: Int) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlphabetSoupTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Welcome) }
                when (val s = screen) {
                    is Screen.Welcome -> Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        WelcomeScreen(
                            modifier      = Modifier.padding(innerPadding),
                            resumableGame = GameSave.getLastSaved(),
                            onResume      = { screen = Screen.Game(GameSave.getLastSaved()!!.size) },
                            onNewGame     = { size ->
                                GameSave.clear(size)   // discard any existing save for this size
                                screen = Screen.Game(size)
                            }
                        )
                    }
                    is Screen.Game -> GameScreen(
                        size   = s.size,
                        onBack = { screen = Screen.Welcome }
                    )
                }
            }
        }
    }
}
