package com.example.gps.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.gps.R
import com.example.gps.objects.GlobalApplication

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer { textView.text = it })

        return root
    }


    fun Test(){

        GlobalApplication.BLE?.connect("CC:50:E3:9C:5B:A6")

        var a = GlobalApplication.BLE!!.bleGATT?.discoverServices()
        var b = GlobalApplication.BLE!!.bleGATT?.services
        var c = GlobalApplication.BLE!!.bleGATT

        // GlobalApplication.BLE?.bleGATT?.writeCharacteristic(characteristic)

        println("")
    }

}
