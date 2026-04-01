package com.gtr3.byheart.di

import android.content.Context
import androidx.room.Room
import com.gtr3.byheart.data.local.db.ByHeartDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ByHeartDatabase =
        Room.databaseBuilder(context, ByHeartDatabase::class.java, "byheart_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideNoteDao(db: ByHeartDatabase) = db.noteDao()
}
