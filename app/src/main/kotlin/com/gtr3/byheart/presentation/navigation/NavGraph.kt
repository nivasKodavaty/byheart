package com.gtr3.byheart.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gtr3.byheart.presentation.auth.LoginScreen
import com.gtr3.byheart.presentation.auth.RegisterScreen
import com.gtr3.byheart.presentation.notes.detail.NoteDetailScreen
import com.gtr3.byheart.presentation.notes.list.NotesListScreen

@Composable
fun NavGraph(startDestination: String = Screen.Login.route) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToNotes    = { navController.navigate(Screen.NotesList.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }},
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToNotes = { navController.navigate(Screen.NotesList.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }},
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.NotesList.route) {
            NotesListScreen(
                onNavigateToDetail = { id -> navController.navigate(Screen.NoteDetail.route(id)) },
                onNavigateToCreate = { navController.navigate(Screen.CreateNote.route) }
            )
        }

        composable(Screen.CreateNote.route) {
            NoteDetailScreen(
                noteId          = null,
                onNavigateBack  = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStack ->
            NoteDetailScreen(
                noteId         = backStack.arguments?.getLong("noteId"),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
