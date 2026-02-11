package com.example.qhagoapp.network
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

interface CommunicationsApiService
{
    // API Health
    @GET("health")
    suspend fun getCommunicationHealth(): Response<Any>
}