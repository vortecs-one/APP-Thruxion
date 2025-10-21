package com.example.qhagoapp.ui.transform
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TransformViewModel : ViewModel()
{
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
}