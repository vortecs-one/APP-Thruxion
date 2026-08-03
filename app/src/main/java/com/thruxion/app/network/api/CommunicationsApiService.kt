package com.thruxion.app.network.api

import com.thruxion.app.network.model.SystemLoginRequest
import com.thruxion.app.network.model.SystemLoginResponse
import com.thruxion.app.network.model.MessageRequest
import com.thruxion.app.network.model.MessageResponse
import com.thruxion.app.network.model.PublicKeyDto
import com.thruxion.app.network.model.MediaUploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface CommunicationsApiService
{
    // API Health
    @GET("health")
    suspend fun getCommunicationHealth(): Response<Any>

    @POST("auth/system-login")
    suspend fun systemLogin(
        @Body request: SystemLoginRequest
    ): Response<SystemLoginResponse>

    // Encrypted Messaging
    @POST("messages/send")
    suspend fun sendMessage(@Body request: MessageRequest): Response<MessageResponse>

    @GET("messages")
    suspend fun getMessages(@Query("partner_id") partnerId: String): Response<List<MessageResponse>>

    // Key Exchange
    @POST("keys/upload")
    suspend fun uploadPublicKey(@Body request: PublicKeyDto): Response<Any>

    @GET("keys/{userId}")
    suspend fun getPublicKey(@Path("userId") userId: String): Response<PublicKeyDto>

    // Media Upload (Encrypted Blobs)
    @Multipart
    @POST("media/upload")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): Response<MediaUploadResponse>
}
