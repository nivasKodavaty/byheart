package com.gtr3.byheart.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gtr3.byheart.data.local.dao.NoteDao
import com.gtr3.byheart.data.local.entity.NoteEntity

@Database(entities = [NoteEntity::class], version = 2, exportSchema = false)
abstract class ByHeartDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
