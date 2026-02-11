package com.example.qhagoapp.network
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

interface HumansApiService
{
    // API Health
    @GET("health")
    suspend fun getHumansHealth(): Response<Any>
}