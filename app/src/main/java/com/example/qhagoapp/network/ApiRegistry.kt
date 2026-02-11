package com.example.qhagoapp.network

object ApiRegistry
{
    private const val BASE_DOMAIN = "https://thruxion.com/api"

    // API 1: Communications
    val communicationsApi: CommunicationsApiService by lazy {
        ApiClientFactory.createClient("$BASE_DOMAIN/communications/")
            .create(CommunicationsApiService::class.java)
    }

    // API 2: Humans
    val humansApi: HumansApiService by lazy {
        ApiClientFactory.createClient("$BASE_DOMAIN/humans/")
            .create(HumansApiService::class.java)
    }

}