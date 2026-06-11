package com.example.qhagoapp.network.api

import com.example.qhagoapp.network.model.SystemLoginRequest
import com.example.qhagoapp.network.model.SystemLoginResponse
import com.example.qhagoapp.network.model.UserLoginRequest
import com.example.qhagoapp.network.model.UserLoginResponse
import com.example.qhagoapp.network.model.HumanResponse
import com.example.qhagoapp.network.model.ChangePasswordRequest
import com.example.qhagoapp.network.model.UpdateHumanRequest
import com.example.qhagoapp.network.model.HumanUpdateResponse
import com.example.qhagoapp.network.model.CreateHumanRequest
import com.example.qhagoapp.network.model.RegisterUserRequest
import com.example.qhagoapp.network.model.HumanCreateResponse
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

    // APP LOGIN
    @POST("user/login")
    suspend fun userLogin(
        @Body request: UserLoginRequest
    ): Response<UserLoginResponse>

    // REGISTER USER
    @POST("user/register")
    suspend fun registerUser(
        @Body request: RegisterUserRequest
    ): Response<UserLoginResponse>

    // CHANGE PASSWORD
    @POST("user/{id}/change-password")
    suspend fun changePassword(
        @Path("id") id: Int,
        @Body request: ChangePasswordRequest
    ): Response<Any>

    // GET HUMAN BY ID
    @GET("human/{id}")
    suspend fun getHumanById(
        @Path("id") id: Int
    ): Response<HumanResponse>

    // CREATE HUMAN
    @POST("human/")
    suspend fun createHuman(
        @Body request: CreateHumanRequest
    ): Response<HumanCreateResponse>

    // UPDATE HUMAN
    @PUT("human/{id}")
    suspend fun updateHuman(
        @Path("id") id: Int,
        @Body request: UpdateHumanRequest
    ): Response<HumanUpdateResponse>
}
