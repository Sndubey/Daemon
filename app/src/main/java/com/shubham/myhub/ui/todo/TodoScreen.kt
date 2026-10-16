package com.shubham.myhub.ui.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shubham.myhub.data.local.entity.GoalEntity
import com.shubham.myhub.data.local.entity.TodoEntity

@Composable
fun TodoScreen(viewModel: TodoViewModel) {
    val todosList by viewModel.todos.collectAsState()
    val remainingCount by viewModel.remainingCount.collectAsState()
    val goalsList by viewModel.goals.collectAsState()

    var quickTodoText by remember { mutableStateOf("") }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1117))
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = viewModel.getGreeting(),
                        fontSize = 14.sp,
                        color = Color(0xFF9AA0A6)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Let's get things done ✨",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE3E2E6)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1C2330),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Theme",
                            tint = Color(0xFFE3E2E6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Add Quick To-Do Input Bar
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF1C2330),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = Color(0xFF5F6368),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    TextField(
                        value = quickTodoText,
                        onValueChange = { quickTodoText = it },
                        placeholder = {
                            Text("Add a quick todo...", color = Color(0xFF5F6368), fontSize = 15.sp)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFFE3E2E6),
                            unfocusedTextColor = Color(0xFFE3E2E6)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (quickTodoText.isNotBlank()) {
                                viewModel.addTodo(quickTodoText, "10:00 AM")
                                quickTodoText = ""
                            }
                        }),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF8AB4F8),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                if (quickTodoText.isNotBlank()) {
                                    viewModel.addTodo(quickTodoText, "10:00 AM")
                                    quickTodoText = ""
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color(0xFF0F1117),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // Today Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE3E2E6)
                )
                Text(
                    text = "$remainingCount remaining",
                    fontSize = 13.sp,
                    color = Color(0xFF9AA0A6)
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1C2330),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (todosList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No items for today yet", color = Color(0xFF9AA0A6), fontSize = 14.sp)
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        todosList.forEachIndexed { index, todo ->
                            TodoRowItem(
                                todo = todo,
                                onToggle = { viewModel.toggleTodo(todo) },
                                onDelete = { viewModel.deleteTodo(todo) }
                            )
                            if (index < todosList.size - 1) {
                                HorizontalDivider(
                                    color = Color(0xFF263042),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Goals Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goals",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE3E2E6)
                )
                IconButton(onClick = { showAddGoalDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Goal",
                        tint = Color(0xFF8AB4F8)
                    )
                }
            }
        }

        // Goals List
        if (goalsList.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1C2330),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No long-term goals yet. Tap + to add one!", color = Color(0xFF9AA0A6), fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(goalsList, key = { it.id }) { goal ->
                GoalCardItem(
                    goal = goal,
                    onClick = { editingGoal = goal }
                )
            }
        }
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onAdd = { title, desc, progress, icon ->
                viewModel.addGoal(title, desc, progress, icon)
                showAddGoalDialog = false
            }
        )
    }

    editingGoal?.let { goal ->
        EditGoalDialog(
            goal = goal,
            onDismiss = { editingGoal = null },
            onSave = { updatedTitle, updatedDesc, updatedProgress ->
                viewModel.updateGoal(goal, updatedTitle, updatedDesc, updatedProgress)
                editingGoal = null
            },
            onDelete = {
                viewModel.deleteGoal(goal)
                editingGoal = null
            }
        )
    }
}

@Composable
fun TodoRowItem(
    todo: TodoEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (todo.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (todo.isCompleted) Color(0xFF8AB4F8) else Color(0xFF5F6368),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todo.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (todo.isCompleted) Color(0xFF9AA0A6) else Color(0xFFE3E2E6),
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
            if (todo.timeStr.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = todo.timeStr,
                    fontSize = 12.sp,
                    color = Color(0xFF9AA0A6)
                )
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun GoalCardItem(
    goal: GoalEntity,
    onClick: () -> Unit
) {
    val (bgColor, iconVector) = when (goal.iconType) {
        "LAPTOP" -> Color(0xFF2D2B52) to Icons.Default.Computer
        "CHART" -> Color(0xFF1E3A34) to Icons.Default.BarChart
        "BOOK" -> Color(0xFF3E2D1E) to Icons.AutoMirrored.Filled.MenuBook
        else -> Color(0xFF263042) to Icons.Default.Flag
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1C2330),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = Color(0xFF8AB4F8),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE3E2E6)
                )
                if (goal.description.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = goal.description,
                        fontSize = 13.sp,
                        color = Color(0xFF9AA0A6)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF263042)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${goal.progressPercentage}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8AB4F8)
                )
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, desc: String, progress: Int, iconType: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var progressStr by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C2330),
        title = { Text("Create Long-Term Goal", color = Color(0xFFE3E2E6)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (e.g. App Development)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") }
                )
                OutlinedTextField(
                    value = progressStr,
                    onValueChange = { progressStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Progress Percentage (0-100)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val progress = progressStr.toIntOrNull() ?: 0
                    onAdd(title, desc, progress, "GENERAL")
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF9AA0A6))
            }
        }
    )
}

@Composable
fun EditGoalDialog(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, progress: Int) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(goal.title) }
    var desc by remember { mutableStateOf(goal.description) }
    var progressVal by remember { mutableStateOf(goal.progressPercentage.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C2330),
        title = { Text("Edit Goal", color = Color(0xFFE3E2E6)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") }
                )
                Column {
                    Text(
                        text = "Goal Progress: ${progressVal.toInt()}%",
                        color = Color(0xFFE3E2E6),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = progressVal,
                        onValueChange = { progressVal = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF8AB4F8),
                            activeTrackColor = Color(0xFF8AB4F8),
                            inactiveTrackColor = Color(0xFF263042)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text("Delete")
                }
                Button(
                    onClick = { onSave(title, desc, progressVal.toInt()) },
                    enabled = title.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF9AA0A6))
            }
        }
    )
}
