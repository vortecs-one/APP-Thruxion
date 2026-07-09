package com.thruxion.app.ui.thruxion
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thruxion.app.network.ApiRegistry
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

// Data model for map
data class MapUser(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val lat: Double,
    val lng: Double,
    val avatarIndex: Int
)

data class SearchResult(
    val id: String,
    val displayName: String,
    val shortName: String,
    val lat: Double,
    val lon: Double,
    val type: String?,
    val importance: Double?,
    val country: String?,
    val city: String?
)

class TransformViewModel : ViewModel()
{
    // Security: private MutableLiveData and public LiveData
    private val _users = MutableLiveData<List<MapUser>>()
    val users: LiveData<List<MapUser>> = _users

    init
    {
        generateMockUsers() // replaces old "texts"
        checkApis() // Automatically runs when the fragment/viewmodel is created
    }

    private fun generateMockUsers()
    {
        val baseLat = 48.8583
        val baseLng = 2.2944
        val names = (1..10).map { "Lawyer # $it" }
        val users = names.mapIndexed { index, name ->
            val latOffset = (Random.nextDouble() - 0.5) * 0.04
            val lngOffset = (Random.nextDouble() - 0.5) * 0.04
            MapUser(
                name = name,
                lat = baseLat + latOffset,
                lng = baseLng + lngOffset,
                avatarIndex = index % 16
            )
        }
        _users.value = users
    }

    fun updateUsersAroundLocation(baseLat: Double, baseLng: Double) {
        val names = (1..10).map { "Lawyer #$it" }

        val updated = names.mapIndexed { index, name ->
            // 0.02 offset is roughly 2km radius
            val latOffset = (Random.nextDouble() - 0.5) * 0.04
            val lngOffset = (Random.nextDouble() - 0.5) * 0.04
            MapUser(
                name = name,
                lat = baseLat + latOffset,
                lng = baseLng + lngOffset,
                avatarIndex = index % 16
            )
        }
        _users.value = updated
    }

    fun checkApis()
    {
        viewModelScope.launch {
            try
            {
                // Test Communications API
                ApiRegistry.communicationsApi.getCommunicationHealth()
                // Test Humans API
                ApiRegistry.humansApi.getHumansHealth()
                // isSuccessful checks for codes 200-299
                Log.d("NetworkTest", "API Health check performed")
            }
            catch (e: Exception)
            {
                Log.e("NetworkTest", "Exception: ", e)
            }
        }
    }


}