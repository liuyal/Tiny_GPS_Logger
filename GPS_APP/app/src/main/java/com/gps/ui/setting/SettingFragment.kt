package com.gps.ui.setting

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.gps.R

class SettingFragment : Fragment() {

    private lateinit var settingViewModel: SettingViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        settingViewModel = ViewModelProvider(this).get(SettingViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_setting, container, false)





        return root
    }

    override fun onStart() {
        super.onStart()
        Log.e("Setting", "Start map Fragment")
    }

    override fun onStop() {
        super.onStop()
        Log.e("Setting", "Stopped map Fragment")
    }


}
