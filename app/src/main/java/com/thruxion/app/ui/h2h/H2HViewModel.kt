package com.thruxion.app.ui.h2h

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class H2HViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is reflow Fragment"
    }
    val text: LiveData<String> = _text
}