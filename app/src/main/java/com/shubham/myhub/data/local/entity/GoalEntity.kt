package com.shubham.myhub.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val progressPercentage: Int = 0, // 0 to 100
    val iconType: String = "LAPTOP", // LAPTOP, CHART, BOOK, FITNESS, GENERAL
    val createdAtMillis: Long = System.currentTimeMillis()
)
