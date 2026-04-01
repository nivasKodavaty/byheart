@file:OptIn(ExperimentalMaterial3Api::class)

package com.gtr3.byheart.presentation.collab.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gtr3.byheart.domain.model.CollabNote
import com.gtr3.byheart.presentation.theme.AppleYellow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CollabNotesListScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: CollabNotesListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CollabNotesListEffect.NavigateToDetail -> onNavigateToDetail(effect.shareCode)
                is CollabNotesListEffect.NavigateToCreate -> onNavigateToDetail(effect.shareCode)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Collab",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Join existing note
                SmallFloatingActionButton(
                    onClick = { viewModel.onIntent(CollabNotesListIntent.ShowJoinSheet) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor   = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Filled.Link, contentDescription = "Join note")
                }
                // Create new collab note
                ExtendedFloatingActionButton(
                    onClick          = { viewModel.onIntent(CollabNotesListIntent.ShowCreateSheet) },
                    containerColor   = AppleYellow,
                    contentColor     = MaterialTheme.colorScheme.onPrimary,
                    icon             = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text             = { Text("New Collab", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.notes.isEmpty() && !state.isLoading -> {
                    CollabEmptyState(
                        onCreateClick = { viewModel.onIntent(CollabNotesListIntent.ShowCreateSheet) },
                        onJoinClick   = { viewModel.onIntent(CollabNotesListIntent.ShowJoinSheet) }
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.notes, key = { it.shareCode }) { note ->
                            CollabNoteCard(
                                note      = note,
                                onClick   = { onNavigateToDetail(note.shareCode) },
                                onCopyCode = {
                                    clipboard.setText(AnnotatedString(note.shareCode))
                                }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            state.error?.let {
                Text(
                    it,
                    color    = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // ── Create sheet ──────────────────────────────────────────────────────────
    if (state.showCreateSheet) {
        CreateCollabNoteSheet(
            title      = state.createTitle,
            useAi      = state.createUseAi,
            isCreating = state.isCreating,
            onTitleChange = { viewModel.onIntent(CollabNotesListIntent.CreateTitleChanged(it)) },
            onUseAiToggle = { viewModel.onIntent(CollabNotesListIntent.CreateUseAiToggled(it)) },
            onCreate   = { viewModel.onIntent(CollabNotesListIntent.CreateNote) },
            onDismiss  = { viewModel.onIntent(CollabNotesListIntent.DismissCreateSheet) }
        )
    }

    // ── Join sheet ────────────────────────────────────────────────────────────
    if (state.showJoinSheet) {
        JoinCollabNoteSheet(
            code       = state.joinCode,
            isJoining  = state.isJoining,
            error      = state.joinError,
            onCodeChange = { viewModel.onIntent(CollabNotesListIntent.JoinCodeChanged(it)) },
            onJoin     = { viewModel.onIntent(CollabNotesListIntent.JoinNote) },
            onDismiss  = { viewModel.onIntent(CollabNotesListIntent.DismissJoinSheet) }
        )
    }
}

// ── Collab note card ──────────────────────────────────────────────────────────

@Composable
private fun CollabNoteCard(
    note: CollabNote,
    onClick: () -> Unit,
    onCopyCode: () -> Unit
) {
    Surface(
        modifier       = Modifier.fillMaxWidth().clickable { onClick() },
        shape          = RoundedCornerShape(16.dp),
        color          = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.5.dp,
        tonalElevation  = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Yellow accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppleYellow)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    note.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Share code pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            note.shareCode,
                            style    = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color    = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    // Participant count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(3.dp))
                        Text("${note.participantCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    note.lastEditedBy?.let {
                        Text("by $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
            // Copy code icon
            IconButton(onClick = onCopyCode) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun CollabEmptyState(onCreateClick: () -> Unit, onJoinClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).background(AppleYellow.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(40.dp), tint = AppleYellow)
        }
        Spacer(Modifier.height(20.dp))
        Text("Collaborate in real time", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create a shared note and send the invite code to anyone. Everyone edits together.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onCreateClick,
            colors  = ButtonDefaults.buttonColors(containerColor = AppleYellow, contentColor = MaterialTheme.colorScheme.onPrimary),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Create Collab Note", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onJoinClick, modifier = Modifier.fillMaxWidth()) {
            Text("Join with Code")
        }
    }
}

// ── Create sheet ──────────────────────────────────────────────────────────────

@Composable
private fun CreateCollabNoteSheet(
    title: String,
    useAi: Boolean,
    isCreating: Boolean,
    onTitleChange: (String) -> Unit,
    onUseAiToggle: (Boolean) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("New Collaborative Note", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value         = title,
                onValueChange = onTitleChange,
                label         = { Text("Note title") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(onDone = { if (!isCreating) onCreate() })
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Generate with AI", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("Let AI draft the initial content", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked         = useAi,
                    onCheckedChange = onUseAiToggle,
                    colors          = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = AppleYellow)
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick  = onCreate,
                enabled  = title.isNotBlank() && !isCreating,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = AppleYellow, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                AnimatedContent(targetState = isCreating) { loading ->
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Create & Get Code", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Join sheet ────────────────────────────────────────────────────────────────

@Composable
private fun JoinCollabNoteSheet(
    code: String,
    isJoining: Boolean,
    error: String?,
    onCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
    onDismiss: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Join a Note", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text("Enter the 6-character invite code shared by the note creator.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value         = code,
                onValueChange = { onCodeChange(it.uppercase().take(6)) },
                label         = { Text("Invite code") },
                placeholder   = { Text("E.g.  XK7P2Q") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                isError       = error != null,
                supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                textStyle     = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 6.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Characters),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); if (!isJoining) onJoin() })
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick  = onJoin,
                enabled  = code.length == 6 && !isJoining,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = AppleYellow, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                AnimatedContent(targetState = isJoining) { loading ->
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Join Note", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
