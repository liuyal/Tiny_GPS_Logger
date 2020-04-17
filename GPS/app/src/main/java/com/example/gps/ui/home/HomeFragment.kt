package com.example.gps.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.gps.MainActivity
import com.example.gps.R
import kotlinx.android.synthetic.main.fragment_home.view.*


// TODO Implement UI
class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = view.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer { textView.text = it })

        view.test_button.setOnClickListener { View ->
            Log.d("", "Clicked S button")
            val main = activity as MainActivity?
            main?.thread?.interrupt()
            main?.thread = null
        }

        view.test_button2.setOnClickListener { View ->
            Log.d("", "Clicked C button")
            val main = activity as MainActivity?
            main?.thread = Thread(Runnable { main?.backgroundTask() })
            main?.thread?.start()
        }

        return view
    }


    // TODO: modify UI to indicate no matching device
    // UI Thread
    private fun disconnectionHandler() {
//        Toast.makeText(applicationContext, "Unable to connect to Default BLE Device!", Toast.LENGTH_SHORT).show()
        Log.e("", "Unable to connect to BLE Device")
    }


}
