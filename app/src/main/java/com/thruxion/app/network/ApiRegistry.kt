package com.thruxion.app.network

import com.thruxion.app.network.api.CommunicationsApiService
import com.thruxion.app.network.api.HealthyApiService
import com.thruxion.app.network.api.HumansApiService
import com.thruxion.app.network.api.HuaweiHealthApi
import com.thruxion.app.network.security.ApiType
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object ApiRegistry
{
    private const val BASE_DOMAIN = "https://thruxion.com/api"
    private const val HUMANS = "$BASE_DOMAIN/humans/"
    private const val HUAWEI_HEALTH_BASE_URL = "https://health-api.cloud.huawei.com/healthkit/v1/"

  //private const val MACHINES = "$BASE_DOMAIN/machines/"
    private const val COMMUNICATIONS = "$BASE_DOMAIN/communications/"
    
    const val HEALTHY_BASE_URL = "https://web-nutrition.vercel.app/"
    const val HEALTHY_LOGIN_URL = "${HEALTHY_BASE_URL}login"
    const val HEALTHY_HANDOFF_SECRET = "thruxion"

    const val FLECHA_URL = "https://qapta-odoo-odoov19.odoo.com/web"



    val humansApi: HumansApiService by lazy {
        ApiClientFactory.create(HUMANS, ApiType.HUMANS)
            .create(HumansApiService::class.java)
    }

    /*val machinesApi: MachinesApiService by lazy {
            ApiClientFactory.create(MACHINES,ApiType.MACHINES)
                .create(MachinesApiService::class.java)
    }*/

    val communicationsApi: CommunicationsApiService by lazy {
        ApiClientFactory.create(COMMUNICATIONS, ApiType.COMMUNICATIONS)
            .create(CommunicationsApiService::class.java)
    }

    val healthyApi: HealthyApiService by lazy {
        ApiClientFactory.create(HEALTHY_BASE_URL, ApiType.HUMANS) // Using HUMANS for default config
            .create(HealthyApiService::class.java)
    }

    val huaweiHealthApi: HuaweiHealthApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()

        Retrofit.Builder()
            .baseUrl(HUAWEI_HEALTH_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()
            .create(HuaweiHealthApi::class.java)
    }


}