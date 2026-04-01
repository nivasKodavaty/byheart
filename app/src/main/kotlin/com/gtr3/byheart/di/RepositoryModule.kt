package com.gtr3.byheart.di

import com.gtr3.byheart.data.repository.AuthRepositoryImpl
import com.gtr3.byheart.data.repository.NotesRepositoryImpl
import com.gtr3.byheart.domain.repository.AuthRepository
import com.gtr3.byheart.domain.repository.NotesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NotesRepository
}
