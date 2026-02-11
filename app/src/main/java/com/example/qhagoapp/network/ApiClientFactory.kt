package com.example.qhagoapp.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClientFactory
{
    // Shared JSON Parser
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Shared HTTP Engine (Sharing this saves battery and memory)
    private val sharedHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Creates a Retrofit instance for a specific URL/Port
     */
    fun createClient(baseUrl: String): Retrofit
    {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(sharedHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

}