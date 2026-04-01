package com.gtr3.byheart.presentation.notes.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long?,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(noteId) {
        noteId?.let { viewModel.onIntent(NoteDetailIntent.LoadNote(it)) }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                NoteDetailEffect.NavigateBack        -> onNavigateBack()
                is NoteDetailEffect.NavigateToDetail -> onNavigateToDetail(effect.id)
                is NoteDetailEffect.ShowError        -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // Loading an existing note
                state.isLoading && noteId != null && state.note == null ->
                    NoteLoadingView()

                // Saving / generating a new note
                state.isLoading && noteId == null ->
                    CreatingNoteView(isAi = state.useAi)

                // Viewing an existing note
                state.note != null ->
                    NoteContentView(note = state.note!!)

                // Creating a new note
                else ->
                    CreateNoteForm(
                        title = state.titleInput,
                        content = state.contentInput,
                        useAi = state.useAi,
                        error = state.error,
                        onTitleChange = { viewModel.onIntent(NoteDetailIntent.TitleChanged(it)) },
                        onContentChange = { viewModel.onIntent(NoteDetailIntent.ContentChanged(it)) },
                        onUseAiToggle = { viewModel.onIntent(NoteDetailIntent.UseAiToggled(it)) },
                        onSave = { viewModel.onIntent(NoteDetailIntent.CreateNote) }
                    )
            }
        }
    }
}

// ─── Loading States ──────────────────────────────────────────────────────────

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
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            if (isAi) "Crafting your note with AI…" else "Saving note…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Note Content View (View Mode) ───────────────────────────────────────────

@Composable
private fun NoteContentView(note: com.gtr3.byheart.domain.model.Note) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = note.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatDetailDate(note.updatedAt),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        if (!note.content.isNullOrBlank()) {
            MarkdownContent(
                content = note.content,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                "No content yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(0.6f)
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ─── Markdown Content Renderer ───────────────────────────────────────────────

@Composable
private fun MarkdownContent(content: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        content.split("\n").forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = parseBoldText(line.removePrefix("# ")),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                }
                line.startsWith("## ") -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = parseBoldText(line.removePrefix("## ")),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(2.dp))
                }
                line.startsWith("### ") -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = parseBoldText(line.removePrefix("### ")),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = parseBoldText(line.drop(2)),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val dotIndex = line.indexOf('.')
                    val num = line.take(dotIndex + 1)
                    val rest = line.drop(dotIndex + 2)
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            num,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = parseBoldText(rest),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                line.isBlank() -> Spacer(Modifier.height(8.dp))
                else -> {
                    Text(
                        text = parseBoldText(line),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 26.sp,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

private fun parseBoldText(text: String): AnnotatedString = buildAnnotatedString {
    var remaining = text
    while (remaining.isNotEmpty()) {
        val start = remaining.indexOf("**")
        if (start == -1) { append(remaining); break }
        append(remaining.take(start))
        remaining = remaining.drop(start + 2)
        val end = remaining.indexOf("**")
        if (end == -1) { append("**"); append(remaining); break }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(remaining.take(end)) }
        remaining = remaining.drop(end + 2)
    }
}

// ─── Create Note Form ────────────────────────────────────────────────────────

@Composable
private fun CreateNoteForm(
    title: String,
    content: String,
    useAi: Boolean,
    error: String?,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onUseAiToggle: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            TextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = {
                    Text(
                        if (useAi) "What should I write about?" else "Title",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 3
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Generate with AI",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        if (useAi) "AI will write the content" else "Write it yourself",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useAi,
                    onCheckedChange = onUseAiToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
            if (!useAi) {
                TextField(
                    value = content,
                    onValueChange = onContentChange,
                    placeholder = {
                        Text(
                            "Start writing…",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                lineHeight = 26.sp
                            )
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 26.sp,
                        fontSize = 16.sp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedIndicatorColor = MaterialTheme.colorScheme.background,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.background,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 200.dp),
                    minLines = 8
                )
            } else {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Describe what you'd like and AI will generate the full note content for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
            error?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        // Bottom action bar
        Surface(
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    if (useAi) "Generate with AI" else "Save Note",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ─── Date Formatting ─────────────────────────────────────────────────────────

private fun formatDetailDate(updatedAt: String): String {
    return try {
        val cleaned = updatedAt.take(19).replace(" ", "T")
        val dt = LocalDateTime.parse(cleaned, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        val now = LocalDateTime.now()
        val timeStr = dt.format(DateTimeFormatter.ofPattern("h:mm a"))
        when {
            dt.toLocalDate() == now.toLocalDate() -> "Today at $timeStr"
            dt.toLocalDate() == now.minusDays(1).toLocalDate() -> "Yesterday at $timeStr"
            dt.year == now.year -> "${dt.format(DateTimeFormatter.ofPattern("MMMM d"))} at $timeStr"
            else -> "${dt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))} at $timeStr"
        }
    } catch (e: Exception) {
        updatedAt.take(10)
    }
}
