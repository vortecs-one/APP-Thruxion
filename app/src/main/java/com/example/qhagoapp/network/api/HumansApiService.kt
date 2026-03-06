package com.example.qhagoapp.network.api

import com.example.qhagoapp.network.model.SystemLoginRequest
import com.example.qhagoapp.network.model.SystemLoginResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body

interface HumansApiService
{
    // API HEALTH
    @GET("health")
    suspend fun getHumansHealth(): Response<Any>

    // JWT TOKEN REQUEST
    @POST("auth/system-login")
    suspend fun systemLogin(
        @Body request: SystemLoginRequest
    ): Response<SystemLoginResponse>


}