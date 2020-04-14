package com.example.gps.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.gps.R
import com.example.gps.objects.GlobalApplication
import kotlinx.android.synthetic.main.fragment_home.view.*
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = view.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer { textView.text = it })

        view.test_button.setOnClickListener { View ->
            Log.d("", "Clicked button")
            Test()
        }

        view.test_button2.setOnClickListener { View ->
            Log.d("", "Clicked button 2")
            GlobalApplication.BLE!!.disconnect()
        }

        return view
    }


    private fun Test() {

        GlobalApplication.BLE?.connect("CC:50:E3:9C:5B:A6")

        var x = GlobalApplication.BLE

        println()
    }

}
