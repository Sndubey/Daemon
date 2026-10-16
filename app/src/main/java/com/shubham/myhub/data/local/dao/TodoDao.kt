package com.shubham.myhub.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    // Unticked first, then the user's drag order. Shared by the app and the home-screen widget.
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, sortOrder ASC, createdAtMillis DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT COALESCE(MIN(sortOrder), 0) FROM todos")
    suspend fun minSortOrder(): Int

    @Query("UPDATE todos SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int): Int

    /** Saves a whole new order in one transaction: position in the list becomes sortOrder. */
    @Transaction
    suspend fun updateSortOrders(idsInOrder: List<Long>): Int {
        idsInOrder.forEachIndexed { index, id -> updateSortOrder(id, index) }
        return idsInOrder.size
    }

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0")
    fun getRemainingCount(): Flow<Int>

    // Flipped in SQL so rapid taps never write back a stale copy of the row.
    @Query("UPDATE todos SET isCompleted = NOT isCompleted WHERE id = :id")
    suspend fun toggleCompleted(id: Long): Int

    @Query("UPDATE todos SET text = :text WHERE id = :id")
    suspend fun updateText(id: Long, text: String): Int

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
