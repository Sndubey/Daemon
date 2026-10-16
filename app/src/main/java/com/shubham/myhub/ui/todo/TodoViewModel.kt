package com.shubham.myhub.ui.todo

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

class TodoViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    val todos: StateFlow<List<TodoEntity>> = repository.allTodos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val remainingCount: StateFlow<Int> = repository.remainingTodosCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val goals: StateFlow<List<GoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good Morning,"
            in 12..16 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
    }

    fun addTodo(text: String, timeStr: String) {
        viewModelScope.launch {
            repository.addTodo(text, timeStr)
        }
    }

    fun toggleTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.toggleTodoCompleted(todo)
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
        }
    }

    fun addGoal(title: String, description: String, progress: Int, iconType: String) {
        viewModelScope.launch {
            repository.addGoal(title, description, progress, iconType)
        }
    }

    fun updateGoal(goal: GoalEntity, newTitle: String, newDesc: String, newProgress: Int) {
        viewModelScope.launch {
            val updated = goal.copy(
                title = newTitle.trim(),
                description = newDesc.trim(),
                progressPercentage = newProgress.coerceIn(0, 100)
            )
            repository.updateGoal(updated)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }
}
