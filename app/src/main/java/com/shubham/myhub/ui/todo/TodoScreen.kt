package com.shubham.myhub.ui.todo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shubham.myhub.data.local.entity.GoalEntity
import com.shubham.myhub.data.local.entity.TodoEntity
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val BackgroundColor = Color(0xFF0F1117)
private val SurfaceColor = Color(0xFF1C2330)
private val SurfaceVariantColor = Color(0xFF263042)
private val AccentColor = Color(0xFF8AB4F8)
private val TextPrimaryColor = Color(0xFFE3E2E6)
private val TextSecondaryColor = Color(0xFF9AA0A6)
private val HintColor = Color(0xFF5F6368)
private val DangerColor = Color(0xFFE57373)

private enum class GoalIcon(val icon: ImageVector, val tileColor: Color, val label: String) {
    LAPTOP(Icons.Default.Computer, Color(0xFF2D2B52), "Work"),
    CHART(Icons.Default.BarChart, Color(0xFF1E3A34), "Growth"),
    BOOK(Icons.AutoMirrored.Filled.MenuBook, Color(0xFF3E2D1E), "Learning"),
    FITNESS(Icons.Default.FitnessCenter, Color(0xFF3A1E2C), "Fitness"),
    GENERAL(Icons.Default.Flag, SurfaceVariantColor, "General");

    companion object {
        fun from(key: String): GoalIcon = entries.firstOrNull { it.name == key } ?: GENERAL
    }
}

@Composable
fun TodoScreen(viewModel: TodoViewModel) {
    val todosList by viewModel.todos.collectAsStateWithLifecycle()
    val remainingCount by viewModel.remainingCount.collectAsStateWithLifecycle()
    val goalsList by viewModel.goals.collectAsStateWithLifecycle()

    var quickTodoText by remember { mutableStateOf("") }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var editingTodo by remember { mutableStateOf<TodoEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

    // Local copy of the list that drag-and-drop rearranges live; saved to the DB when the drag ends.
    // While dragging, DB updates are ignored so the list doesn't jump under the finger.
    var orderedTodos by remember { mutableStateOf(todosList) }
    var isDraggingTodo by remember { mutableStateOf(false) }
    LaunchedEffect(todosList) {
        if (!isDraggingTodo) orderedTodos = todosList
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromId = (from.key as? String)?.removePrefix(TODO_KEY_PREFIX)?.toLongOrNull()
        val toId = (to.key as? String)?.removePrefix(TODO_KEY_PREFIX)?.toLongOrNull()
        if (fromId == null || toId == null) return@rememberReorderableLazyListState
        val list = orderedTodos.toMutableList()
        val fromIndex = list.indexOfFirst { it.id == fromId }
        val toIndex = list.indexOfFirst { it.id == toId }
        if (fromIndex < 0 || toIndex < 0) return@rememberReorderableLazyListState
        // Ticked items always stay below unticked ones, so only reorder within the same group.
        if (list[fromIndex].isCompleted != list[toIndex].isCompleted) return@rememberReorderableLazyListState
        list.add(toIndex, list.removeAt(fromIndex))
        orderedTodos = list
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val deleted = viewModel.recentlyDeleted
    LaunchedEffect(deleted) {
        if (deleted == null) return@LaunchedEffect
        val message = when (deleted) {
            is DeletedItem.Todo -> "To-do deleted"
            is DeletedItem.Goal -> "Goal deleted"
        }
        val result = snackbarHostState.showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.clearRecentlyDeleted()
    }

    fun submitQuickTodo() {
        if (quickTodoText.isNotBlank()) {
            viewModel.addTodo(quickTodoText)
            quickTodoText = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp)
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(text = viewModel.getGreeting(), fontSize = 14.sp, color = TextSecondaryColor)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Let's get things done ✨",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryColor
                        )
                    }
                    // Decorative time-of-day badge (it used to look like a button that did nothing).
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (viewModel.isDaytime()) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = null,
                            tint = TextPrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            item(key = "quick_add") {
                QuickTodoBar(
                    text = quickTodoText,
                    onTextChange = { quickTodoText = it },
                    onSubmit = ::submitQuickTodo
                )
                Spacer(Modifier.height(24.dp))
            }

            item(key = "today_header") {
                SectionHeader(title = "Today") {
                    Text(
                        text = if (orderedTodos.isEmpty()) "" else "$remainingCount remaining",
                        fontSize = 13.sp,
                        color = TextSecondaryColor
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            if (orderedTodos.isEmpty()) {
                item(key = "today_empty") {
                    EmptyCard("No to-dos yet. Add one above.")
                }
            } else {
                items(orderedTodos, key = { "$TODO_KEY_PREFIX${it.id}" }) { todo ->
                    ReorderableItem(reorderState, key = "$TODO_KEY_PREFIX${todo.id}") { isDragging ->
                        TodoRowItem(
                            todo = todo,
                            isFirst = todo.id == orderedTodos.first().id,
                            isLast = todo.id == orderedTodos.last().id,
                            isDragging = isDragging,
                            dragHandleModifier = Modifier.draggableHandle(
                                onDragStarted = {
                                    isDraggingTodo = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    isDraggingTodo = false
                                    viewModel.reorderTodos(orderedTodos.map { it.id })
                                }
                            ),
                            onToggle = { viewModel.toggleTodo(todo) },
                            onEdit = { editingTodo = todo },
                            onDelete = { viewModel.deleteTodo(todo) }
                        )
                    }
                }
            }

            item(key = "goals_header") {
                Spacer(Modifier.height(24.dp))
                SectionHeader(title = "Goals") {
                    IconButton(onClick = { showAddGoalDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add goal", tint = AccentColor)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            if (goalsList.isEmpty()) {
                item(key = "goals_empty") {
                    EmptyCard("No long-term goals yet. Tap + to add one.")
                }
            } else {
                items(goalsList, key = { "goal_${it.id}" }) { goal ->
                    GoalCardItem(
                        goal = goal,
                        onClick = { editingGoal = goal },
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .animateItem()
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = SurfaceVariantColor,
                contentColor = TextPrimaryColor,
                actionColor = AccentColor
            )
        }
    }

    if (showAddGoalDialog) {
        GoalDialog(
            goal = null,
            onDismiss = { showAddGoalDialog = false },
            onSave = { title, desc, progress, icon ->
                viewModel.addGoal(title, desc, progress, icon)
                showAddGoalDialog = false
            }
        )
    }

    editingGoal?.let { goal ->
        GoalDialog(
            goal = goal,
            onDismiss = { editingGoal = null },
            onSave = { title, desc, progress, icon ->
                viewModel.updateGoal(goal, title, desc, progress, icon)
                editingGoal = null
            },
            onDelete = {
                viewModel.deleteGoal(goal)
                editingGoal = null
            }
        )
    }

    editingTodo?.let { todo ->
        EditTodoDialog(
            todo = todo,
            onDismiss = { editingTodo = null },
            onSave = { newText ->
                viewModel.updateTodoText(todo, newText)
                editingTodo = null
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimaryColor)
        trailing()
    }
}

@Composable
private fun EmptyCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor, RoundedCornerShape(20.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = TextSecondaryColor, fontSize = 14.sp)
    }
}

@Composable
private fun QuickTodoBar(text: String, onTextChange: (String) -> Unit, onSubmit: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = SurfaceColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = HintColor,
                modifier = Modifier.size(22.dp)
            )
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Add a quick to-do", color = HintColor, fontSize = 15.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimaryColor,
                    unfocusedTextColor = TextPrimaryColor,
                    cursorColor = AccentColor
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.weight(1f)
            )
            FilledIconButton(
                onClick = onSubmit,
                enabled = text.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = AccentColor,
                    contentColor = BackgroundColor,
                    disabledContainerColor = SurfaceVariantColor,
                    disabledContentColor = HintColor
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add to-do")
            }
        }
    }
}

private const val TODO_KEY_PREFIX = "todo_"

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoRowItem(
    todo: TodoEntity,
    isFirst: Boolean,
    isLast: Boolean,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A dragged row lifts out of the grouped card, so it gets all four corners rounded.
    val shape = if (isDragging) {
        RoundedCornerShape(16.dp)
    } else {
        RoundedCornerShape(
            topStart = if (isFirst) 20.dp else 0.dp,
            topEnd = if (isFirst) 20.dp else 0.dp,
            bottomStart = if (isLast) 20.dp else 0.dp,
            bottomEnd = if (isLast) 20.dp else 0.dp
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (isDragging) 12.dp else 0.dp, shape)
            .clip(shape)
            .background(if (isDragging) SurfaceVariantColor else SurfaceColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onToggle,
                    onLongClick = onEdit,
                    onLongClickLabel = "Edit to-do"
                )
                .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle: press and drag to move the to-do up or down.
            Box(
                modifier = dragHandleModifier
                    .padding(start = 4.dp)
                    .size(width = 28.dp, height = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Drag to reorder",
                    tint = if (isDragging) AccentColor else HintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (todo.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (todo.isCompleted) "Mark as not done" else "Mark as done",
                    tint = if (todo.isCompleted) AccentColor else HintColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = todo.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (todo.isCompleted) TextSecondaryColor else TextPrimaryColor,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (todo.timeStr.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(text = todo.timeStr, fontSize = 12.sp, color = TextSecondaryColor)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete to-do",
                    tint = HintColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (!isLast && !isDragging) {
            HorizontalDivider(
                color = SurfaceVariantColor,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun GoalCardItem(goal: GoalEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val goalIcon = GoalIcon.from(goal.iconType)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = SurfaceColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(goalIcon.tileColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(goalIcon.icon, contentDescription = null, tint = AccentColor, modifier = Modifier.size(24.dp))
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (goal.description.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = goal.description,
                            fontSize = 13.sp,
                            color = TextSecondaryColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${goal.progressPercentage}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentColor
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { goal.progressPercentage / 100f },
                color = AccentColor,
                trackColor = SurfaceVariantColor,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentColor,
    unfocusedBorderColor = SurfaceVariantColor,
    focusedLabelColor = AccentColor,
    unfocusedLabelColor = TextSecondaryColor,
    focusedTextColor = TextPrimaryColor,
    unfocusedTextColor = TextPrimaryColor,
    cursorColor = AccentColor
)

/** Create (goal == null) or edit a goal. */
@Composable
private fun GoalDialog(
    goal: GoalEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, progress: Int, iconType: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(goal?.title ?: "") }
    var desc by remember { mutableStateOf(goal?.description ?: "") }
    var progress by remember { mutableFloatStateOf((goal?.progressPercentage ?: 0).toFloat()) }
    var icon by remember { mutableStateOf(GoalIcon.from(goal?.iconType ?: GoalIcon.GENERAL.name)) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = SurfaceColor,
            title = { Text("Delete goal?", color = TextPrimaryColor) },
            text = { Text("\"${goal?.title}\" will be removed.", color = TextSecondaryColor) },
            confirmButton = {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerColor, contentColor = BackgroundColor)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = TextSecondaryColor) }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (goal == null) "New goal" else "Edit goal",
                    color = TextPrimaryColor,
                    modifier = Modifier.weight(1f)
                )
                if (onDelete != null) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete goal", tint = DangerColor)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. App development", color = HintColor) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon", color = TextSecondaryColor, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalIcon.entries.forEach { option ->
                        val selected = option == icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(option.tileColor)
                                .border(2.dp, if (selected) AccentColor else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { icon = option },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(option.icon, contentDescription = option.label, tint = AccentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Text("Progress: ${progress.toInt()}%", color = TextPrimaryColor, fontSize = 14.sp)
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    valueRange = 0f..100f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentColor,
                        activeTrackColor = AccentColor,
                        inactiveTrackColor = SurfaceVariantColor,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, desc, progress.toInt(), icon.name) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = BackgroundColor)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondaryColor) }
        }
    )
}

@Composable
private fun EditTodoDialog(todo: TodoEntity, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(todo.text) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = { Text("Edit to-do", color = TextPrimaryColor) },
        text = {
            // Multi-line so longer to-dos can be read and edited in full; Enter adds a new line,
            // and the box scrolls once the text passes 8 lines.
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = false,
                minLines = 2,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default
                ),
                colors = dialogFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor, contentColor = BackgroundColor)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondaryColor) }
        }
    )
}
