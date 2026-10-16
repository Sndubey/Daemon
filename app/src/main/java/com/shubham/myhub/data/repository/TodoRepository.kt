package com.shubham.myhub.data.repository

import com.shubham.myhub.data.local.dao.GoalDao
import com.shubham.myhub.data.local.dao.TodoDao
import com.shubham.myhub.data.local.entity.GoalEntity
import com.shubham.myhub.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val todoDao: TodoDao,
    private val goalDao: GoalDao
) {
    val allTodos: Flow<List<TodoEntity>> = todoDao.getAllTodos()
    val remainingTodosCount: Flow<Int> = todoDao.getRemainingCount()
    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()

    suspend fun addTodo(text: String, timeStr: String = "") {
        if (text.isBlank()) return
        val todo = TodoEntity(text = text.trim(), timeStr = timeStr.trim())
        todoDao.insertTodo(todo)
    }

    suspend fun toggleTodoCompleted(todo: TodoEntity) {
        todoDao.updateTodo(todo.copy(isCompleted = !todo.isCompleted))
    }

    suspend fun deleteTodo(todo: TodoEntity) {
        todoDao.deleteTodo(todo)
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

    suspend fun updateGoalProgress(goal: GoalEntity, newProgress: Int) {
        goalDao.updateGoal(goal.copy(progressPercentage = newProgress.coerceIn(0, 100)))
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.deleteGoal(goal)
    }
}
