package com.shubham.myhub.ui.notes

import android.os.SystemClock
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shubham.myhub.data.local.entity.NoteEntity
import kotlinx.coroutines.delay

private val BackgroundColor = Color(0xFF0F1117)
private val SurfaceColor = Color(0xFF1C2330)
private val SurfaceVariantColor = Color(0xFF263042)
private val AccentColor = Color(0xFF8AB4F8)
private val TextPrimaryColor = Color(0xFFE3E2E6)
private val TextSecondaryColor = Color(0xFF9AA0A6)
private val HintColor = Color(0xFF5F6368)
private val DangerColor = Color(0xFFE57373)

@Composable
fun NotesScreen(viewModel: NotesViewModel) {
    val notesList by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showSearchField by rememberSaveable { mutableStateOf(searchQuery.isNotEmpty()) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Save drafts and close locked notes whenever the app leaves the foreground.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppBackgrounded() }

    val deleted = viewModel.recentlyDeleted
    LaunchedEffect(deleted) {
        if (deleted == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Note deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.clearRecentlyDeleted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            NotesHeader(
                isSearchVisible = showSearchField,
                onSearchClick = {
                    showSearchField = !showSearchField
                    if (!showSearchField) viewModel.onSearchQueryChanged("")
                }
            )

            AnimatedVisibility(visible = showSearchField) {
                SearchField(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (notesList.isEmpty()) {
                EmptyNotesState(
                    isSearching = searchQuery.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(notesList, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            onClick = { viewModel.onNoteClick(note) },
                            onToggleLock = { viewModel.onToggleLockClick(note) },
                            onDelete = { viewModel.onDeleteClick(note) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = viewModel::createNote,
            containerColor = AccentColor,
            contentColor = BackgroundColor,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New note")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = SurfaceVariantColor,
                contentColor = TextPrimaryColor,
                actionColor = AccentColor
            )
        }
    }

    viewModel.editor?.let { state ->
        // key() gives every opened note fresh editor state instead of reusing the last one.
        key(state.sessionKey) {
            NoteEditor(
                state = state,
                onTitleChange = viewModel::onTitleChange,
                onContentChange = viewModel::onContentChange,
                onCategoryChange = viewModel::onCategoryChange,
                onToggleLock = viewModel::toggleLockInEditor,
                onDelete = viewModel::deleteFromEditor,
                onClose = viewModel::closeEditor
            )
        }
    }

    viewModel.authPrompt?.let { prompt ->
        key(prompt.action) {
            PasswordUnlockDialog(
                prompt = prompt,
                onSubmit = viewModel::submitPassword,
                onDismiss = viewModel::dismissAuth
            )
        }
    }
}

@Composable
private fun NotesHeader(isSearchVisible: Boolean, onSearchClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Notes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Capture your thoughts, ideas and more",
                fontSize = 13.sp,
                color = TextSecondaryColor
            )
        }

        Surface(
            onClick = onSearchClick,
            shape = CircleShape,
            color = if (isSearchVisible) AccentColor else SurfaceColor,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearchVisible) "Close search" else "Search",
                    tint = if (isSearchVisible) BackgroundColor else TextPrimaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search notes", color = HintColor) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryColor) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondaryColor)
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceColor,
            unfocusedContainerColor = SurfaceColor,
            focusedBorderColor = AccentColor,
            unfocusedBorderColor = SurfaceVariantColor,
            focusedTextColor = TextPrimaryColor,
            unfocusedTextColor = TextPrimaryColor,
            cursorColor = AccentColor
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
    )
}

@Composable
private fun EmptyNotesState(isSearching: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isSearching) Icons.Default.SearchOff else Icons.AutoMirrored.Filled.StickyNote2,
                contentDescription = null,
                tint = SurfaceVariantColor,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isSearching) "No matching notes" else "No notes yet",
                color = TextPrimaryColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isSearching) "Locked notes are only matched by title" else "Tap + to write your first note",
                color = TextSecondaryColor,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun rememberEditedLabel(millis: Long): String {
    val context = LocalContext.current
    return remember(millis) {
        DateUtils.formatDateTime(
            context,
            millis,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_TIME
        )
    }
}

@Composable
private fun NoteCardItem(
    note: NoteEntity,
    onClick: () -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val category = NoteCategory.from(note.categoryIcon)
    val dateStr = rememberEditedLabel(note.updatedAtMillis)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = SurfaceColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(start = 14.dp, top = 8.dp, end = 4.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = category.emoji, fontSize = 15.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (note.title.isBlank()) TextSecondaryColor else TextPrimaryColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Note options",
                            tint = TextSecondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = SurfaceVariantColor
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (note.isLocked) "Remove lock" else "Lock note", color = TextPrimaryColor) },
                            onClick = {
                                showMenu = false
                                onToggleLock()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (note.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AccentColor
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = DangerColor) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = DangerColor)
                            }
                        )
                    }
                }
            }

            Column(Modifier.padding(end = 10.dp)) {
                Spacer(Modifier.height(6.dp))

                if (note.isLocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariantColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Locked", fontSize = 13.sp, color = TextSecondaryColor)
                    }
                } else if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSecondaryColor,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(text = dateStr, fontSize = 11.sp, color = HintColor)
            }
        }
    }
}

@Composable
private fun NoteEditor(
    state: NoteEditorState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onCategoryChange: (NoteCategory) -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val contentFocus = remember { FocusRequester() }
    val isNewNote = state.noteId == 0L
    LaunchedEffect(Unit) { if (isNewNote) contentFocus.requestFocus() }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            // Keeps locked notes out of screenshots and the recent-apps preview.
            securePolicy = if (state.isLocked) SecureFlagPolicy.SecureOn else SecureFlagPolicy.Inherit
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = BackgroundColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Save and close", tint = TextPrimaryColor)
                    }
                    Spacer(Modifier.weight(1f))

                    IconButton(onClick = onToggleLock) {
                        Icon(
                            imageVector = if (state.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (state.isLocked) "Remove lock" else "Lock note",
                            tint = if (state.isLocked) AccentColor else TextSecondaryColor
                        )
                    }
                    if (!isNewNote) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete note", tint = DangerColor)
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = AccentColor)
                    }
                }

                if (state.isLocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .background(SurfaceVariantColor, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Locked note", fontSize = 12.sp, color = TextSecondaryColor)
                    }
                }

                EditorTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    placeholder = "Title",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    singleLine = true,
                    imeAction = ImeAction.Next,
                    onImeAction = { contentFocus.requestFocus() },
                    modifier = Modifier.fillMaxWidth()
                )

                EditorTextField(
                    value = state.content,
                    onValueChange = onContentChange,
                    placeholder = "Note",
                    fontSize = 16.sp,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(contentFocus)
                )

                HorizontalDivider(color = SurfaceVariantColor)
                CategoryPicker(selected = NoteCategory.from(state.category), onSelect = onCategoryChange)
                Text(
                    text = state.updatedAtMillis?.let { "Edited ${rememberEditedLabel(it)}" } ?: "New note",
                    fontSize = 11.sp,
                    color = HintColor,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {}
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = fontSize, fontWeight = fontWeight, color = HintColor) },
        textStyle = LocalTextStyle.current.copy(
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = TextPrimaryColor,
            lineHeight = fontSize * 1.4f
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = AccentColor
        ),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onNext = { onImeAction() }),
        modifier = modifier
    )
}

@Composable
private fun CategoryPicker(selected: NoteCategory, onSelect: (NoteCategory) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        NoteCategory.entries.forEach { category ->
            val isSelected = category == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(if (isSelected) SurfaceVariantColor else SurfaceColor, RoundedCornerShape(50))
                    .border(1.dp, if (isSelected) AccentColor else Color.Transparent, RoundedCornerShape(50))
                    .clickable { onSelect(category) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(category.emoji, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = category.label,
                    fontSize = 13.sp,
                    color = if (isSelected) AccentColor else TextSecondaryColor
                )
            }
        }
    }
}

@Composable
private fun PasswordUnlockDialog(
    prompt: AuthPrompt,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var passwordInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Clear the field after every wrong attempt.
    LaunchedEffect(prompt.failedAttemptId) {
        if (prompt.failedAttemptId > 0) passwordInput = ""
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    var secondsLeft by remember { mutableLongStateOf(0L) }
    LaunchedEffect(prompt.lockedOutUntil) {
        while (true) {
            val remaining = prompt.lockedOutUntil - SystemClock.elapsedRealtime()
            secondsLeft = if (remaining > 0) (remaining + 999) / 1000 else 0
            if (secondsLeft == 0L) break
            delay(250)
        }
    }
    val isLockedOut = secondsLeft > 0
    val canSubmit = !isLockedOut && passwordInput.isNotEmpty()

    val (title, actionLabel) = when (prompt.action) {
        is ProtectedAction.Open -> "Locked note" to "Unlock"
        is ProtectedAction.RemoveLock -> "Remove lock" to "Remove lock"
        is ProtectedAction.Delete -> "Delete locked note" to "Delete"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AccentColor) },
        title = { Text(title, color = TextPrimaryColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter your password to continue.", fontSize = 14.sp, color = TextSecondaryColor)
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { new -> passwordInput = new.filter(Char::isDigit).take(MAX_PASSWORD_LENGTH) },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSubmit(passwordInput) }),
                    singleLine = true,
                    enabled = !isLockedOut,
                    isError = prompt.errorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentColor,
                        unfocusedBorderColor = SurfaceVariantColor,
                        focusedLabelColor = AccentColor,
                        unfocusedLabelColor = TextSecondaryColor,
                        focusedTextColor = TextPrimaryColor,
                        unfocusedTextColor = TextPrimaryColor,
                        cursorColor = AccentColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                when {
                    isLockedOut -> Text(
                        text = "Too many wrong attempts. Try again in ${secondsLeft}s.",
                        color = DangerColor,
                        fontSize = 12.sp
                    )
                    prompt.errorMessage != null -> Text(prompt.errorMessage, color = DangerColor, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(passwordInput) },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (prompt.action is ProtectedAction.Delete) DangerColor else AccentColor,
                    contentColor = BackgroundColor
                )
            ) {
                Text(actionLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryColor)
            }
        }
    )
}

private const val MAX_PASSWORD_LENGTH = 12
