package com.example.gps.ui.logs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LogsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Logs Fragment"
    }
    val text: LiveData<String> = _text
}