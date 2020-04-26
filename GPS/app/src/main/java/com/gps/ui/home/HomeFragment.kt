package com.gps.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.gps.MainActivity
import com.gps.R
import kotlinx.android.synthetic.main.fragment_home.view.*


// TODO Implement UI
class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_home, container, false)


        //TODO: Add refresh functionality
        view.floatingActionButtonA.setOnClickListener { _ ->
            Log.d("HOME", "Refresh")
        }

        //TODO: Add relink functionality
        view.floatingActionButtonB.setOnClickListener { _ ->
            Log.d("HOME", "Relink")
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

    override fun onResume() {
        super.onResume()
        Log.d("HOME", "onResume h Fragment")
    }

    override fun onPause() {
        super.onPause()
        val main = activity as MainActivity?
        if (main?.statueCheckThread != null) {
            main.statueCheckThread?.interrupt()
            main.statueCheckThread = null
        }
        Log.d("HOME", "Pause h Fragment")
    }

    override fun onStop() {
        super.onStop()

        Log.d("HOME", "Stopped h Fragment")
    }





}
