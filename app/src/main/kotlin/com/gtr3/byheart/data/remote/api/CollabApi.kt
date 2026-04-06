package com.gtr3.byheart.data.remote.api

import com.gtr3.byheart.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface CollabApi {

    @GET("api/collab")
    suspend fun getAllCollabNotes(): List<CollabNoteDto>

    @GET("api/collab/{shareCode}")
    suspend fun getCollabNote(@Path("shareCode") shareCode: String): CollabNoteDto

    @POST("api/collab")
    suspend fun createCollabNote(@Body request: CreateCollabNoteRequest): CollabNoteDto

    @POST("api/collab/join")
    suspend fun joinCollabNote(@Body request: JoinCollabNoteRequest): CollabNoteDto

    /** Returns Response<CollabNoteDto> so the caller can inspect the 409 body on conflict */
    @PUT("api/collab/{shareCode}")
    suspend fun updateCollabNote(
        @Path("shareCode") shareCode: String,
        @Body request: UpdateCollabNoteRequest
    ): Response<CollabNoteDto>

    @POST("api/collab/{shareCode}/chat")
    suspend fun chatOnCollabNote(
        @Path("shareCode") shareCode: String,
        @Body request: CollabChatRequest
    ): CollabNoteDto

    @POST("api/collab/{shareCode}/refine-selection")
    suspend fun refineSelection(
        @Path("shareCode") shareCode: String,
        @Body request: CollabRefineSelectionRequest
    ): RefineSelectionResponse

    @GET("api/collab/{shareCode}/participants")
    suspend fun getParticipants(@Path("shareCode") shareCode: String): List<ParticipantDto>

    @DELETE("api/collab/{shareCode}/leave")
    suspend fun leaveCollabNote(@Path("shareCode") shareCode: String)
}
