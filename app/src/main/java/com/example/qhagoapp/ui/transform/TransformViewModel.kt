package com.example.qhagoapp.ui.transform
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qhagoapp.network.ApiRegistry
import kotlinx.coroutines.launch

class TransformViewModel : ViewModel()
{
    private val _healthStatus = MutableLiveData<String>()
    val healthStatus: LiveData<String> = _healthStatus

    private val _texts = MutableLiveData<List<String>>().apply {
        // Create a mutable list with your custom first line
        val items = mutableListOf("S.O.S","QHago?","MyWitness")
        // Create the list from the loop
        val loopedItems = (1..7).mapIndexed { _, i ->
            "Lawyer # $i"
        }
        // Add the looped items to your list
        items.addAll(loopedItems)
        // Set the final combined list to the LiveData
        value = items
    }

    val texts: LiveData<List<String>> = _texts

    init {
        checkApis() // Automatically runs when the fragment/viewmodel is created
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