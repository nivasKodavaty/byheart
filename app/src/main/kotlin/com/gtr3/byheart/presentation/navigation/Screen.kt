package com.gtr3.byheart.presentation.navigation

sealed class Screen(val route: String) {
    data object Login      : Screen("login")
    data object Register   : Screen("register")
    data object Main       : Screen("main")          // bottom-nav shell
    data object NotesList  : Screen("notes_list")
    data object CreateNote : Screen("create_note")
    data object NoteDetail : Screen("note_detail/{noteId}") {
        fun route(id: Long) = "note_detail/$id"
    }
    data object CollabList   : Screen("collab_list")
    data object CollabCreate : Screen("collab_create")
    data object CollabDetail : Screen("collab_detail/{shareCode}") {
        fun route(shareCode: String) = "collab_detail/$shareCode"
    }
}
