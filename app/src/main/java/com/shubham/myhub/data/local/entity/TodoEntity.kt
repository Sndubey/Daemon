package com.shubham.myhub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val timeStr: String = "",
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    /** User-chosen position (drag to reorder); lower comes first. Added in DB v5. */
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0
)
