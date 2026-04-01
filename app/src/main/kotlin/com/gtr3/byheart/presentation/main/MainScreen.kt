package com.gtr3.byheart.presentation.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gtr3.byheart.presentation.collab.list.CollabNotesListScreen
import com.gtr3.byheart.presentation.notes.list.NotesListScreen
import com.gtr3.byheart.presentation.navigation.Screen

private enum class MainTab(val label: String, val icon: ImageVector, val route: String) {
    Notes ("Notes",  Icons.Filled.Notes, Screen.NotesList.route),
    Collab("Collab", Icons.Filled.Group, Screen.CollabList.route)
}

@Composable
fun MainScreen(
    onNavigateToNoteDetail: (Long) -> Unit,
    onNavigateToCreateNote: () -> Unit,
    onNavigateToCollabDetail: (String) -> Unit
) {
    val bottomNavController = rememberNavController()
    val backStack by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick  = {
                            bottomNavController.navigate(tab.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontWeight = if (currentRoute == tab.route) FontWeight.SemiBold else FontWeight.Normal) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = bottomNavController,
            startDestination = Screen.NotesList.route,
            modifier         = Modifier.fillMaxSize().padding(padding),
            enterTransition  = { fadeIn(tween(200)) },
            exitTransition   = { fadeOut(tween(200)) },
            popEnterTransition  = { fadeIn(tween(200)) },
            popExitTransition   = { fadeOut(tween(200)) }
        ) {
            composable(Screen.NotesList.route) {
                NotesListScreen(
                    onNavigateToDetail = onNavigateToNoteDetail,
                    onNavigateToCreate = onNavigateToCreateNote
                )
            }
            composable(Screen.CollabList.route) {
                CollabNotesListScreen(
                    onNavigateToDetail = onNavigateToCollabDetail
                )
            }
        }
    }
}
