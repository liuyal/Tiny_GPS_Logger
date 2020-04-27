package com.gps.ui.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.gps.MainActivity
import com.gps.R
import com.gps.objects.GlobalApp
import com.gps.objects.STATE_CONNECTED
import kotlinx.android.synthetic.main.fragment_home.view.*


// TODO Implement UI
// TODO: Add auto refresh, disconnect/reconnect functionality
class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var mainRunning: Boolean = false
    private var isConnected: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        view.action_button_A.setOnClickListener {
            val main = activity as MainActivity?
            if (!mainRunning) {
                main?.statueCheckThread = null
                main?.statueCheckThread = Thread(Runnable { main?.checkTaskLoop() })
                main?.statueCheckThread!!.start()
                mainRunning = true
                view.action_button_A.setColorFilter(Color.GREEN)
                view.action_button_B.setColorFilter(ResourcesCompat.getColor(resources, R.color.DodgerBlue, null))
            } else {
                main?.statueCheckThread?.interrupt()
                main?.statueCheckThread = null
                mainRunning = false
                view.action_button_A.setColorFilter(Color.WHITE)
            }
        }

        view.action_button_B.setOnClickListener {
            val bleConnection = GlobalApp.BLE?.connectionState == STATE_CONNECTED
            this.isConnected = bleConnection
            val main = activity as MainActivity?
            if (this.isConnected && main != null) {
                if (main.statueCheckThread != null) {
                    main.statueCheckThread?.interrupt()
                    main.statueCheckThread = null
                }
                GlobalApp.BLE?.disconnect()
                main.disconnectionUIHandler()
                view.action_button_B.setColorFilter(Color.WHITE)
                view.action_button_A.setColorFilter(Color.WHITE)
            } else {
                main?.statueCheckThread = null
                main?.statueCheckThread = Thread(Runnable { main?.connectionUIHandler() })
                main?.statueCheckThread!!.start()
                view.action_button_B.setColorFilter(ResourcesCompat.getColor(resources, R.color.DodgerBlue, null))
            }
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        val bleConnection = GlobalApp.BLE?.connectionState == STATE_CONNECTED
        this.isConnected = bleConnection
        val main = activity as MainActivity?
        if (isConnected && main != null) {
            main.statueCheckThread = null
            main.statueCheckThread = Thread(Runnable { main.connectionUIHandler() })
            main.statueCheckThread!!.start()
            view?.action_button_B?.setColorFilter(ResourcesCompat.getColor(resources, R.color.DodgerBlue, null))
        }

        Log.d("HOME", "Start h Fragment")
    }

    override fun onResume() {
        super.onResume()
        Log.d("HOME", "onResume h Fragment")
    }

    override fun onPause() {
        super.onPause()

        Log.d("HOME", "Pause h Fragment")
    }

    override fun onStop() {
        super.onStop()
        Log.d("HOME", "Stopped h Fragment")
    }

}
