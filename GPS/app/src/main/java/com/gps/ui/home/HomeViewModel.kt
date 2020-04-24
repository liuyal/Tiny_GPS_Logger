package com.gps.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


// TODO: Get data, UI Thread to pull data from main thread or Global BLE
class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home"
    }

    val text: LiveData<String> = _text






}