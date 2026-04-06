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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.gtr3.byheart.core.auth.AuthEventBus
import com.gtr3.byheart.presentation.navigation.NavGraph
import com.gtr3.byheart.presentation.navigation.Screen
import com.gtr3.byheart.presentation.theme.ByHeartTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var authEventBus: AuthEventBus

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
                val navController: NavHostController = rememberNavController()

                // ── Single place: status bar & nav bar icon colours ────────────
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars     = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }
                }

                // ── Session expiry: force logout to Login on 401 ───────────────
                LaunchedEffect(Unit) {
                    authEventBus.unauthorizedEvent.collect {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                // ── Single place: system-bar padding + IME padding ─────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .imePadding()
                ) {
                    val startDest = viewModel.startDestination
                    if (startDest != null) {
                        NavGraph(
                            navController    = navController,
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
