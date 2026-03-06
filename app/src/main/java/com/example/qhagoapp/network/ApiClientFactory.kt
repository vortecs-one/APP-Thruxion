package com.example.qhagoapp.network

import com.example.qhagoapp.network.security.ApiType
import com.example.qhagoapp.network.security.AuthInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClientFactory
{
    fun create(baseUrl: String, apiType: ApiType): Retrofit {

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(apiType))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}