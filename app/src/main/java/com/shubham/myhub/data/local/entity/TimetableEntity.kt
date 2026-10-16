package com.shubham.myhub.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "timetables")
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/** One task on one weekday of a timetable. Deleted together with its timetable. */
@Entity(
    tableName = "timetable_entries",
    foreignKeys = [
        ForeignKey(
            entity = TimetableEntity::class,
            parentColumns = ["id"],
            childColumns = ["timetableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("timetableId")]
)
data class TimetableEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timetableId: Long,
    /** 0 = Sunday … 6 = Saturday. */
    val dayOfWeek: Int,
    val text: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class TimetableWithEntries(
    @Embedded val timetable: TimetableEntity,
    @Relation(parentColumn = "id", entityColumn = "timetableId")
    val entries: List<TimetableEntryEntity>
)
