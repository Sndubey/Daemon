package com.shubham.myhub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val categoryIcon: String = "IDEAS", // IDEAS, SHOPPING, MEETING, PERSONAL, BOOKS, THOUGHTS
    val isLocked: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    /** Hidden notes only appear after long-pressing the "+" button. Added in DB v6. */
    @ColumnInfo(defaultValue = "0")
    val isHidden: Boolean = false,
    /** Pinned notes are listed first. Added in DB v7. */
    @ColumnInfo(defaultValue = "0")
    val isPinned: Boolean = false
)
