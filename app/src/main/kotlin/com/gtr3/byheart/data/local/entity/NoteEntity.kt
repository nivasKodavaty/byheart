package com.gtr3.byheart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val content: String?,
    val updatedAt: String,
    val isPinned: Boolean = false,
    val folderName: String? = null
)
