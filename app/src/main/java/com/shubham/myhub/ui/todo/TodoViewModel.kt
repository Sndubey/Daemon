package com.shubham.myhub.ui.todo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shubham.myhub.data.local.entity.GoalEntity
import com.shubham.myhub.data.local.entity.TodoEntity
import com.shubham.myhub.data.repository.TodoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/** Something the user just deleted and can still undo. */
sealed interface DeletedItem {
    data class Todo(val todo: TodoEntity) : DeletedItem
    data class Goal(val goal: GoalEntity) : DeletedItem
}

class TodoViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    val todos: StateFlow<List<TodoEntity>> = repository.allTodos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val remainingCount: StateFlow<Int> = repository.remainingTodosCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val goals: StateFlow<List<GoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var recentlyDeleted by mutableStateOf<DeletedItem?>(null)
        private set

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good Morning,"
            in 12..16 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
    }

    fun isDaytime(): Boolean = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 6..17

    fun addTodo(text: String) {
        viewModelScope.launch {
            repository.addTodo(text)
        }
    }

    fun toggleTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.toggleTodoCompleted(todo.id)
        }
    }

    fun updateTodoText(todo: TodoEntity, newText: String) {
        if (newText.trim() == todo.text) return
        viewModelScope.launch {
            repository.updateTodoText(todo.id, newText)
        }
    }

    fun reorderTodos(idsInOrder: List<Long>) {
        viewModelScope.launch {
            repository.reorderTodos(idsInOrder)
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
            recentlyDeleted = DeletedItem.Todo(todo)
        }
    }

    fun addGoal(title: String, description: String, progress: Int, iconType: String) {
        viewModelScope.launch {
            repository.addGoal(title, description, progress, iconType)
        }
    }

    fun updateGoal(goal: GoalEntity, newTitle: String, newDesc: String, newProgress: Int, newIconType: String) {
        val updated = goal.copy(
            title = newTitle.trim(),
            description = newDesc.trim(),
            progressPercentage = newProgress.coerceIn(0, 100),
            iconType = newIconType
        )
        if (updated == goal || updated.title.isBlank()) return
        viewModelScope.launch {
            repository.updateGoal(updated)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            recentlyDeleted = DeletedItem.Goal(goal)
        }
    }

    fun undoDelete() {
        val item = recentlyDeleted ?: return
        recentlyDeleted = null
        viewModelScope.launch {
            when (item) {
                is DeletedItem.Todo -> repository.restoreTodo(item.todo)
                is DeletedItem.Goal -> repository.restoreGoal(item.goal)
            }
        }
    }

    fun clearRecentlyDeleted() {
        recentlyDeleted = null
    }
}
