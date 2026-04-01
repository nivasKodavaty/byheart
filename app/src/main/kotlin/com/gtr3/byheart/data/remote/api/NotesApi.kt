package com.gtr3.byheart.data.remote.api

import com.gtr3.byheart.data.remote.dto.ChatRequest
import com.gtr3.byheart.data.remote.dto.CreateNoteRequest
import com.gtr3.byheart.data.remote.dto.NoteDto
import com.gtr3.byheart.data.remote.dto.RefineSelectionRequest
import com.gtr3.byheart.data.remote.dto.RefineSelectionResponse
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

    @POST("api/notes/{id}/chat")
    suspend fun chatOnNote(@Path("id") id: Long, @Body request: ChatRequest): NoteDto

    @PATCH("api/notes/{id}/pin")
    suspend fun pinNote(@Path("id") id: Long): NoteDto

    @POST("api/notes/{id}/refine-selection")
    suspend fun refineSelection(
        @Path("id") id: Long,
        @Body request: RefineSelectionRequest
    ): RefineSelectionResponse

    @DELETE("api/notes/{id}")
    suspend fun deleteNote(@Path("id") id: Long)
}
