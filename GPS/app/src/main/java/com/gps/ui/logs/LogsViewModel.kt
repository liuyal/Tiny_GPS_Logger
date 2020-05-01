package com.gps.ui.logs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LogsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Logs Fragment"
    }
    val text: LiveData<String> = _text
}