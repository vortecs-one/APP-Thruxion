package com.thruxion.app.network.security

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val apiType: ApiType) : Interceptor
{
    override fun intercept(chain: Interceptor.Chain): Response
    {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        val token = when(apiType) {
            ApiType.COMMUNICATIONS -> TokenManager.getCommunicationsToken()
            ApiType.HUMANS -> TokenManager.getHumansToken()
        }
        if(token != null)
            requestBuilder.addHeader("Authorization", "Bearer $token")
        return chain.proceed(requestBuilder.build())
    }

}