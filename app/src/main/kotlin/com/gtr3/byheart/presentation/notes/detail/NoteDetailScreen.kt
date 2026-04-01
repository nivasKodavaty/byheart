package com.gtr3.byheart.presentation.notes.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(noteId) {
        noteId?.let { viewModel.onIntent(NoteDetailIntent.LoadNote(it)) }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                NoteDetailEffect.NavigateBack     -> onNavigateBack()
                is NoteDetailEffect.ShowError     -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "New Note" else state.note?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.note != null -> NoteContentView(state.note!!.content)
                else -> CreateNoteView(
                    title = state.titleInput,
                    error = state.error,
                    onTitleChange = { viewModel.onIntent(NoteDetailIntent.TitleChanged(it)) },
                    onGenerate = { viewModel.onIntent(NoteDetailIntent.CreateNote) }
                )
            }
        }
    }
}

@Composable
private fun CreateNoteView(
    title: String,
    error: String?,
    onTitleChange: (String) -> Unit,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("What's on your mind?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Enter a title") },
            placeholder = { Text("e.g. Easy chicken recipe, Bahubali movie, Morning habits...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Generate Note with AI")
        }
    }
}

@Composable
private fun NoteContentView(content: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        content?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
