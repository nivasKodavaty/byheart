@file:OptIn(ExperimentalMaterial3Api::class)

package com.gtr3.byheart.presentation.collab.detail

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gtr3.byheart.core.notification.ReminderScheduler
import com.gtr3.byheart.domain.model.Message
import com.gtr3.byheart.presentation.theme.AppleYellow
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun CollabNoteDetailScreen(
    shareCode: String,
    onNavigateBack: () -> Unit,
    viewModel: CollabNoteDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val richTextState = rememberRichTextState()
    var isInitializing by remember { mutableStateOf(false) }
    var capturedRange by remember { mutableStateOf(TextRange.Zero) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Undo / Redo ────────────────────────────────────────────────────────────
    val undoStack = remember { ArrayDeque<String>() }
    val redoStack = remember { ArrayDeque<String>() }
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    fun pushUndo(html: String) {
        if (undoStack.lastOrNull() == html) return
        undoStack.addLast(html)
        if (undoStack.size > 5) undoStack.removeFirst()
        redoStack.clear()
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // ── Load note ──────────────────────────────────────────────────────────────
    LaunchedEffect(shareCode) {
        viewModel.onIntent(CollabNoteDetailIntent.Load(shareCode))
    }

    // ── Polling — every 5 seconds ──────────────────────────────────────────────
    LaunchedEffect(shareCode) {
        while (true) {
            delay(5_000L)
            viewModel.onIntent(CollabNoteDetailIntent.PollForUpdates)
        }
    }

    // ── Initial content load into editor ──────────────────────────────────────
    LaunchedEffect(state.note?.shareCode) {
        val note = state.note ?: return@LaunchedEffect
        isInitializing = true
        val content = note.content ?: ""
        if (content.startsWith("<")) richTextState.setHtml(content)
        else richTextState.setMarkdown(content)
        isInitializing = false
        pushUndo(richTextState.toHtml())
    }

    // ── AI / poll content version reload ──────────────────────────────────────
    LaunchedEffect(state.aiContentVersion) {
        if (state.aiContentVersion == 0) return@LaunchedEffect
        isInitializing = true
        val content = state.note?.content ?: ""
        if (content.startsWith("<")) richTextState.setHtml(content)
        else richTextState.setMarkdown(content)
        isInitializing = false
        pushUndo(richTextState.toHtml())
    }

    // ── Effects ────────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                CollabNoteDetailEffect.NavigateBack -> onNavigateBack()
                CollabNoteDetailEffect.NoteLeft     -> onNavigateBack()
                CollabNoteDetailEffect.ReloadEditor -> {
                    isInitializing = true
                    val content = state.note?.content ?: ""
                    if (content.startsWith("<")) richTextState.setHtml(content)
                    else richTextState.setMarkdown(content)
                    isInitializing = false
                    pushUndo(richTextState.toHtml())
                }
                is CollabNoteDetailEffect.ConflictDetected -> {
                    // Reload editor with latest server content
                    isInitializing = true
                    val latest = effect.latestContent ?: ""
                    if (latest.startsWith("<")) richTextState.setHtml(latest)
                    else richTextState.setMarkdown(latest)
                    isInitializing = false
                    pushUndo(richTextState.toHtml())
                    snackbarHostState.showSnackbar(
                        message     = "A collaborator saved changes. Your edit was not applied — latest version loaded.",
                        duration    = SnackbarDuration.Long,
                        withDismissAction = true
                    )
                    viewModel.onIntent(CollabNoteDetailIntent.AcceptConflict)
                }
                is CollabNoteDetailEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is CollabNoteDetailEffect.ScheduleReminder -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    ReminderScheduler.schedule(
                        context          = context,
                        noteId           = effect.noteId,
                        noteTitle        = effect.noteTitle,
                        reminderTitle    = effect.reminderTitle,
                        triggerAtMillis  = effect.triggerAtMillis,
                        reminderDays     = effect.reminderDays
                    )
                }
            }
        }
    }

    // ── Listen to rich-text edits for undo debounce ───────────────────────────
    LaunchedEffect(richTextState.annotatedString) {
        if (isInitializing) return@LaunchedEffect
        viewModel.onIntent(CollabNoteDetailIntent.EditedContentChanged(richTextState.toHtml()))
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(800L)
            pushUndo(richTextState.toHtml())
        }
    }

    // ── Apply selection replacement ────────────────────────────────────────────
    val onApplyReplacement: () -> Unit = {
        state.selectionReplacement?.let { replacement ->
            val html = richTextState.toHtml()
            val spliced = spliceHtmlAtPlainRange(html, state.selectionStart, state.selectionEnd, replacement)
            isInitializing = true
            richTextState.setHtml(spliced)
            isInitializing = false
            pushUndo(richTextState.toHtml())
            viewModel.onIntent(CollabNoteDetailIntent.ApplySelectionReplacement)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CollabDetailTopBar(
                shareCode    = state.note?.shareCode ?: shareCode,
                participants = state.note?.participantCount ?: 0,
                isSaving     = state.isSaving,
                hasChanges   = state.hasUnsavedChanges,
                onBack       = { viewModel.onIntent(CollabNoteDetailIntent.NavigateBack) },
                onSave       = { viewModel.onIntent(CollabNoteDetailIntent.SaveContent(richTextState.toHtml())) },
                onShareCode  = { viewModel.onIntent(CollabNoteDetailIntent.ShowShareCode) },
                onLeave      = { viewModel.onIntent(CollabNoteDetailIntent.LeaveNote) }
            )
        },
        bottomBar = {
            Column {
                // Selection replacement banner
                AnimatedVisibility(
                    visible = state.selectionReplacement != null,
                    enter   = fadeIn(),
                    exit    = fadeOut()
                ) {
                    SelectionReplacementBanner(
                        replacement = state.selectionReplacement ?: "",
                        onApply     = onApplyReplacement,
                        onDiscard   = { viewModel.onIntent(CollabNoteDetailIntent.DiscardSelectionReplacement) }
                    )
                }
                // Formatting toolbar
                CollabFormattingToolbar(
                    richTextState = richTextState,
                    canUndo       = undoStack.size > 1,
                    canRedo       = redoStack.isNotEmpty(),
                    onUndo = {
                        if (undoStack.size > 1) {
                            redoStack.addLast(undoStack.removeLast())
                            isInitializing = true
                            richTextState.setHtml(undoStack.last())
                            isInitializing = false
                        }
                    },
                    onRedo = {
                        if (redoStack.isNotEmpty()) {
                            val next = redoStack.removeLast()
                            undoStack.addLast(next)
                            isInitializing = true
                            richTextState.setHtml(next)
                            isInitializing = false
                        }
                    }
                )
                // Unified AI input
                CollabAiInputBar(
                    aiInput          = state.aiInput,
                    isLoading        = state.isAiLoading || state.isSelectionLoading,
                    selectionText    = state.selectionText,
                    onValueChange    = { viewModel.onIntent(CollabNoteDetailIntent.AiInputChanged(it)) },
                    onSend           = { viewModel.onIntent(CollabNoteDetailIntent.SendAiMessage) },
                    onDismissSelect  = { viewModel.onIntent(CollabNoteDetailIntent.DismissSelectionEdit) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Title field
            BasicTextField(
                value         = state.editedTitle,
                onValueChange = { viewModel.onIntent(CollabNoteDetailIntent.TitleChanged(it)) },
                textStyle     = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (state.editedTitle.isEmpty()) {
                        Text("Untitled", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    inner()
                }
            )

            // Last-edited-by + participant info row
            state.note?.let { note ->
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    note.lastEditedBy?.let {
                        Text("Edited by $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${note.participantCount} collaborator${if (note.participantCount != 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp)

            // Rich text editor
            RichTextEditor(
                state    = richTextState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                colors   = RichTextEditorDefaults.richTextEditorColors(
                    containerColor    = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor       = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 26.sp
                )
            )
        }
    }

    // ── Share code sheet ───────────────────────────────────────────────────────
    if (state.showShareCodeSheet) {
        ShareCodeSheet(
            shareCode = state.note?.shareCode ?: shareCode,
            onDismiss = { viewModel.onIntent(CollabNoteDetailIntent.DismissShareCode) }
        )
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun CollabDetailTopBar(
    shareCode: String,
    participants: Int,
    isSaving: Boolean,
    hasChanges: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onShareCode: () -> Unit,
    onLeave: () -> Unit
) {
    var showLeaveConfirm by remember { mutableStateOf(false) }

    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            // Share code chip
            Surface(
                shape  = RoundedCornerShape(20.dp),
                color  = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.clickable { onShareCode() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(shareCode, style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(4.dp))
            // Save
            AnimatedVisibility(visible = hasChanges) {
                FilledTonalButton(
                    onClick  = onSave,
                    enabled  = !isSaving,
                    modifier = Modifier.height(36.dp),
                    colors   = ButtonDefaults.filledTonalButtonColors(containerColor = AppleYellow, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Save", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
            // Leave
            IconButton(onClick = { showLeaveConfirm = true }) {
                Icon(Icons.Filled.ExitToApp, contentDescription = "Leave note", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title  = { Text("Leave note?") },
            text   = { Text("You'll no longer have access unless you rejoin with the code.") },
            confirmButton = {
                TextButton(onClick = { showLeaveConfirm = false; onLeave() }) {
                    Text("Leave", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Formatting toolbar ────────────────────────────────────────────────────────

@Composable
private fun CollabFormattingToolbar(
    richTextState: RichTextState,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Surface(
        color       = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo / Redo
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(18.dp))
            }
            VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 4.dp))
            // Bold
            FormatButton(
                active  = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold,
                onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) }
            ) { Text("B", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) }
            // Italic
            FormatButton(
                active  = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic,
                onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic)) }
            ) { Text("I", style = MaterialTheme.typography.labelLarge.copy(fontStyle = FontStyle.Italic)) }
            // Underline
            FormatButton(
                active  = richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
                onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(textDecoration = TextDecoration.Underline)) }
            ) { Text("U", style = MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline)) }
            // Strikethrough
            FormatButton(
                active  = richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
                onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(textDecoration = TextDecoration.LineThrough)) }
            ) { Text("S", style = MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.LineThrough)) }
            VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 4.dp))
            // Bullet list
            FormatButton(
                active  = richTextState.isUnorderedList,
                onClick = { richTextState.toggleUnorderedList() }
            ) { Icon(Icons.Filled.FormatListBulleted, contentDescription = "Bullet list", modifier = Modifier.size(18.dp)) }
            // Numbered list
            FormatButton(
                active  = richTextState.isOrderedList,
                onClick = { richTextState.toggleOrderedList() }
            ) { Icon(Icons.Filled.FormatListNumbered, contentDescription = "Numbered list", modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun FormatButton(active: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(8.dp),
        color    = if (active) AppleYellow.copy(alpha = 0.2f) else Color.Transparent,
        modifier = Modifier.size(36.dp).clickable { onClick() },
        contentColor = if (active) AppleYellow else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

// ── AI input bar ──────────────────────────────────────────────────────────────

@Composable
private fun CollabAiInputBar(
    aiInput: String,
    isLoading: Boolean,
    selectionText: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismissSelect: () -> Unit
) {
    Surface(
        color       = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionText.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppleYellow.copy(alpha = 0.15f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp), tint = AppleYellow)
                        Spacer(Modifier.width(4.dp))
                        Text("Editing selection", style = MaterialTheme.typography.labelSmall, color = AppleYellow)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp).clickable { onDismissSelect() }, tint = AppleYellow)
                    }
                }
            }
            Surface(
                shape    = RoundedCornerShape(20.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    value         = aiInput,
                    onValueChange = onValueChange,
                    textStyle     = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier      = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (aiInput.isEmpty()) Text(
                            if (selectionText.isNotEmpty()) "Instruction for selection…" else "Ask AI to edit this note…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        inner()
                    }
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (aiInput.isNotBlank() && !isLoading) AppleYellow else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .clickable(enabled = aiInput.isNotBlank() && !isLoading) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp), tint = if (aiInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Selection replacement banner ──────────────────────────────────────────────

@Composable
private fun SelectionReplacementBanner(
    replacement: String,
    onApply: () -> Unit,
    onDiscard: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("AI suggestion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                Text(replacement.take(80) + if (replacement.length > 80) "…" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 2)
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDiscard) { Text("Discard") }
            Button(onClick = onApply, colors = ButtonDefaults.buttonColors(containerColor = AppleYellow, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text("Apply") }
        }
    }
}

// ── Share code sheet ──────────────────────────────────────────────────────────

@Composable
private fun ShareCodeSheet(shareCode: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Share this note", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text("Anyone who enters this code can view and edit this note.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            // Big code display
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AppleYellow.copy(alpha = 0.12f)
            ) {
                Text(
                    shareCode,
                    style    = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp),
                    color    = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(shareCode))
                    copied = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = if (copied) MaterialTheme.colorScheme.secondary else AppleYellow, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Icon(if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (copied) "Copied!" else "Copy Code", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── HTML splice helper (reused from NoteDetailScreen) ─────────────────────────

private fun spliceHtmlAtPlainRange(html: String, start: Int, end: Int, replacement: String): String {
    var plain = 0; var htmlIdx = 0
    var startHtml = -1; var endHtml = -1
    while (htmlIdx < html.length) {
        if (startHtml == -1 && plain == start) startHtml = htmlIdx
        if (endHtml   == -1 && plain == end)   endHtml   = htmlIdx
        if (html[htmlIdx] == '<') { while (htmlIdx < html.length && html[htmlIdx] != '>') htmlIdx++; htmlIdx++ }
        else { plain++; htmlIdx++ }
    }
    if (startHtml == -1) startHtml = html.length
    if (endHtml   == -1) endHtml   = html.length
    return html.substring(0, startHtml) + replacement + html.substring(endHtml)
}
