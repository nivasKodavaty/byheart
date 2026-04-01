package com.gtr3.byheart.presentation.navigation

sealed class Screen(val route: String) {
    data object Login      : Screen("login")
    data object Register   : Screen("register")
    data object NotesList  : Screen("notes_list")
    data object CreateNote : Screen("create_note")
    data object NoteDetail : Screen("note_detail/{noteId}") {
        fun route(id: Long) = "note_detail/$id"
    }
}
