package com.gtr3.byheart

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.gtr3.byheart.presentation.navigation.NavGraph
import com.gtr3.byheart.presentation.theme.ByHeartTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { viewModel.startDestination == null }

        val pendingNoteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L).takeIf { it != -1L }

        // Lay content out behind both status bar and navigation bar
        enableEdgeToEdge()

        setContent {
            ByHeartTheme {
                val isDark = isSystemInDarkTheme()
                val view = LocalView.current

                // ── Single place: status bar & nav bar icon colours ────────────
                // Light mode  → dark icons on light bg
                // Dark mode   → light icons on dark bg
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars     = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }
                }

                // ── Single place: system-bar padding + IME padding ─────────────
                // windowInsetsPadding(systemBars) reserves space for the status
                // bar (top) and nav bar (bottom) once, for the whole app.
                // imePadding() sits below that and grows when the software
                // keyboard appears, pushing the focused bottom-bar up.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .imePadding()
                ) {
                    val startDest = viewModel.startDestination
                    if (startDest != null) {
                        NavGraph(
                            startDestination = startDest,
                            pendingNoteId    = pendingNoteId
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "noteId"
    }
}
