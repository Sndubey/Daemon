package com.shubham.myhub.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shubham.myhub.data.local.entity.TimetableEntryEntity
import com.shubham.myhub.data.local.entity.TimetableWithEntries
import java.util.Calendar

private val BackgroundColor = Color(0xFF0F1117)
private val SurfaceColor = Color(0xFF1C2330)
private val SurfaceVariantColor = Color(0xFF263042)
private val AccentColor = Color(0xFF8AB4F8)
private val TextPrimaryColor = Color(0xFFE3E2E6)
private val TextSecondaryColor = Color(0xFF9AA0A6)
private val HintColor = Color(0xFF5F6368)
private val DangerColor = Color(0xFFE57373)

private val DayShortNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val DayFullNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

/** Which text dialog is open, if any. */
private sealed interface TimetableDialog {
    data object NewTimetable : TimetableDialog
    data class Rename(val id: Long, val currentTitle: String) : TimetableDialog
    data class ConfirmDelete(val id: Long, val title: String) : TimetableDialog
    data class AddEntry(val timetableId: Long, val dayOfWeek: Int) : TimetableDialog
    data class EditEntry(val entry: TimetableEntryEntity) : TimetableDialog
}

@Composable
fun TimetableScreen(viewModel: TimetableViewModel) {
    val timetables by viewModel.timetables.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<TimetableDialog?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    // 0 = Sunday, matching TimetableEntryEntity.dayOfWeek.
    val today = remember { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 }

    val deleted = viewModel.recentlyDeleted
    LaunchedEffect(deleted) {
        if (deleted == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Timetable deleted",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp)
        ) {
            item(key = "header") {
                Column {
                    Text("Timetable", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor)
                    Spacer(Modifier.height(4.dp))
                    Text("Your weekly plans, Sunday to Saturday", fontSize = 13.sp, color = TextSecondaryColor)
                }
            }

            if (timetables.isEmpty()) {
                item(key = "empty") {
                    EmptyTimetables(onCreate = { dialog = TimetableDialog.NewTimetable })
                }
            } else {
                items(timetables, key = { it.timetable.id }) { timetable ->
                    TimetableCard(
                        data = timetable,
                        today = today,
                        onAddEntry = { day -> dialog = TimetableDialog.AddEntry(timetable.timetable.id, day) },
                        onEntryClick = { entry -> dialog = TimetableDialog.EditEntry(entry) },
                        onRename = {
                            dialog = TimetableDialog.Rename(timetable.timetable.id, timetable.timetable.title)
                        },
                        onDelete = {
                            dialog = TimetableDialog.ConfirmDelete(timetable.timetable.id, timetable.timetable.title)
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { dialog = TimetableDialog.NewTimetable },
            containerColor = AccentColor,
            contentColor = BackgroundColor,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New timetable")
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

    when (val current = dialog) {
        null -> Unit
        TimetableDialog.NewTimetable -> TextInputDialog(
            title = "New timetable",
            initialText = "",
            placeholder = "e.g. College, Gym, Work week",
            confirmLabel = "Create",
            onConfirm = { viewModel.createTimetable(it); dialog = null },
            onDismiss = { dialog = null }
        )
        is TimetableDialog.Rename -> TextInputDialog(
            title = "Rename timetable",
            initialText = current.currentTitle,
            confirmLabel = "Save",
            onConfirm = { viewModel.renameTimetable(current.id, it); dialog = null },
            onDismiss = { dialog = null }
        )
        is TimetableDialog.ConfirmDelete -> AlertDialog(
            onDismissRequest = { dialog = null },
            containerColor = SurfaceColor,
            title = { Text("Delete timetable?", color = TextPrimaryColor) },
            text = {
                Text("\"${current.title}\" and all its tasks will be removed.", color = TextSecondaryColor)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTimetable(current.id); dialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerColor, contentColor = BackgroundColor)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("Cancel", color = TextSecondaryColor) }
            }
        )
        is TimetableDialog.AddEntry -> TextInputDialog(
            title = "Add to ${DayFullNames[current.dayOfWeek]}",
            initialText = "",
            placeholder = "e.g. 9:00 Maths lecture",
            confirmLabel = "Add",
            onConfirm = { viewModel.addEntry(current.timetableId, current.dayOfWeek, it); dialog = null },
            onDismiss = { dialog = null }
        )
        is TimetableDialog.EditEntry -> TextInputDialog(
            title = "Edit ${DayFullNames[current.entry.dayOfWeek]} task",
            initialText = current.entry.text,
            confirmLabel = "Save",
            onConfirm = {
                if (it.trim() != current.entry.text) viewModel.updateEntry(current.entry.id, it)
                dialog = null
            },
            onDelete = { viewModel.deleteEntry(current.entry.id); dialog = null },
            onDismiss = { dialog = null }
        )
    }
}

@Composable
private fun EmptyTimetables(onCreate: () -> Unit) {
    Surface(
        onClick = onCreate,
        shape = RoundedCornerShape(20.dp),
        color = SurfaceColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 40.dp, horizontal = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = SurfaceVariantColor,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("No timetables yet", color = TextPrimaryColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Tap + to create your first weekly timetable", color = TextSecondaryColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TimetableCard(
    data: TimetableWithEntries,
    today: Int,
    onAddEntry: (dayOfWeek: Int) -> Unit,
    onEntryClick: (TimetableEntryEntity) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entriesByDay = remember(data.entries) {
        data.entries.sortedBy { it.createdAtMillis }.groupBy { it.dayOfWeek }
    }
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = data.timetable.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Timetable options", tint = TextSecondaryColor)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = SurfaceVariantColor
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename", color = TextPrimaryColor) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = AccentColor) },
                            onClick = { showMenu = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = DangerColor) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerColor) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            HorizontalDivider(color = SurfaceVariantColor)

            DayShortNames.forEachIndexed { day, _ ->
                DayRow(
                    day = day,
                    isToday = day == today,
                    entries = entriesByDay[day].orEmpty(),
                    onAdd = { onAddEntry(day) },
                    onEntryClick = onEntryClick
                )
                if (day < DayShortNames.lastIndex) {
                    HorizontalDivider(color = SurfaceVariantColor, modifier = Modifier.padding(start = 72.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DayRow(
    day: Int,
    isToday: Boolean,
    entries: List<TimetableEntryEntity>,
    onAdd: () -> Unit,
    onEntryClick: (TimetableEntryEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Left: weekday name
        Box(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .padding(start = 12.dp, top = 10.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = DayShortNames[day],
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isToday) BackgroundColor else TextSecondaryColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isToday) AccentColor else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Right: that day's tasks; tapping empty space adds one.
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onAdd, onClickLabel = "Add task to ${DayFullNames[day]}")
                .padding(top = 6.dp, bottom = 6.dp, end = 4.dp)
        ) {
            if (entries.isEmpty()) {
                Text(
                    text = "Tap to add",
                    fontSize = 13.sp,
                    color = HintColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                entries.forEach { entry ->
                    Text(
                        text = entry.text,
                        fontSize = 14.sp,
                        color = TextPrimaryColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp, bottom = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isToday) SurfaceVariantColor else BackgroundColor.copy(alpha = 0.5f))
                            .clickable(onClickLabel = "Edit task") { onEntryClick(entry) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }

        IconButton(onClick = onAdd, modifier = Modifier.padding(top = 2.dp)) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add task to ${DayFullNames[day]}",
                tint = if (isToday) AccentColor else HintColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    initialText: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    placeholder: String = "",
    onDelete: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = TextPrimaryColor, modifier = Modifier.weight(1f))
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerColor)
                    }
                }
            }
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder, color = HintColor) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onConfirm(text) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentColor,
                    unfocusedBorderColor = SurfaceVariantColor,
                    focusedTextColor = TextPrimaryColor,
                    unfocusedTextColor = TextPrimaryColor,
                    cursorColor = AccentColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = BackgroundColor)
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondaryColor) }
        }
    )
}
