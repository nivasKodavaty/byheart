package com.gtr3.byheart.data.remote.api

import com.gtr3.byheart.data.remote.dto.CreateNoteRequest
import com.gtr3.byheart.data.remote.dto.NoteDto
import com.gtr3.byheart.data.remote.dto.UpdateNoteRequest
import retrofit2.http.*

interface NotesApi {

    @GET("api/notes")
    suspend fun getAllNotes(): List<NoteDto>

    @GET("api/notes/{id}")
    suspend fun getNoteById(@Path("id") id: Long): NoteDto

    @POST("api/notes")
    suspend fun createNote(@Body request: CreateNoteRequest): NoteDto

    @PUT("api/notes")
    suspend fun updateNote(@Body request: UpdateNoteRequest): NoteDto

    @DELETE("api/notes/{id}")
    suspend fun deleteNote(@Path("id") id: Long)
}
