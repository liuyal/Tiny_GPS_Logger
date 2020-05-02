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
    private var autoCheck: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        view.action_button_A.setOnClickListener {
            val main = activity as MainActivity?
            val checkBLEon = checkBLEon()
            if (checkBLEon && !autoCheck) {
                autoCheck = true
                main?.statueCheckThread = null
                main?.statueCheckThread = Thread(Runnable { main?.checkTaskLoop() })
                main?.statueCheckThread!!.start()
            } else if (checkBLEon) {
                autoCheck = false
                main?.statueCheckThread?.interrupt()
                main?.statueCheckThread = null
                view.action_button_A.setColorFilter(Color.WHITE)
            }
        }

        view.action_button_B.setOnClickListener {
            val isConnected = GlobalApp.BLE?.connectionState == STATE_CONNECTED
            val main = activity as MainActivity?
            val checkBLEon = checkBLEon()
            if (checkBLEon && isConnected && main != null) {
                main.statueCheckThread?.interrupt()
                main.statueCheckThread = null
                main.disconnectionUIHandler()
            } else if (checkBLEon) {
                if (GlobalApp.BLE == null) {
                    GlobalApp.BLE = BLEDevice(main as Context, activity as ContextWrapper)
                    GlobalApp.BLE!!.initialize()
                }
                main?.statueCheckThread = null
                main?.statueCheckThread = Thread(Runnable { main?.connectionUIHandler() })
                main?.statueCheckThread!!.start()
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
        if (checkBLEon() &&  GlobalApp.BLE?.connectionState == STATE_CONNECTED && main != null) {
            if (GlobalApp.BLE == null) {
                GlobalApp.BLE = BLEDevice(main as Context, activity as ContextWrapper)
                GlobalApp.BLE!!.initialize()
            }
            main.statueCheckThread = null
            main.statueCheckThread = Thread(Runnable { main.connectionUIHandler() })
            main.statueCheckThread!!.start()
        }
    }

    private fun checkBLEon(): Boolean {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Toast.makeText(activity, "BlueTooth is not supported!", Toast.LENGTH_SHORT).show()
        } else if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            Toast.makeText(activity, "BlueTooth is not enabled!", Toast.LENGTH_SHORT).show()
        } else if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
            return true
        }
        return false
    }
}
