package com.thruxion.app.network

import com.thruxion.app.network.api.CommunicationsApiService
import com.thruxion.app.network.api.HumansApiService
import com.thruxion.app.network.security.ApiType

object ApiRegistry
{
    private const val BASE_DOMAIN = "https://thruxion.com/api"
    private const val HUMANS = "$BASE_DOMAIN/humans/"

  //private const val MACHINES = "$BASE_DOMAIN/machines/"
    private const val COMMUNICATIONS = "$BASE_DOMAIN/communications/"

    val communicationsApi: CommunicationsApiService by lazy {
        ApiClientFactory.create(COMMUNICATIONS, ApiType.COMMUNICATIONS)
            .create(CommunicationsApiService::class.java)
    }

    val humansApi: HumansApiService by lazy {
        ApiClientFactory.create(HUMANS, ApiType.HUMANS)
            .create(HumansApiService::class.java)
    }

    /*val machinesApi: MachinesApiService by lazy {
        ApiClientFactory.create(MACHINES,ApiType.MACHINES)
            .create(MachinesApiService::class.java)
    }*/



}