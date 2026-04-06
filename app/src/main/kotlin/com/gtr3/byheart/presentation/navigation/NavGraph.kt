package com.gtr3.byheart.presentation.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gtr3.byheart.presentation.auth.LoginScreen
import com.gtr3.byheart.presentation.auth.RegisterScreen
import com.gtr3.byheart.presentation.collab.detail.CollabNoteDetailScreen
import com.gtr3.byheart.presentation.main.MainScreen
import com.gtr3.byheart.presentation.notes.detail.NoteDetailScreen
import com.gtr3.byheart.presentation.settings.SettingsScreen

// ── Durations ─────────────────────────────────────────────────────────────────
private const val SLIDE_MS  = 320
private const val FADE_MS   = 260
private const val DETAIL_MS = 380

// ── Auth — horizontal page slide ─────────────────────────────────────────────
private val slideInFromRight  = slideInHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it }
private val slideOutToLeft    = slideOutHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { -it }
private val slideInFromLeft   = slideInHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { -it }
private val slideOutToRight   = slideOutHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it }

// ── Auth → App — welcoming zoom-fade ─────────────────────────────────────────
private val zoomFadeIn  = fadeIn(tween(FADE_MS))  + scaleIn(tween(FADE_MS), initialScale = 0.94f)
private val zoomFadeOut = fadeOut(tween(FADE_MS)) + scaleOut(tween(FADE_MS), targetScale  = 1.04f)

// ── List — gentle background fade-scale ──────────────────────────────────────
private val listEnter = fadeIn(tween(FADE_MS))  + scaleIn(tween(FADE_MS), initialScale = 0.97f)
private val listExit  = fadeOut(tween(FADE_MS)) + scaleOut(tween(FADE_MS), targetScale  = 0.97f)

// ── Detail — signature spring-lift ────────────────────────────────────────────
private val detailEnter = slideInVertically(
    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
    initialOffsetY = { it }
) + fadeIn(tween(DETAIL_MS / 2))

private val detailExit = slideOutVertically(
    animationSpec = tween(DETAIL_MS - 40, easing = FastOutSlowInEasing),
    targetOffsetY = { it }
) + fadeOut(tween(DETAIL_MS / 2))

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String         = Screen.Login.route,
    pendingNoteId: Long?             = null
) {

    LaunchedEffect(pendingNoteId) {
        if (pendingNoteId != null && startDestination == Screen.Main.route) {
            navController.navigate(Screen.NoteDetail.route(pendingNoteId))
        }
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination,
        enterTransition  = { fadeIn(tween(FADE_MS)) },
        exitTransition   = { fadeOut(tween(FADE_MS)) },
        popEnterTransition  = { fadeIn(tween(FADE_MS)) },
        popExitTransition   = { fadeOut(tween(FADE_MS)) }
    ) {

        // ── Login ─────────────────────────────────────────────────────────────
        composable(
            route               = Screen.Login.route,
            enterTransition     = { slideInFromLeft },
            exitTransition      = { zoomFadeOut },
            popEnterTransition  = { slideInFromLeft },
            popExitTransition   = { slideOutToRight }
        ) {
            LoginScreen(
                onNavigateToNotes    = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        // ── Register ──────────────────────────────────────────────────────────
        composable(
            route               = Screen.Register.route,
            enterTransition     = { slideInFromRight },
            exitTransition      = { zoomFadeOut },
            popEnterTransition  = { slideInFromLeft },
            popExitTransition   = { slideOutToRight }
        ) {
            RegisterScreen(
                onNavigateToNotes = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── Main (bottom-nav shell with Notes + Collab tabs) ──────────────────
        composable(
            route               = Screen.Main.route,
            enterTransition     = { zoomFadeIn },
            exitTransition      = { listExit },
            popEnterTransition  = { listEnter },
            popExitTransition   = { listExit }
        ) {
            MainScreen(
                onNavigateToNoteDetail   = { id -> navController.navigate(Screen.NoteDetail.route(id)) },
                onNavigateToCreateNote   = { navController.navigate(Screen.CreateNote.route) },
                onNavigateToCollabDetail = { shareCode -> navController.navigate(Screen.CollabDetail.route(shareCode)) },
                onNavigateToSettings     = { navController.navigate(Screen.Settings.route) }
            )
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(
            route               = Screen.Settings.route,
            enterTransition     = { detailEnter },
            exitTransition      = { detailExit },
            popEnterTransition  = { listEnter },
            popExitTransition   = { detailExit }
        ) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout       = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Create Note ───────────────────────────────────────────────────────
        composable(
            route               = Screen.CreateNote.route,
            enterTransition     = { detailEnter },
            exitTransition      = { detailExit },
            popEnterTransition  = { listEnter },
            popExitTransition   = { detailExit }
        ) {
            NoteDetailScreen(
                noteId             = null,
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToDetail = { id ->
                    navController.navigate(Screen.NoteDetail.route(id)) {
                        popUpTo(Screen.CreateNote.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Note Detail ───────────────────────────────────────────────────────
        composable(
            route     = Screen.NoteDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
            enterTransition     = { detailEnter },
            exitTransition      = { detailExit },
            popEnterTransition  = { listEnter },
            popExitTransition   = { detailExit }
        ) { backStack ->
            NoteDetailScreen(
                noteId             = backStack.arguments?.getLong("noteId"),
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToDetail = {}
            )
        }

        // ── Collab Note Detail ────────────────────────────────────────────────
        composable(
            route     = Screen.CollabDetail.route,
            arguments = listOf(navArgument("shareCode") { type = NavType.StringType }),
            enterTransition     = { detailEnter },
            exitTransition      = { detailExit },
            popEnterTransition  = { listEnter },
            popExitTransition   = { detailExit }
        ) { backStack ->
            CollabNoteDetailScreen(
                shareCode      = backStack.arguments?.getString("shareCode") ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
