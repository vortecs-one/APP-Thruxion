package com.example.qhagoapp.network.api

import com.example.qhagoapp.network.model.SystemLoginRequest
import com.example.qhagoapp.network.model.SystemLoginResponse
import com.example.qhagoapp.network.model.UserLoginRequest
import com.example.qhagoapp.network.model.UserLoginResponse
import com.example.qhagoapp.network.model.HumanResponse
import retrofit2.Response
import retrofit2.http.*

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

    @POST("user/login")
    suspend fun userLogin(
        @Body request: UserLoginRequest
    ): Response<UserLoginResponse>

    // GET HUMAN BY ID
    @GET("human/{id}")
    suspend fun getHumanById(
        @Path("id") id: Int
    ): Response<HumanResponse>
}