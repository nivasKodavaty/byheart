@file:OptIn(ExperimentalMaterial3Api::class)

package com.gtr3.byheart.presentation.collab.detail

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
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
    var showHeadingSheet by remember { mutableStateOf(false) }
    var capturedRange by remember { mutableStateOf(TextRange.Zero) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val undoStack = remember { ArrayDeque<String>() }
    val redoStack = remember { ArrayDeque<String>() }
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    fun pushUndo(stack: ArrayDeque<String>, html: String) {
        if (stack.lastOrNull() == html) return
        stack.addLast(html)
        while (stack.size > 5) stack.removeFirst()
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // ── Load ──────────────────────────────────────────────────────────────────
    LaunchedEffect(shareCode) {
        viewModel.onIntent(CollabNoteDetailIntent.Load(shareCode))
    }

    // ── Poll every 5 s ────────────────────────────────────────────────────────
    LaunchedEffect(shareCode) {
        while (true) {
            delay(5_000L)
            viewModel.onIntent(CollabNoteDetailIntent.PollForUpdates)
        }
    }

    // ── Initial content load ──────────────────────────────────────────────────
    LaunchedEffect(state.note?.shareCode) {
        state.note ?: return@LaunchedEffect
        undoStack.clear(); redoStack.clear()
        isInitializing = true
        loadIntoEditor(richTextState, state.editedContent)
        delay(100)
        isInitializing = false
        pushUndo(undoStack, richTextState.toHtml())
    }

    // ── AI / poll version reload ───────────────────────────────────────────────
    LaunchedEffect(state.aiContentVersion) {
        if (state.aiContentVersion == 0) return@LaunchedEffect
        pushUndo(undoStack, richTextState.toHtml())
        redoStack.clear()
        isInitializing = true
        loadIntoEditor(richTextState, state.editedContent)
        delay(100)
        isInitializing = false
        pushUndo(undoStack, richTextState.toHtml())
    }

    // ── Capture text selection ────────────────────────────────────────────────
    LaunchedEffect(richTextState.selection) {
        if (isInitializing) return@LaunchedEffect
        val sel = richTextState.selection
        if (!sel.collapsed && state.note != null) {
            val text = richTextState.annotatedString.text
            val selectedText = text.substring(
                sel.start.coerceAtMost(text.length),
                sel.end.coerceAtMost(text.length)
            )
            if (selectedText.isNotBlank()) {
                capturedRange = sel
                viewModel.onIntent(CollabNoteDetailIntent.TextSelected(selectedText, sel.start, sel.end))
            }
        }
    }

    // ── Effects ───────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                CollabNoteDetailEffect.NavigateBack -> onNavigateBack()
                CollabNoteDetailEffect.NoteLeft     -> onNavigateBack()
                CollabNoteDetailEffect.ReloadEditor -> { /* handled via aiContentVersion */ }
                is CollabNoteDetailEffect.ConflictDetected -> {
                    pushUndo(undoStack, richTextState.toHtml())
                    isInitializing = true
                    loadIntoEditor(richTextState, effect.latestContent ?: "")
                    delay(100)
                    isInitializing = false
                    pushUndo(undoStack, richTextState.toHtml())
                    snackbarHostState.showSnackbar(
                        message = "A collaborator saved — latest version loaded. Please review and save again.",
                        duration = SnackbarDuration.Long,
                        withDismissAction = true
                    )
                    viewModel.onIntent(CollabNoteDetailIntent.AcceptConflict)
                }
                is CollabNoteDetailEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is CollabNoteDetailEffect.ScheduleReminder -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    ReminderScheduler.schedule(
                        context         = context,
                        noteId          = effect.noteId,
                        noteTitle       = effect.noteTitle,
                        reminderTitle   = effect.reminderTitle,
                        triggerAtMillis = effect.triggerAtMillis,
                        reminderDays    = effect.reminderDays
                    )
                }
            }
        }
    }

    // ── Track user edits ──────────────────────────────────────────────────────
    LaunchedEffect(richTextState.annotatedString) {
        if (isInitializing || state.note == null) return@LaunchedEffect
        viewModel.onIntent(CollabNoteDetailIntent.EditedContentChanged(richTextState.toHtml()))
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(800L)
            pushUndo(undoStack, richTextState.toHtml())
            redoStack.clear()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CollabDetailTopBar(
                shareCode  = state.note?.shareCode ?: shareCode,
                isSaving   = state.isSaving,
                hasChanges = state.hasUnsavedChanges,
                onBack     = { viewModel.onIntent(CollabNoteDetailIntent.NavigateBack) },
                onSave      = { viewModel.onIntent(CollabNoteDetailIntent.SaveContent(richTextState.toHtml())) },
                onShareCode = { viewModel.onIntent(CollabNoteDetailIntent.ShowShareCode) },
                onReminder  = { viewModel.onIntent(CollabNoteDetailIntent.OpenReminderSheet) },
                onLeave     = { viewModel.onIntent(CollabNoteDetailIntent.LeaveNote) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.note == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.note != null -> {
                    val scrollState = rememberScrollState()

                    Column(modifier = Modifier.fillMaxSize()) {
                        // ── Scrollable content ────────────────────────────────
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState)
                                .padding(horizontal = 20.dp)
                        ) {
                            Spacer(Modifier.height(4.dp))

                            // Editable title
                            BasicTextField(
                                value         = state.editedTitle,
                                onValueChange = { viewModel.onIntent(CollabNoteDetailIntent.TitleChanged(it)) },
                                textStyle     = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 24.sp,
                                    color      = MaterialTheme.colorScheme.onBackground
                                ),
                                cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier      = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (state.editedTitle.isEmpty()) {
                                        Text(
                                            "Untitled",
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                    inner()
                                }
                            )

                            Spacer(Modifier.height(4.dp))

                            // Collab meta row
                            state.note?.let { note ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    note.lastEditedBy?.let {
                                        Text("Edited by $it", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.clickable { viewModel.onIntent(CollabNoteDetailIntent.ShowParticipants) }
                                    ) {
                                        Icon(Icons.Filled.Group, contentDescription = "View participants", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text(
                                            "${note.participantCount} collaborator${if (note.participantCount != 1) "s" else ""}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            Spacer(Modifier.height(14.dp))

                            RichTextEditor(
                                state    = richTextState,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp),
                                colors   = RichTextEditorDefaults.richTextEditorColors(
                                    containerColor          = MaterialTheme.colorScheme.background,
                                    focusedIndicatorColor   = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor             = MaterialTheme.colorScheme.primary,
                                    textColor               = MaterialTheme.colorScheme.onBackground
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

                            // ── Chat history ──────────────────────────────────
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
                                        "user"      -> CollabChatUserBubble(msg)
                                        "assistant" -> CollabChatAiBubble()
                                    }
                                    Spacer(Modifier.height(6.dp))
                                }
                                if (state.isAiLoading) {
                                    CollabChatLoadingBubble()
                                    Spacer(Modifier.height(6.dp))
                                }
                            }

                            state.chatError?.let { err ->
                                Spacer(Modifier.height(8.dp))
                                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(Modifier.height(100.dp))
                        }

                        // ── Selection replacement review (same position as NoteDetail) ──
                        AnimatedVisibility(
                            visible = state.selectionReplacement != null,
                            enter   = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                            exit    = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
                        ) {
                            CollabSelectionReviewBar(
                                replacement = state.selectionReplacement ?: "",
                                onApply     = {
                                    val replacement = state.selectionReplacement ?: return@CollabSelectionReviewBar
                                    coroutineScope.launch {
                                        val fullHtml = richTextState.toHtml()
                                        val annotatedStr = richTextState.annotatedString
                                        fun breaksBefore(pos: Int): Int =
                                            annotatedStr.paragraphStyles.count {
                                                it.end <= pos && it.end < annotatedStr.length
                                            }
                                        val htmlStart = capturedRange.start - breaksBefore(capturedRange.start)
                                        val htmlEnd   = capturedRange.end   - breaksBefore(capturedRange.end)
                                        val newHtml  = spliceHtmlAtPlainRange(fullHtml, htmlStart, htmlEnd, replacement)
                                        pushUndo(undoStack, fullHtml)
                                        redoStack.clear()
                                        isInitializing = true
                                        richTextState.setHtml(newHtml)
                                        delay(100)
                                        isInitializing = false
                                        pushUndo(undoStack, richTextState.toHtml())
                                        viewModel.onIntent(CollabNoteDetailIntent.EditedContentChanged(richTextState.toHtml()))
                                    }
                                    viewModel.onIntent(CollabNoteDetailIntent.ApplySelectionReplacement)
                                },
                                onDiscard   = { viewModel.onIntent(CollabNoteDetailIntent.DiscardSelectionReplacement) }
                            )
                        }

                        // ── Heading picker sheet ──────────────────────────────
                        AnimatedVisibility(
                            visible = showHeadingSheet,
                            enter   = slideInVertically(initialOffsetY = { it }),
                            exit    = slideOutVertically(targetOffsetY = { it })
                        ) {
                            CollabHeadingSheet(richTextState = richTextState, onDismiss = { showHeadingSheet = false })
                        }

                        // ── Formatting toolbar ────────────────────────────────
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        CollabFormattingToolbar(
                            richTextState        = richTextState,
                            canUndo              = undoStack.size > 1,
                            canRedo              = redoStack.isNotEmpty(),
                            onToggleHeadingSheet = { showHeadingSheet = !showHeadingSheet },
                            onUndo = {
                                if (undoStack.size > 1) {
                                    val current = undoStack.removeLast()
                                    redoStack.addFirst(current)
                                    coroutineScope.launch {
                                        isInitializing = true
                                        richTextState.setHtml(undoStack.last())
                                        delay(80)
                                        isInitializing = false
                                        viewModel.onIntent(CollabNoteDetailIntent.EditedContentChanged(richTextState.toHtml()))
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
                                        viewModel.onIntent(CollabNoteDetailIntent.EditedContentChanged(richTextState.toHtml()))
                                    }
                                }
                            }
                        )

                        // ── Unified AI input ──────────────────────────────────
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        CollabUnifiedAiInputBar(
                            input            = state.aiInput,
                            selectionText    = state.selectionText,
                            isLoading        = state.isAiLoading || state.isSelectionLoading,
                            onInputChange    = { viewModel.onIntent(CollabNoteDetailIntent.AiInputChanged(it)) },
                            onSend           = { viewModel.onIntent(CollabNoteDetailIntent.SendAiMessage) },
                            onDismissSelection = { viewModel.onIntent(CollabNoteDetailIntent.DismissSelectionEdit) }
                        )
                    }
                }
            }
        }
    }

    // ── Share code sheet ───────────────────────────────────────────────────────
    if (state.showShareCodeSheet) {
        CollabShareCodeSheet(
            shareCode = state.note?.shareCode ?: shareCode,
            onDismiss = { viewModel.onIntent(CollabNoteDetailIntent.DismissShareCode) }
        )
    }

    // ── Participants sheet ─────────────────────────────────────────────────────
    if (state.showParticipantsSheet) {
        CollabParticipantsSheet(
            participants = state.participants,
            isLoading    = state.isParticipantsLoading,
            onDismiss    = { viewModel.onIntent(CollabNoteDetailIntent.DismissParticipants) }
        )
    }

    // ── Reminder sheet ─────────────────────────────────────────────────────────
    if (state.showReminderSheet) {
        CollabReminderSheet(
            state     = state,
            onToggle  = { viewModel.onIntent(CollabNoteDetailIntent.ReminderToggled) },
            onTitleChange = { viewModel.onIntent(CollabNoteDetailIntent.ReminderTitleChanged(it)) },
            onTimeChange  = { viewModel.onIntent(CollabNoteDetailIntent.ReminderTimeChanged(it)) },
            onDayToggle   = { viewModel.onIntent(CollabNoteDetailIntent.ReminderDayToggled(it)) },
            onSchedule    = { viewModel.onIntent(CollabNoteDetailIntent.ScheduleReminderNow) },
            onDismiss     = { viewModel.onIntent(CollabNoteDetailIntent.DismissReminderSheet) }
        )
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun CollabDetailTopBar(
    shareCode: String,
    isSaving: Boolean,
    hasChanges: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onShareCode: () -> Unit,
    onReminder: () -> Unit,
    onLeave: () -> Unit
) {
    var showLeaveConfirm by remember { mutableStateOf(false) }

    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBack) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                }
            }
        },
        actions = {
            // Share code chip
            Surface(
                shape    = RoundedCornerShape(20.dp),
                color    = MaterialTheme.colorScheme.primaryContainer,
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
            // Save button — same style as NoteDetailScreen
            AnimatedVisibility(visible = hasChanges, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    shape    = MaterialTheme.shapes.small,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    TextButton(
                        onClick  = onSave,
                        enabled  = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
            // Reminder bell
            IconButton(onClick = onReminder) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = "Set reminder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Leave
            IconButton(onClick = { showLeaveConfirm = true }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Leave note", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor         = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        )
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

// ── Selection review bar — exact copy of NoteDetailScreen's SelectionReviewBar ─

@Composable
private fun CollabSelectionReviewBar(replacement: String, onApply: () -> Unit, onDiscard: () -> Unit) {
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(6.dp))
                Text("Review AI suggestion", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                Text(text = replacement, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), maxLines = 6)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDiscard) {
                    Text("Discard", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onApply, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text("Apply", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Formatting toolbar — exact copy of NoteDetailScreen's FormattingToolbar ───

@Composable
private fun CollabFormattingToolbar(
    richTextState: RichTextState,
    canUndo: Boolean,
    canRedo: Boolean,
    onToggleHeadingSheet: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    val isBold          = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold
    val isItalic        = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic
    val isUnderline     = richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true
    val isStrikethrough = richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true
    val isBulletList    = richTextState.isUnorderedList
    val isOrderedList   = richTextState.isOrderedList

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(40.dp)) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(40.dp)) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
        }
        VerticalDivider(modifier = Modifier.height(22.dp).padding(horizontal = 2.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        TextButton(onClick = onToggleHeadingSheet, modifier = Modifier.height(40.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text("Aa", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        VerticalDivider(modifier = Modifier.height(22.dp).padding(horizontal = 2.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        CollabFormatIconButton(Icons.Filled.FormatBold, isBold)          { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
        CollabFormatIconButton(Icons.Filled.FormatItalic, isItalic)      { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
        CollabFormatIconButton(Icons.Filled.FormatUnderlined, isUnderline) { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
        CollabFormatIconButton(Icons.Filled.FormatStrikethrough, isStrikethrough) { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }
        VerticalDivider(modifier = Modifier.height(22.dp).padding(horizontal = 2.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        CollabFormatIconButton(Icons.AutoMirrored.Filled.FormatListBulleted, isBulletList)  { richTextState.toggleUnorderedList() }
        CollabFormatIconButton(Icons.Filled.FormatListNumbered, isOrderedList) { richTextState.toggleOrderedList() }
    }
}

@Composable
private fun CollabFormatIconButton(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick  = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(8.dp))
    ) {
        Icon(icon, contentDescription = null, tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

// ── Heading sheet — exact copy of NoteDetailScreen's HeadingSheet ─────────────

@Composable
private fun CollabHeadingSheet(richTextState: RichTextState, onDismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Box(modifier = Modifier.width(36.dp).height(4.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            CollabHeadingOption("Title",      "Large title",     26.sp, FontWeight.Bold)      { richTextState.toggleSpanStyle(SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold)); onDismiss() }
            CollabHeadingOption("Heading",    "Section heading", 22.sp, FontWeight.Bold)      { richTextState.toggleSpanStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)); onDismiss() }
            CollabHeadingOption("Subheading", "Smaller heading", 18.sp, FontWeight.SemiBold)  { richTextState.toggleSpanStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)); onDismiss() }
            CollabHeadingOption("Body",       "Regular text",    16.sp, FontWeight.Normal)    { richTextState.toggleSpanStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)); onDismiss() }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CollabHeadingOption(label: String, description: String, fontSize: TextUnit, fontWeight: FontWeight, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = fontSize, fontWeight = fontWeight, color = MaterialTheme.colorScheme.onSurface)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Unified AI input bar — exact copy of NoteDetailScreen's UnifiedAiInputBar ─

@Composable
private fun CollabUnifiedAiInputBar(
    input: String,
    selectionText: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismissSelection: () -> Unit
) {
    val hasSelection = selectionText.isNotEmpty()
    val placeholder  = when {
        isLoading    -> "AI is working…"
        hasSelection -> "Refine: \"${selectionText.take(40)}${if (selectionText.length > 40) "…" else ""}\""
        else         -> "✦ Ask AI to modify…"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (hasSelection) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        if (hasSelection) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoFixHigh, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(4.dp))
                Text("Selection mode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismissSelection, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, "Clear selection", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value         = input,
                    onValueChange = onInputChange,
                    enabled       = !isLoading,
                    textStyle     = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine    = false,
                    maxLines      = 4,
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (input.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        inner()
                    }
                )
            }
            Spacer(Modifier.width(8.dp))
            val sendReady = input.isNotBlank() && !isLoading
            IconButton(
                onClick  = onSend,
                enabled  = sendReady,
                modifier = Modifier.size(44.dp).background(if (sendReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (sendReady) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Chat bubbles — exact copy of NoteDetailScreen ─────────────────────────────

@Composable
private fun CollabChatUserBubble(msg: Message) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.widthIn(max = 280.dp)) {
            Text(text = msg.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun CollabChatAiBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.widthIn(max = 280.dp)) {
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
private fun CollabChatLoadingBubble() {
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

// ── Share code sheet ───────────────────────────────────────────────────────────

@Suppress("DEPRECATION")
@Composable
private fun CollabShareCodeSheet(shareCode: String, onDismiss: () -> Unit) {
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
            Surface(shape = RoundedCornerShape(20.dp), color = AppleYellow.copy(alpha = 0.12f)) {
                Text(shareCode, style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp), color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp))
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick  = { clipboard.setText(AnnotatedString(shareCode)); copied = true },
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

// ── Participants sheet ────────────────────────────────────────────────────────

@Composable
private fun CollabParticipantsSheet(
    participants: List<com.gtr3.byheart.domain.model.CollabParticipant>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                "Collaborators",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(16.dp))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (participants.isEmpty()) {
                Text(
                    "No participants found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                participants.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar circle with initials
                        val initials = (p.displayName?.take(2) ?: p.email.take(2)).uppercase()
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                initials,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                p.displayName ?: p.email.substringBefore("@"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                p.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }
        }
    }
}

// ── Reminder sheet ────────────────────────────────────────────────────────────

@Composable
private fun CollabReminderSheet(
    state: CollabNoteDetailState,
    onToggle: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTimeChange: (Long) -> Unit,
    onDayToggle: (Int) -> Unit,
    onSchedule: () -> Unit,
    onDismiss: () -> Unit
) {
    val isRepeating = state.reminderDays.isNotEmpty()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
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
                    val base = if (isRepeating) System.currentTimeMillis() else pendingDateMillis
                    val cal = java.util.Calendar.getInstance().apply {
                        timeInMillis = base
                        set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(java.util.Calendar.MINUTE, timePickerState.minute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    onTimeChange(cal.timeInMillis)
                    showTimePicker = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                "Set Reminder",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(16.dp))

            // Toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        Text("Enable reminder", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                        Text("Get notified at a specific time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = state.reminderEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor  = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor  = MaterialTheme.colorScheme.primary
                    )
                )
            }

            AnimatedVisibility(visible = state.reminderEnabled) {
                Column {
                    Spacer(Modifier.height(16.dp))

                    // Day-of-week picker
                    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                    Text("Repeat on", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayLabels.forEachIndexed { index, label ->
                            val selected = index in state.reminderDays
                            Surface(
                                shape    = RoundedCornerShape(50),
                                color    = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(38.dp).clickable { onDayToggle(index) }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // Notification title
                    TextField(
                        value         = state.reminderTitle,
                        onValueChange = onTitleChange,
                        placeholder   = { Text("Notification title (optional)", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) },
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        colors        = TextFieldDefaults.colors(
                            focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor   = MaterialTheme.colorScheme.background,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.background,
                            cursorColor             = MaterialTheme.colorScheme.primary
                        ),
                        shape    = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))

                    // Time picker button
                    OutlinedButton(
                        onClick  = { if (isRepeating) showTimePicker = true else showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Outlined.NotificationsNone, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state.reminderTimeMillis > 0L) {
                                val ldt = java.time.LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli(state.reminderTimeMillis),
                                    java.time.ZoneId.systemDefault()
                                )
                                if (isRepeating) ldt.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
                                else ldt.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
                            } else {
                                if (isRepeating) "Pick time" else "Pick date & time"
                            }
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    // Schedule button
                    Button(
                        onClick  = onSchedule,
                        enabled  = state.reminderTimeMillis > 0L,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Filled.NotificationsActive, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Schedule Reminder", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun loadIntoEditor(richTextState: RichTextState, content: String) {
    if (content.trimStart().startsWith("<")) richTextState.setHtml(content)
    else richTextState.setMarkdown(content)
}

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
