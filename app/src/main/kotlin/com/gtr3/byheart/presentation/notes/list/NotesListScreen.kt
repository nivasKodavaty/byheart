@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.gtr3.byheart.presentation.notes.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gtr3.byheart.domain.model.Note
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun NotesListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: NotesListViewModel = hiltViewModel()
) {
    val state      by viewModel.state.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    val listState   = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // FAB expands when list is at the top or not scrolling
    val expandedFab by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NotesListEffect.NavigateToDetail -> onNavigateToDetail(effect.id)
                NotesListEffect.NavigateToCreate    -> onNavigateToCreate()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            NotesDrawer(
                state             = state,
                onSelectFolder    = { folder ->
                    viewModel.onIntent(NotesListIntent.SelectFolder(folder))
                    scope.launch { drawerState.close() }
                },
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    TopAppBar(
                        title = {
                            Text(
                                text       = state.selectedFolder ?: "Notes",
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Notes,
                                        contentDescription = "Folders",
                                        tint     = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            Text(
                                "${state.filtered.size} note${if (state.filtered.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor         = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.background
                        )
                    )
                    NoteSearchBar(
                        query         = state.searchQuery,
                        onQueryChange = { viewModel.onIntent(NotesListIntent.SearchChanged(it)) },
                        modifier      = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp)
                    )
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick        = { viewModel.onIntent(NotesListIntent.CreateNote) },
                    expanded       = expandedFab,
                    icon           = { Icon(Icons.Outlined.Edit, contentDescription = "New Note") },
                    text           = { Text("New Note", fontWeight = FontWeight.SemiBold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary,
                    shape          = RoundedCornerShape(16.dp)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh    = { viewModel.onIntent(NotesListIntent.Refresh) },
                modifier     = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    state.isLoading && state.notes.isEmpty() -> LoadingView()
                    state.filtered.isEmpty()                 -> EmptyView(state.searchQuery)
                    else -> NotesSectionsList(
                        state          = state,
                        listState      = listState,
                        onOpen         = { viewModel.onIntent(NotesListIntent.OpenNote(it)) },
                        onPin          = { viewModel.onIntent(NotesListIntent.PinNote(it)) },
                        onDelete       = { viewModel.onIntent(NotesListIntent.DeleteNote(it)) },
                        onToggleFolder = { viewModel.onIntent(NotesListIntent.ToggleFolderCollapse(it)) }
                    )
                }
            }
        }
    }
}

// ─── Loading / Empty States ───────────────────────────────────────────────────

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color       = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.5.dp
        )
    }
}

@Composable
private fun EmptyView(query: String) {
    Column(
        modifier              = Modifier.fillMaxSize(),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Text(
            text  = if (query.isNotBlank()) "🔍" else "✦",
            fontSize = 56.sp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text  = if (query.isNotBlank()) "No results for \"$query\"" else "No Notes Yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text  = if (query.isNotBlank()) "Try a different search term"
                    else "Tap New Note to start writing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Drawer ───────────────────────────────────────────────────────────────────

@Composable
private fun NotesDrawer(
    state: NotesListState,
    onSelectFolder: (String?) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier             = Modifier.width(288.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "byheart",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(16.dp))

        DrawerItem(
            icon     = {
                Icon(
                    Icons.AutoMirrored.Filled.Notes,
                    null,
                    tint     = if (state.selectedFolder == null) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            label    = "All Notes",
            count    = state.allNotesCount,
            selected = state.selectedFolder == null,
            onClick  = { onSelectFolder(null) }
        )

        if (state.folders.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "FOLDERS",
                style    = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            state.folders.forEach { folder ->
                DrawerItem(
                    icon  = {
                        Icon(
                            imageVector = if (state.selectedFolder == folder) Icons.Filled.Folder
                                          else Icons.Outlined.Folder,
                            contentDescription = null,
                            tint     = if (state.selectedFolder == folder) MaterialTheme.colorScheme.onPrimary
                                       else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label    = folder,
                    count    = state.folderCount(folder),
                    selected = state.selectedFolder == folder,
                    onClick  = { onSelectFolder(folder) }
                )
            }
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color    = MaterialTheme.colorScheme.outlineVariant
        )
        DrawerItem(
            icon  = {
                Icon(
                    Icons.Default.Settings, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            label    = "Settings",
            count    = 0,
            selected = false,
            onClick  = onNavigateToSettings
        )
    }
}

@Composable
private fun DrawerItem(
    icon: @Composable () -> Unit,
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "drawer_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.onSurface,
        label = "drawer_text"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            modifier  = Modifier.weight(1f),
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis
        )
        if (count > 0) {
            Spacer(Modifier.width(8.dp))
            Text(
                text  = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ─── Notes List ───────────────────────────────────────────────────────────────

@Composable
private fun NotesSectionsList(
    state: NotesListState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onOpen: (Long) -> Unit,
    onPin: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onToggleFolder: (String) -> Unit
) {
    LazyColumn(
        state           = listState,
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(start = 12.dp, end = 12.dp, bottom = 100.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (state.selectedFolder != null) {
            if (state.pinned.isNotEmpty()) {
                item(key = "header_pinned") { SectionHeader("Pinned") }
                items(state.pinned, key = { "pinned_${it.id}" }) { note ->
                    SwipeableNoteCard(note, onOpen, onPin, onDelete,
                        modifier = Modifier.animateItem(
                            fadeInSpec  = spring(stiffness = Spring.StiffnessMediumLow),
                            fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ))
                }
                if (state.nonPinned.isNotEmpty()) {
                    item(key = "header_folder") { SectionHeader(state.selectedFolder) }
                }
            }
            items(state.nonPinned, key = { "folder_note_${it.id}" }) { note ->
                SwipeableNoteCard(note, onOpen, onPin, onDelete,
                    modifier = Modifier.animateItem(
                        fadeInSpec  = spring(stiffness = Spring.StiffnessMediumLow),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ))
            }
        } else {
            if (state.pinned.isNotEmpty()) {
                item(key = "header_pinned") { SectionHeader("Pinned") }
                items(state.pinned, key = { "pinned_${it.id}" }) { note ->
                    SwipeableNoteCard(note, onOpen, onPin, onDelete,
                        modifier = Modifier.animateItem(
                            fadeInSpec  = spring(stiffness = Spring.StiffnessMediumLow),
                            fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ))
                }
            }
            state.folders.forEach { folder ->
                val folderNotes = state.notesInFolder(folder)
                if (folderNotes.isNotEmpty()) {
                    val isCollapsed = folder in state.collapsedFolders
                    item(key = "header_$folder") {
                        SectionHeader(
                            title       = folder,
                            isCollapsed = isCollapsed,
                            onToggle    = { onToggleFolder(folder) }
                        )
                    }
                    if (!isCollapsed) {
                        items(folderNotes, key = { "folder_${folder}_${it.id}" }) { note ->
                            SwipeableNoteCard(note, onOpen, onPin, onDelete,
                                modifier = Modifier.animateItem(
                                    fadeInSpec  = spring(stiffness = Spring.StiffnessMediumLow),
                                    fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ))
                        }
                    }
                }
            }
            if (state.unfiled.isNotEmpty()) {
                if (state.pinned.isNotEmpty() || state.folders.any { state.notesInFolder(it).isNotEmpty() }) {
                    item(key = "header_notes") { SectionHeader("Notes") }
                }
                items(state.unfiled, key = { "unfiled_${it.id}" }) { note ->
                    SwipeableNoteCard(note, onOpen, onPin, onDelete,
                        modifier = Modifier.animateItem(
                            fadeInSpec  = spring(stiffness = Spring.StiffnessMediumLow),
                            fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    isCollapsed: Boolean = false,
    onToggle: (() -> Unit)? = null
) {
    val chevronAngle by animateFloatAsState(
        targetValue   = if (isCollapsed) -90f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "chevron_rotate"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(start = 4.dp, top = 12.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text     = title.uppercase(),
            style    = MaterialTheme.typography.labelSmall.copy(
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (onToggle != null) {
            Icon(
                imageVector        = Icons.Default.ExpandMore,
                contentDescription = if (isCollapsed) "Expand" else "Collapse",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier           = Modifier.size(18.dp).rotate(chevronAngle)
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

// ─── Swipeable Card ───────────────────────────────────────────────────────────

@Composable
private fun SwipeableNoteCard(
    note: Note,
    onClick: (Long) -> Unit,
    onPin: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(note.id); true }
            else false
        },
        positionalThreshold = { it * 0.45f }
    )
    SwipeToDismissBox(
        state                    = dismissState,
        enableDismissFromStartToEnd = false,
        modifier                 = modifier,
        backgroundContent = {
            val progress = dismissState.progress
            val color by animateColorAsState(
                targetValue   = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.error.copy(alpha = (progress * 2).coerceIn(0f, 1f))
                    else Color.Transparent,
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    ) {
        NoteCard(note = note, onClick = { onClick(note.id) }, onPin = { onPin(note.id) })
    }
}

// ─── Note Card ────────────────────────────────────────────────────────────────

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, onPin: () -> Unit) {
    Surface(
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(16.dp),
        color         = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.5.dp,
        tonalElevation  = 1.dp,
        onClick       = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = note.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))

                // Date + snippet on one line
                val snippet = note.content?.stripHtml()?.ifBlank { null }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = formatNoteDate(note.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (snippet != null) {
                        Text(
                            text     = "  $snippet",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                // Folder chip
                if (note.folderName != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier          = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Folder, null,
                            modifier = Modifier.size(10.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            note.folderName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pin button
            IconButton(onClick = onPin, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (note.isPinned) "Unpin" else "Pin",
                    tint     = if (note.isPinned) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── Search Bar ───────────────────────────────────────────────────────────────

@Composable
private fun NoteSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Search, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value         = query,
                onValueChange = onQueryChange,
                singleLine    = true,
                textStyle     = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                modifier      = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Search notes…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                    inner()
                }
            )
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun String.stripHtml(): String =
    replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ").replace("&amp;", "&")
        .replace("&lt;", "<").replace("&gt;", ">")
        .replace(Regex("\\s+"), " ").trim()

private fun formatNoteDate(updatedAt: String): String {
    return try {
        val cleaned = updatedAt.take(19).replace(" ", "T")
        val dt  = LocalDateTime.parse(cleaned, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        val now = LocalDateTime.now()
        when {
            dt.toLocalDate() == now.toLocalDate()              -> dt.format(DateTimeFormatter.ofPattern("h:mm a"))
            dt.toLocalDate() == now.minusDays(1).toLocalDate() -> "Yesterday"
            dt.year == now.year                                -> dt.format(DateTimeFormatter.ofPattern("MMM d"))
            else                                               -> dt.format(DateTimeFormatter.ofPattern("MM/dd/yy"))
        }
    } catch (_: Exception) { updatedAt.take(10) }
}
