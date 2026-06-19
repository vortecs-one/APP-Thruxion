package com.thruxion.app.network.api

import com.thruxion.app.network.model.HandoffResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface HealthyApiService {
    @POST("api/auth/app-handoff/issue")
    suspend fun issueHandoff(
        @Header("x-app-timestamp") timestamp: Long,
        @Header("x-app-signature") signature: String,
        @Header("x-app-language") language: String,
        @Body request: RequestBody
    ): Response<HandoffResponse>
}
