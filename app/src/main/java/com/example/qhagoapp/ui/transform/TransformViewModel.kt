package com.example.qhagoapp.ui.transform
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qhagoapp.network.ApiRegistry
import kotlinx.coroutines.launch
import kotlin.random.Random

// Data model for map
data class MapUser(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val lat: Double,
    val lng: Double
)

class TransformViewModel : ViewModel()
{
    private val _healthStatus = MutableLiveData<String>()
    val healthStatus: LiveData<String> = _healthStatus
    // Security: private MutableLiveData and public LiveData
    private val _users = MutableLiveData<List<MapUser>>()
    val users: LiveData<List<MapUser>> = _users

    init {
        generateMockUsers() // replaces old "texts"
        checkApis() // Automatically runs when the fragment/viewmodel is created
    }

    /**
     * 🔥 Generates mock users near a base location
     * (Later you replace this with backend API)
     */
    private fun generateMockUsers()
    {
        val baseLat = 48.8583
        val baseLng = 2.2944
        val names = mutableListOf("S.O.S", "QHago?", "MyWitness")
        names.addAll((1..7).map { "Lawyer # $it" })
        val users = names.map { name ->
            val latOffset = (Random.nextDouble() - 0.5) * 0.04
            val lngOffset = (Random.nextDouble() - 0.5) * 0.04
            MapUser(
                name = name,
                lat = baseLat + latOffset,
                lng = baseLng + lngOffset
            )
        }
        _users.value = users
    }

    fun updateUsersAroundLocation(baseLat: Double, baseLng: Double) {
        val names = listOf("S.O.S", "QHago?", "MyWitness") + (1..10).map { "Lawyer #$it" }

        val updated = names.map { name ->
            // 0.02 offset is roughly 2km radius
            val latOffset = (Random.nextDouble() - 0.5) * 0.04
            val lngOffset = (Random.nextDouble() - 0.5) * 0.04
            MapUser(
                name = name,
                lat = baseLat + latOffset,
                lng = baseLng + lngOffset
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
                val commsRes = ApiRegistry.communicationsApi.getCommunicationHealth()
                // Test Humans API
                val humansRes = ApiRegistry.humansApi.getHumansHealth()
                // isSuccessful checks for codes 200-299
                if (commsRes.isSuccessful && humansRes.isSuccessful)
                {
                    _healthStatus.postValue("All API Systems Online")
                    Log.d("NetworkTest", "Comms: OK, Humans: OK")
                }
                else
                {
                    val commsStatus = if(commsRes.isSuccessful) "OK" else "Error ${commsRes.code()}"
                    val humansStatus = if(humansRes.isSuccessful) "OK" else "Error ${humansRes.code()}"
                    _healthStatus.postValue("Comms: $commsStatus | Humans: $humansStatus")
                    Log.e("NetworkTest", "Comms: $commsStatus, Humans: $humansStatus")
                }
            }
            catch (e: Exception)
            {
                _healthStatus.postValue("Connection Error: ${e.localizedMessage}")
                Log.e("NetworkTest", "Exception: ", e)
            }
        }
    }

}
