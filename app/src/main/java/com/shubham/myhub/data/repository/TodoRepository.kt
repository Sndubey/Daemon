package com.shubham.myhub.data.repository

import com.shubham.myhub.data.local.dao.GoalDao
import com.shubham.myhub.data.local.dao.TodoDao
import com.shubham.myhub.data.local.entity.GoalEntity
import com.shubham.myhub.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val todoDao: TodoDao,
    private val goalDao: GoalDao,
    /** Called after every to-do change, e.g. to refresh the home-screen widget. */
    private val onTodosChanged: suspend () -> Unit = {}
) {
    val allTodos: Flow<List<TodoEntity>> = todoDao.getAllTodos()
    val remainingTodosCount: Flow<Int> = todoDao.getRemainingCount()
    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()

    suspend fun addTodo(text: String, timeStr: String = "") {
        if (text.isBlank()) return
        // New to-dos go to the top, as before reordering existed.
        val todo = TodoEntity(
            text = text.trim(),
            timeStr = timeStr.trim(),
            sortOrder = todoDao.minSortOrder() - 1
        )
        todoDao.insertTodo(todo)
        notifyTodosChanged()
    }

    suspend fun toggleTodoCompleted(id: Long) {
        todoDao.toggleCompleted(id)
        notifyTodosChanged()
    }

    suspend fun updateTodoText(id: Long, text: String) {
        if (text.isBlank()) return
        todoDao.updateText(id, text.trim())
        notifyTodosChanged()
    }

    suspend fun deleteTodo(todo: TodoEntity) {
        todoDao.deleteTodo(todo)
        notifyTodosChanged()
    }

    /** Persists a drag-and-drop order; the widget picks it up via the shared query. */
    suspend fun reorderTodos(idsInOrder: List<Long>) {
        todoDao.updateSortOrders(idsInOrder)
        notifyTodosChanged()
    }

    suspend fun deleteTodoById(id: Long) {
        todoDao.deleteById(id)
        notifyTodosChanged()
    }

    /** Re-inserts a deleted todo with its original id (used for undo). */
    suspend fun restoreTodo(todo: TodoEntity) {
        todoDao.insertTodo(todo)
        notifyTodosChanged()
    }

    private suspend fun notifyTodosChanged() {
        // A widget refresh failing must never break saving the to-do itself.
        runCatching { onTodosChanged() }
    }

    suspend fun addGoal(title: String, description: String, progressPercentage: Int, iconType: String) {
        if (title.isBlank()) return
        val goal = GoalEntity(
            title = title.trim(),
            description = description.trim(),
            progressPercentage = progressPercentage.coerceIn(0, 100),
            iconType = iconType
        )
        goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: GoalEntity) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.deleteGoal(goal)
    }

    /** Re-inserts a deleted goal with its original id (used for undo). */
    suspend fun restoreGoal(goal: GoalEntity) {
        goalDao.insertGoal(goal)
    }
}
