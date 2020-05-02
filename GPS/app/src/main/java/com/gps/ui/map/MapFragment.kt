package com.gps.ui.map

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.ContextWrapper
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
import kotlinx.android.synthetic.main.fragment_map.view.*

class MapFragment : Fragment() {

    private lateinit var mapViewModel: MapViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mapViewModel = ViewModelProvider(this).get(MapViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        view?.mapView?.onCreate(savedInstanceState)
        view?.mapView?.getMapAsync(activity as MainActivity?)
        (activity as MainActivity?)?.createMaps()

        view.toggleButton.setOnClickListener {
            val main = activity as MainActivity?
            main?.statueCheckThread?.interrupt()
            main?.statueCheckThread = null
            main?.statueCheckThread = Thread(Runnable { main?.updateMaps() })
            main?.statueCheckThread!!.start()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        this.bleLinkCheck()
        view?.mapView?.onStart()
        Log.d("MAP", "Start m Fragment")
    }

    override fun onResume() {
        super.onResume()
        view?.mapView?.onResume()
        Log.d("MAP", "onResume m Fragment")
    }

    override fun onPause() {
        view?.mapView?.onResume()
        Log.d("MAP", "Pause m Fragment")
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        view?.mapView?.onStop()
        Log.d("MAP", "Stopped m Fragment")
    }

    override fun onDestroy() {
        view?.mapView?.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        view?.mapView?.onLowMemory()
    }

    private fun bleLinkCheck() {
        val main = activity as MainActivity?
        if (this.checkBLEon() && GlobalApp.BLE?.connectionState == STATE_CONNECTED && main != null) {
            if (GlobalApp.BLE == null) {
                GlobalApp.BLE = BLEDevice(main as Context, activity as ContextWrapper)
                GlobalApp.BLE!!.initialize()
            }
            main.statueCheckThread = null
            main.statueCheckThread = Thread(Runnable { main.connectionHandler() })
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
