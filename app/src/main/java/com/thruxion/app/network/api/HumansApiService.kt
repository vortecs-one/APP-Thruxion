package com.thruxion.app.network.api

import com.thruxion.app.network.model.SystemLoginRequest
import com.thruxion.app.network.model.SystemLoginResponse
import com.thruxion.app.network.model.UserLoginRequest
import com.thruxion.app.network.model.UserLoginResponse
import com.thruxion.app.network.model.HumanResponse
import com.thruxion.app.network.model.ChangePasswordRequest
import com.thruxion.app.network.model.UpdateHumanRequest
import com.thruxion.app.network.model.HumanUpdateResponse
import com.thruxion.app.network.model.CreateHumanRequest
import com.thruxion.app.network.model.RegisterUserRequest
import com.thruxion.app.network.model.HumanCreateResponse
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
