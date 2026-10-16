package com.shubham.myhub.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shubham.myhub.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoEntity): Int

    @Delete
    suspend fun deleteTodo(todo: TodoEntity): Int

    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, createdAtMillis DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0")
    fun getRemainingCount(): Flow<Int>

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
