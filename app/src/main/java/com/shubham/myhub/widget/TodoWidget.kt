package com.shubham.myhub.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.shubham.myhub.MainActivity
import com.shubham.myhub.MyHubApplication
import com.shubham.myhub.R
import com.shubham.myhub.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.first

private val SurfaceColor = ColorProvider(Color(0xFF1C2330))
private val AccentColor = ColorProvider(Color(0xFF8AB4F8))
private val BackgroundColor = ColorProvider(Color(0xFF0F1117))
private val TextPrimaryColor = ColorProvider(Color(0xFFE3E2E6))
private val TextSecondaryColor = ColorProvider(Color(0xFF9AA0A6))
private val HintColor = ColorProvider(Color(0xFF5F6368))

/** Resizable home-screen widget listing to-dos, Google Keep style. */
class TodoWidget : GlanceAppWidget() {

    // Re-lay out for the exact size the user resizes the widget to.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as MyHubApplication).todoRepository
        val initialTodos = repository.allTodos.first()

        provideContent {
            val todos by repository.allTodos.collectAsState(initial = initialTodos)
            TodoWidgetContent(todos)
        }
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}

/** Ticks or unticks a to-do straight from the widget. */
class ToggleTodoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TodoIdKey] ?: return
        (context.applicationContext as MyHubApplication).todoRepository.toggleTodoCompleted(id)
    }

    companion object {
        val TodoIdKey = ActionParameters.Key<Long>("todo_id")
    }
}

/** Deletes a to-do straight from the widget's X button. */
class DeleteTodoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ToggleTodoAction.TodoIdKey] ?: return
        (context.applicationContext as MyHubApplication).todoRepository.deleteTodoById(id)
    }
}

@Composable
private fun TodoWidgetContent(todos: List<TodoEntity>) {
    val size = LocalSize.current
    val isCompact = size.height < 140.dp || size.width < 160.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(SurfaceColor)
            .cornerRadius(24.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(start = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text(
                    text = "To Do",
                    style = TextStyle(color = TextPrimaryColor, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }

            Box(
                modifier = GlanceModifier
                    .size(if (isCompact) 32.dp else 38.dp)
                    .background(AccentColor)
                    .cornerRadius(19.dp)
                    .clickable(actionStartActivity<QuickAddTodoActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = "Add to-do",
                    colorFilter = ColorFilter.tint(BackgroundColor),
                    modifier = GlanceModifier.size(20.dp)
                )
            }
        }

        if (todos.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity<QuickAddTodoActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap + to add a to-do",
                    style = TextStyle(color = TextSecondaryColor, fontSize = 13.sp)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(todos, itemId = { it.id }) { todo ->
                    TodoWidgetRow(todo)
                }
            }
        }
    }
}

private const val ROW_MAX_LINES = 3

/**
 * Widgets can't ellipsize text themselves (Glance only clips at maxLines), so long to-dos are
 * shortened here to roughly fit [ROW_MAX_LINES] lines at the widget's width, ending in "…".
 */
private fun ellipsizeForWidget(text: String, charsPerLine: Int, maxLines: Int): String {
    var linesUsed = 0
    val result = StringBuilder()
    val lines = text.trim().lines()
    for ((index, line) in lines.withIndex()) {
        val visualLines = maxOf(1, (line.length + charsPerLine - 1) / charsPerLine)
        val linesLeft = maxLines - linesUsed
        if (visualLines > linesLeft) {
            // Fill what's left of the space, leaving room for the ellipsis.
            val room = (linesLeft * charsPerLine - 1).coerceAtLeast(0)
            result.append(line.take(room).trimEnd()).append('…')
            return result.toString()
        }
        result.append(line)
        linesUsed += visualLines
        if (index < lines.lastIndex) {
            if (linesUsed >= maxLines) return result.toString().trimEnd() + "…"
            result.append('\n')
        }
    }
    return result.toString()
}

@Composable
private fun TodoWidgetRow(todo: TodoEntity) {
    val todoParams = actionParametersOf(ToggleTodoAction.TodoIdKey to todo.id)
    // Text width = widget minus side padding (24dp), checkbox (~48dp) and the X button (36dp).
    // 14sp text averages ~7.5dp per character; 8.5dp leaves slack for word wrapping
    // so the "…" itself isn't clipped.
    val textWidthDp = LocalSize.current.width.value - 24f - 48f - 36f
    val charsPerLine = (textWidthDp / 8.5f).toInt().coerceAtLeast(8)
    val displayText = remember(todo.text, charsPerLine) {
        ellipsizeForWidget(todo.text, charsPerLine, ROW_MAX_LINES)
    }
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckBox(
            checked = todo.isCompleted,
            onCheckedChange = actionRunCallback<ToggleTodoAction>(todoParams),
            text = displayText,
            style = TextStyle(
                color = if (todo.isCompleted) HintColor else TextPrimaryColor,
                fontSize = 14.sp,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            ),
            colors = CheckboxDefaults.colors(checkedColor = AccentColor, uncheckedColor = TextSecondaryColor),
            maxLines = ROW_MAX_LINES,
            modifier = GlanceModifier.defaultWeight()
        )
        // 36dp box keeps the X easy to hit without crowding the text.
        Box(
            modifier = GlanceModifier
                .size(36.dp)
                .cornerRadius(18.dp)
                .clickable(actionRunCallback<DeleteTodoAction>(todoParams)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_close),
                contentDescription = "Delete to-do",
                colorFilter = ColorFilter.tint(HintColor),
                modifier = GlanceModifier.size(18.dp)
            )
        }
    }
}
