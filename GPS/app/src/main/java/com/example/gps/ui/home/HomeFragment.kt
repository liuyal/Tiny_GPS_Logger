package com.example.gps.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
            Log.d("HOME", "Clicked S button")
        }

        view.test_button2.setOnClickListener { View ->
            Log.d("HOME", "Clicked C button")
        }

        return view
    }


    override fun onStart() {
        super.onStart()
        val main = activity as MainActivity?
        main?.statueCheckThread = null
        main?.statueCheckThread = Thread(Runnable { main?.checkTask() })
        main?.statueCheckThread!!.start()
        Log.d("HOME", "Start h Fragment")
    }


    override fun onStop() {
        super.onStop()
        val main = activity as MainActivity?
        if (main?.statueCheckThread != null) {
            main.statueCheckThread?.interrupt()
            main.statueCheckThread = null
        }
        Log.d("HOME", "Stopped h Fragment")
    }
}
