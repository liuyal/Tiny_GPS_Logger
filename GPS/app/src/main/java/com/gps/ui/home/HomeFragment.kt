package com.gps.ui.home

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.gps.MainActivity
import com.gps.R
import com.gps.objects.BLEDevice
import com.gps.objects.GlobalApp
import com.gps.objects.STATE_CONNECTED
import kotlinx.android.synthetic.main.fragment_home.view.*

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    // TODO: Make persistent
    private var autoCheckRunning: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        view.action_button_A.setOnClickListener {
            val main = activity as MainActivity?
            if (main != null && main.checkBTon() && !autoCheckRunning) {
                autoCheckRunning = true
                if (main.statueCheckThread != null && main.statueCheckThread?.isAlive!!) main.statueCheckThread?.interrupt()
                main.statueCheckThread = null
                main.statueCheckThread = Thread(Runnable { main.checkTaskLoop() })
                main.statueCheckThread!!.start()
            } else if (main != null && main.checkBTon() && main.statueCheckThread?.isAlive!!) {
                autoCheckRunning = false
                main.statueCheckThread?.interrupt()
                main.statueCheckThread = null
                view.action_button_A.setColorFilter(Color.WHITE)
            }
        }

        view.action_button_B.setOnClickListener {
            val main = activity as MainActivity?
            if (main != null && main.checkBTon() && GlobalApp.BLE?.connectionState == STATE_CONNECTED) {
                if (main.statueCheckThread != null && main.statueCheckThread?.isAlive!!) main.statueCheckThread?.interrupt()
                main.statueCheckThread = null
                main.disconnectionUIHandler()
            } else if (main != null && main.checkBTon()) {
                if (GlobalApp.BLE == null) {
                    GlobalApp.BLE = BLEDevice(main as Context, activity as ContextWrapper)
                    GlobalApp.BLE!!.initialize()
                }
                if (main.statueCheckThread != null && main.statueCheckThread?.isAlive!!) main.statueCheckThread?.interrupt()
                main.statueCheckThread = null
                main.statueCheckThread = Thread(Runnable { main.connectionUIHandler() })
                main.statueCheckThread!!.start()
            }
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        Log.d("HOME", "Start h Fragment")
    }

    override fun onResume() {
        super.onResume()
        bleLinkCheck()
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

    private fun bleLinkCheck() {
        val main = activity as MainActivity?
        if (main != null && main.checkBTon() && GlobalApp.BLE?.connectionState == STATE_CONNECTED) {
            if (GlobalApp.BLE == null) {
                GlobalApp.BLE = BLEDevice(main as Context, activity as ContextWrapper)
                GlobalApp.BLE!!.initialize()
            }
            if (main.statueCheckThread != null && main.statueCheckThread?.isAlive!!) main.statueCheckThread?.interrupt()
            main.statueCheckThread = null
            main.statueCheckThread = Thread(Runnable { main.connectionUIHandler() })
            main.statueCheckThread!!.start()
        }
    }
}
