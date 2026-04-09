@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gtr3.byheart.presentation.notes.detail

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gtr3.byheart.core.notification.ReminderScheduler
import com.gtr3.byheart.domain.model.Message
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NoteDetailScreen(
    noteId: Long?,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val richTextState = rememberRichTextState()
    var isInitializing by remember { mutableStateOf(false) }
    var showHeadingSheet by remember { mutableStateOf(false) }
    var capturedRange by remember { mutableStateOf(TextRange.Zero) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // ── Undo / Redo stacks (screen-local, up to 5 snapshots each) ──────────
    val undoStack = remember { ArrayDeque<String>() }
    val redoStack = remember { ArrayDeque<String>() }
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    // ── Notification permission launcher ───────────────────────────────────
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently */ }

    LaunchedEffect(noteId) {
        noteId?.let { viewModel.onIntent(NoteDetailIntent.LoadNote(it)) }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                NoteDetailEffect.NavigateBack        -> onNavigateBack()
                is NoteDetailEffect.NavigateToDetail -> onNavigateToDetail(effect.id)
                is NoteDetailEffect.ShowError        -> Unit
                is NoteDetailEffect.ScheduleReminder -> {
                    // Request POST_NOTIFICATIONS on API 33+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    ReminderScheduler.schedule(
                        context         = context,
                        noteId          = effect.noteId,
                        reminderTitle   = effect.reminderTitle,
                        noteTitle       = effect.noteTitle,
                        triggerAtMillis = effect.triggerAtMillis,
                        reminderDays    = effect.reminderDays
                    )
                }
            }
        }
    }

    // ── Load content into editor when note first arrives ───────────────────
    LaunchedEffect(state.note?.id) {
        state.note?.id ?: return@LaunchedEffect
        isInitializing = true
        undoStack.clear(); redoStack.clear()
        loadIntoEditor(richTextState, state.editedContent)
        delay(100)
        isInitializing = false
        // Seed undo stack with the loaded content so the user can always undo back to it
        pushUndo(undoStack, richTextState.toHtml())
    }

    // ── Reload editor when AI chat responds with new content ───────────────
    LaunchedEffect(state.aiContentVersion) {
        if (state.aiContentVersion > 0) {
            isInitializing = true
            pushUndo(undoStack, richTextState.toHtml())  // save pre-AI state
            redoStack.clear()
            loadIntoEditor(richTextState, state.editedContent)
            delay(100)
            isInitializing = false
            pushUndo(undoStack, richTextState.toHtml())  // save AI result as new checkpoint
        }
    }

    // ── Track user edits for ViewModel + debounced undo snapshot ──────────
    LaunchedEffect(richTextState.annotatedString) {
        if (isInitializing || state.note == null) return@LaunchedEffect
        viewModel.onIntent(NoteDetailIntent.EditedContentChanged(richTextState.toHtml()))
        // Debounce: push undo snapshot 800 ms after the user stops typing
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(800)
            pushUndo(undoStack, richTextState.toHtml())
            redoStack.clear()
        }
    }

    // ── Capture text selection ─────────────────────────────────────────────
    LaunchedEffect(richTextState.selection) {
        if (isInitializing) return@LaunchedEffect
        val sel = richTextState.selection
        if (!sel.collapsed && state.note != null) {
            val text = richTextState.annotatedString.text
            val selectedText = text.substring(sel.start, sel.end)
            if (selectedText.isNotBlank()) {
                capturedRange = sel
                viewModel.onIntent(NoteDetailIntent.TextSelected(selectedText, sel.start, sel.end))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint     = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (state.note != null) {
                        IconButton(onClick = { viewModel.onIntent(NoteDetailIntent.TogglePin) }) {
                            Icon(
                                imageVector        = if (state.note?.isPinned == true) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (state.note?.isPinned == true) "Unpin" else "Pin",
                                tint               = if (state.note?.isPinned == true) MaterialTheme.colorScheme.primary
                                                     else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.onIntent(NoteDetailIntent.ShowFolderDialog) }) {
                            Icon(
                                Icons.Outlined.CreateNewFolder,
                                contentDescription = "Set folder",
                                tint = if (state.note?.folderName != null) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    AnimatedVisibility(visible = state.hasUnsavedChanges, enter = fadeIn(), exit = fadeOut()) {
                        Surface(
                            shape  = MaterialTheme.shapes.small,
                            color  = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            TextButton(onClick = {
                                viewModel.onIntent(NoteDetailIntent.SaveContent(richTextState.toHtml()))
                            }) {
                                Text(
                                    "Save",
                                    color      = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 14.sp
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (state.showFolderDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onIntent(NoteDetailIntent.DismissFolderDialog) },
                title = { Text("Set Folder") },
                text = {
                    OutlinedTextField(
                        value = state.folderDialogInput,
                        onValueChange = { viewModel.onIntent(NoteDetailIntent.FolderDialogInputChanged(it)) },
                        placeholder = { Text("e.g. Cooking, Travel…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.onIntent(NoteDetailIntent.SaveFolder) }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onIntent(NoteDetailIntent.DismissFolderDialog) }) { Text("Cancel") }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && noteId != null && state.note == null -> NoteLoadingView()
                state.isLoading && noteId == null                       -> CreatingNoteView(isAi = state.useAi)
                state.note != null ->
                    NoteViewEditScreen(
                        state = state,
                        richTextState = richTextState,
                        showHeadingSheet = showHeadingSheet,
                        canUndo = undoStack.size > 1,
                        canRedo = redoStack.isNotEmpty(),
                        onToggleHeadingSheet = { showHeadingSheet = !showHeadingSheet },
                        onDismissHeadingSheet = { showHeadingSheet = false },
                        onUndo = {
                            if (undoStack.size > 1) {
                                val current = undoStack.removeLast()
                                redoStack.addFirst(current)
                                coroutineScope.launch {
                                    isInitializing = true
                                    richTextState.setHtml(undoStack.last())
                                    delay(80)
                                    isInitializing = false
                                    viewModel.onIntent(NoteDetailIntent.EditedContentChanged(richTextState.toHtml()))
                                }
                            }
                        },
                        onRedo = {
                            if (redoStack.isNotEmpty()) {
                                val next = redoStack.removeFirst()
                                undoStack.addLast(next)
                                coroutineScope.launch {
                                    isInitializing = true
                                    richTextState.setHtml(next)
                                    delay(80)
                                    isInitializing = false
                                    viewModel.onIntent(NoteDetailIntent.EditedContentChanged(richTextState.toHtml()))
                                }
                            }
                        },
                        onShowFolderDialog = { viewModel.onIntent(NoteDetailIntent.ShowFolderDialog) },
                        onAiInputChange = { viewModel.onIntent(NoteDetailIntent.AiInputChanged(it)) },
                        onSendAiMessage = { viewModel.onIntent(NoteDetailIntent.SendAiMessage) },
                        onApplySelectionReplacement = {
                            val replacement = state.selectionReplacement ?: return@NoteViewEditScreen
                            coroutineScope.launch {
                                val fullHtml = richTextState.toHtml()
                                val annotatedStr = richTextState.annotatedString
                                // richeditor-compose uses a trailing space (not '\n') as the
                                // paragraph separator in annotatedString.text. Each such space
                                // has no HTML plain-text equivalent. Count the number of
                                // paragraph-style ranges that END at or before the given position
                                // (excluding the last range which has no trailing separator) to
                                // get the correct HTML plain-text offset.
                                fun breaksBefore(pos: Int): Int =
                                    annotatedStr.paragraphStyles.count {
                                        it.end <= pos && it.end < annotatedStr.length
                                    }
                                val htmlStart = capturedRange.start - breaksBefore(capturedRange.start)
                                val htmlEnd   = capturedRange.end   - breaksBefore(capturedRange.end)
                                val newHtml = spliceHtmlAtPlainRange(fullHtml, htmlStart, htmlEnd, replacement)
                                pushUndo(undoStack, fullHtml)  // save pre-apply state
                                redoStack.clear()
                                isInitializing = true
                                richTextState.setHtml(newHtml)
                                delay(100)
                                isInitializing = false
                                pushUndo(undoStack, richTextState.toHtml())  // save post-apply state
                                viewModel.onIntent(NoteDetailIntent.EditedContentChanged(richTextState.toHtml()))
                            }
                            viewModel.onIntent(NoteDetailIntent.ApplySelectionReplacement)
                        },
                        onDiscardSelectionReplacement = { viewModel.onIntent(NoteDetailIntent.DiscardSelectionReplacement) },
                        onDismissSelectionEdit = { viewModel.onIntent(NoteDetailIntent.DismissSelectionEdit) }
                    )
                else ->
                    CreateNoteForm(
                        state = state,
                        onTitleChange         = { viewModel.onIntent(NoteDetailIntent.TitleChanged(it)) },
                        onContentChange       = { viewModel.onIntent(NoteDetailIntent.ContentChanged(it)) },
                        onUseAiToggle         = { viewModel.onIntent(NoteDetailIntent.UseAiToggled(it)) },
                        onFolderChange        = { viewModel.onIntent(NoteDetailIntent.FolderInputChanged(it)) },
                        onReminderToggle      = { viewModel.onIntent(NoteDetailIntent.ReminderToggled) },
                        onReminderTitleChange = { viewModel.onIntent(NoteDetailIntent.ReminderTitleChanged(it)) },
                        onReminderTimeChange  = { viewModel.onIntent(NoteDetailIntent.ReminderTimeChanged(it)) },
                        onReminderDayToggle   = { viewModel.onIntent(NoteDetailIntent.ReminderDayToggled(it)) },
                        onSave                = { viewModel.onIntent(NoteDetailIntent.CreateNote) }
                    )
            }
        }
    }
}

// ── Undo helpers ──────────────────────────────────────────────────────────────

private fun pushUndo(stack: ArrayDeque<String>, html: String) {
    if (stack.lastOrNull() == html) return   // no-op on duplicate
    stack.addLast(html)
    while (stack.size > 5) stack.removeFirst()
}

private fun loadIntoEditor(richTextState: RichTextState, content: String) {
    if (content.trimStart().startsWith("<")) richTextState.setHtml(content)
    else richTextState.setMarkdown(content)
}

// ─── Loading States ───────────────────────────────────────────────────────────

@Composable
private fun NoteLoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CreatingNoteView(isAi: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            if (isAi) "Crafting your note with AI…" else "Saving note…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── View / Edit / Chat Screen ────────────────────────────────────────────────

@Composable
private fun NoteViewEditScreen(
    state: NoteDetailState,
    richTextState: RichTextState,
    showHeadingSheet: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onToggleHeadingSheet: () -> Unit,
    onDismissHeadingSheet: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onShowFolderDialog: () -> Unit,
    onAiInputChange: (String) -> Unit,
    onSendAiMessage: () -> Unit,
    onApplySelectionReplacement: () -> Unit,
    onDiscardSelectionReplacement: () -> Unit,
    onDismissSelectionEdit: () -> Unit
) {
    val note = state.note!!
    val scrollState = rememberScrollState()
    val isLoading = state.isAiLoading || state.isSelectionLoading

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Scrollable content ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = note.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = formatDetailDate(note.updatedAt),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (note.folderName != null) {
                    SuggestionChip(
                        onClick = onShowFolderDialog,
                        label = { Text(note.folderName, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(14.dp))

            RichTextEditor(
                state = richTextState,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp),
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    textColor = MaterialTheme.colorScheme.onBackground
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, fontSize = 16.sp),
                placeholder = {
                    Text(
                        "Start writing…",
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp, fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            )

            // ── Chat history ─────────────────────────────────────────────────
            if (state.chatMessages.isNotEmpty() || state.isAiLoading) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Text(
                    "✦ AI Revisions",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                state.chatMessages.forEach { msg ->
                    when (msg.role) {
                        "user"      -> ChatUserBubble(msg)
                        "assistant" -> ChatAiBubble()
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (state.isAiLoading) { ChatLoadingBubble(); Spacer(Modifier.height(6.dp)) }
            }

            state.chatError?.let { err ->
                Spacer(Modifier.height(8.dp))
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(100.dp))
        }

        // ── Selection replacement review ─────────────────────────────────────
        AnimatedVisibility(
            visible = state.selectionReplacement != null,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        ) {
            SelectionReviewBar(
                replacement = state.selectionReplacement ?: "",
                onApply = onApplySelectionReplacement,
                onDiscard = onDiscardSelectionReplacement
            )
        }

        // ── Heading picker sheet ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = showHeadingSheet,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            HeadingSheet(richTextState = richTextState, onDismiss = onDismissHeadingSheet)
        }

        // ── Formatting toolbar (with undo/redo) ───────────────────────────────
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        FormattingToolbar(
            richTextState = richTextState,
            canUndo = canUndo,
            canRedo = canRedo,
            onToggleHeadingSheet = onToggleHeadingSheet,
            onUndo = onUndo,
            onRedo = onRedo
        )

        // ── Unified AI input bar ──────────────────────────────────────────────
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        UnifiedAiInputBar(
            input = state.aiInput,
            selectionText = state.selectionText,
            isLoading = isLoading,
            onInputChange = onAiInputChange,
            onSend = onSendAiMessage,
            onDismissSelection = onDismissSelectionEdit
        )
    }
}

// ─── Selection Review Bar ─────────────────────────────────────────────────────

@Composable
private fun SelectionReviewBar(
    replacement: String,
    onApply: () -> Unit,
    onDiscard: () -> Unit
) {
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Review AI suggestion",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                Text(
                    text = replacement,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    maxLines = 6
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDiscard) {
                    Text("Discard", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Apply", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Unified AI Input Bar ─────────────────────────────────────────────────────

@Composable
private fun UnifiedAiInputBar(
    input: String,
    selectionText: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismissSelection: () -> Unit
) {
    val hasSelection = selectionText.isNotEmpty()
    val placeholder = when {
        isLoading   -> "AI is working…"
        hasSelection -> "Refine: \"${selectionText.take(40)}${if (selectionText.length > 40) "…" else ""}\""
        else         -> "✦ Ask AI to modify…"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (hasSelection) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.background
            )
            .imePadding()
    ) {
        // Selection chip row — shown when text is selected
        if (hasSelection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoFixHigh, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(4.dp))
                Text(
                    "Selection mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismissSelection, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, "Clear selection", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    enabled = !isLoading,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (input.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        inner()
                    }
                )
            }
            Spacer(Modifier.width(8.dp))
            val sendReady = input.isNotBlank() && !isLoading
            IconButton(
                onClick = onSend,
                enabled = sendReady,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (sendReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (sendReady) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ─── Formatting Toolbar (with Undo / Redo) ────────────────────────────────────

@Composable
private fun FormattingToolbar(
    richTextState: RichTextState,
    canUndo: Boolean,
    canRedo: Boolean,
    onToggleHeadingSheet: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    val isBold        = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold
    val isItalic      = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic
    val isUnderline   = richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true
    val isStrikethrough = richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true
    val isBulletList  = richTextState.isUnorderedList
    val isOrderedList = richTextState.isOrderedList

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo
        IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
        // Redo
        IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }

        VerticalDivider(
            modifier = Modifier.height(22.dp).padding(horizontal = 2.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        // Aa — heading picker
        TextButton(onClick = onToggleHeadingSheet, modifier = Modifier.height(40.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text("Aa", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        VerticalDivider(
            modifier = Modifier.height(22.dp).padding(horizontal = 2.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        FormatIconButton(Icons.Filled.FormatBold, isBold) {
            richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        }
        FormatIconButton(Icons.Filled.FormatItalic, isItalic) {
            richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        }
        FormatIconButton(Icons.Filled.FormatUnderlined, isUnderline) {
            richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        }
        FormatIconButton(Icons.Filled.FormatStrikethrough, isStrikethrough) {
            richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
        }

        VerticalDivider(
            modifier = Modifier.height(22.dp).padding(horizontal = 2.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        FormatIconButton(Icons.Filled.FormatListBulleted, isBulletList) { richTextState.toggleUnorderedList() }
        FormatIconButton(Icons.Filled.FormatListNumbered, isOrderedList) { richTextState.toggleOrderedList() }
    }
}

@Composable
private fun FormatIconButton(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─── Heading Sheet (Apple Notes style) ───────────────────────────────────────

@Composable
private fun HeadingSheet(richTextState: RichTextState, onDismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .width(36.dp).height(4.dp)
                    .background(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape = RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            HeadingOption("Title", "Large title", 26.sp, FontWeight.Bold) {
                richTextState.toggleSpanStyle(SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold)); onDismiss()
            }
            HeadingOption("Heading", "Section heading", 22.sp, FontWeight.Bold) {
                richTextState.toggleSpanStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)); onDismiss()
            }
            HeadingOption("Subheading", "Smaller heading", 18.sp, FontWeight.SemiBold) {
                richTextState.toggleSpanStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)); onDismiss()
            }
            HeadingOption("Body", "Regular text", 16.sp, FontWeight.Normal) {
                richTextState.toggleSpanStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)); onDismiss()
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeadingOption(label: String, description: String, fontSize: TextUnit, fontWeight: FontWeight, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = fontSize, fontWeight = fontWeight, color = MaterialTheme.colorScheme.onSurface)
        Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Chat Bubbles ─────────────────────────────────────────────────────────────

@Composable
private fun ChatUserBubble(msg: Message) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(text = msg.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun ChatAiBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✦", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Suggestion ready", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.height(2.dp))
                Text("Review and edit above, then tap Save to keep it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun ChatLoadingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("AI is thinking…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

// ─── Create Note Form (with Reminder) ────────────────────────────────────────

@Composable
private fun CreateNoteForm(
    state: NoteDetailState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onUseAiToggle: (Boolean) -> Unit,
    onFolderChange: (String) -> Unit,
    onReminderToggle: () -> Unit,
    onReminderTitleChange: (String) -> Unit,
    onReminderTimeChange: (Long) -> Unit,
    onReminderDayToggle: (Int) -> Unit,
    onSave: () -> Unit
) {
    val isRepeating = state.reminderDays.isNotEmpty()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    // Holds the picked date while waiting for time (unused in repeating mode)
    var pendingDateMillis by remember { mutableStateOf(0L) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    // For repeating reminders we only need hour+minute; use today as the date base
                    val base = if (isRepeating) System.currentTimeMillis() else pendingDateMillis
                    val cal = java.util.Calendar.getInstance().apply {
                        timeInMillis = base
                        set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(java.util.Calendar.MINUTE, timePickerState.minute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    onReminderTimeChange(cal.timeInMillis)
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            TextField(
                value = state.titleInput,
                onValueChange = onTitleChange,
                placeholder = {
                    Text(
                        if (state.useAi) "What should I write about?" else "Title",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                minLines = 1, maxLines = 3
            )
            Spacer(Modifier.height(4.dp))

            // AI toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Generate with AI", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onBackground)
                    Text(if (state.useAi) "AI will write the content" else "Write it yourself", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.useAi,
                    onCheckedChange = onUseAiToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            if (!state.useAi) {
                TextField(
                    value = state.contentInput,
                    onValueChange = onContentChange,
                    placeholder = {
                        Text("Start writing…", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), lineHeight = 26.sp))
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground, lineHeight = 26.sp, fontSize = 16.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedIndicatorColor = MaterialTheme.colorScheme.background,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.background,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp),
                    minLines = 8
                )
            } else {
                Spacer(Modifier.height(24.dp))
                Text("Describe what you'd like and AI will generate the full note content for you.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
            }

            Spacer(Modifier.height(12.dp))

            // Folder
            TextField(
                value = state.folderInput,
                onValueChange = onFolderChange,
                placeholder = { Text("Folder (optional)", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.background,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.background,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(Modifier.height(4.dp))

            // ── Reminder section ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (state.reminderEnabled) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = if (state.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Set Reminder", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onBackground)
                        Text("Get notified at a specific time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = state.reminderEnabled,
                    onCheckedChange = { onReminderToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            AnimatedVisibility(visible = state.reminderEnabled) {
                Column {
                    Spacer(Modifier.height(12.dp))

                    // ── Day-of-week picker ──────────────────────────────────
                    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                    Text(
                        "Repeat on",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayLabels.forEachIndexed { index, label ->
                            val selected = index in state.reminderDays
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { onReminderDayToggle(index) }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // ── Notification title ──────────────────────────────────
                    TextField(
                        value = state.reminderTitle,
                        onValueChange = onReminderTitleChange,
                        placeholder = { Text("Notification title (optional)", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.background,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.background,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))

                    // ── Time picker button ──────────────────────────────────
                    // Repeating: only need time. One-time: need date + time.
                    OutlinedButton(
                        onClick = { if (isRepeating) showTimePicker = true else showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Outlined.NotificationsNone, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state.reminderTimeMillis > 0L) {
                                val ldt = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(state.reminderTimeMillis),
                                    ZoneId.systemDefault()
                                )
                                if (isRepeating)
                                    ldt.format(DateTimeFormatter.ofPattern("h:mm a"))
                                else
                                    ldt.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
                            } else {
                                if (isRepeating) "Pick time" else "Pick date & time"
                            }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            state.error?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 8.dp) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (state.useAi) "Generate with AI" else "Save Note", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

// ─── HTML Range Splice ────────────────────────────────────────────────────────

private fun spliceHtmlAtPlainRange(html: String, plainStart: Int, plainEnd: Int, replacement: String): String {
    if (plainStart >= plainEnd || html.isEmpty()) return html
    var plain = 0
    var i = 0
    var htmlStart = -1
    var htmlEnd = html.length

    while (i < html.length) {
        if (plain == plainEnd) { htmlEnd = i; break }
        when {
            html[i] == '<' -> { while (i < html.length && html[i] != '>') i++; i++ }
            html[i] == '&' -> {
                if (plain == plainStart) htmlStart = i
                while (i < html.length && html[i] != ';') i++
                i++; plain++
            }
            else -> {
                if (plain == plainStart) htmlStart = i
                i++; plain++
            }
        }
    }

    return if (htmlStart == -1) html
    else html.substring(0, htmlStart) + replacement + html.substring(htmlEnd)
}

// ─── Date Formatting ─────────────────────────────────────────────────────────

private fun formatDetailDate(updatedAt: String): String {
    return try {
        val cleaned = updatedAt.take(19).replace(" ", "T")
        val dt = LocalDateTime.parse(cleaned, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        val now = LocalDateTime.now()
        val timeStr = dt.format(DateTimeFormatter.ofPattern("h:mm a"))
        when {
            dt.toLocalDate() == now.toLocalDate()              -> "Today at $timeStr"
            dt.toLocalDate() == now.minusDays(1).toLocalDate() -> "Yesterday at $timeStr"
            dt.year == now.year -> "${dt.format(DateTimeFormatter.ofPattern("MMMM d"))} at $timeStr"
            else                -> "${dt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))} at $timeStr"
        }
    } catch (e: Exception) {
        updatedAt.take(10)
    }
}
