package com.gps.ui.map

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.gps.MainActivity
import com.gps.R
import com.gps.objects.*
import kotlinx.android.synthetic.main.fragment_map.view.*
import kotlinx.android.synthetic.main.fragment_map.view.statusBar1
import kotlinx.android.synthetic.main.fragment_map.view.statusBar2
import kotlinx.android.synthetic.main.fragment_map.view.statusBar3
import kotlinx.android.synthetic.main.fragment_map.view.statusBar4
import kotlinx.android.synthetic.main.fragment_map.view.statusBar5
import kotlinx.android.synthetic.main.fragment_map.view.statusBar6

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapViewModel: MapViewModel

    private var mapCheckThread: Thread? = null

    private lateinit var googleMap: GoogleMap
    private lateinit var markerOptions: MarkerOptions
    private lateinit var marker: Marker
    private lateinit var cameraPosition: CameraPosition
    private var defaultLatitude = 49.279793
    private var defaultLongitude = -123.115669

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mapViewModel = ViewModelProvider(this).get(MapViewModel::class.java)
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        view?.mapView?.onCreate(savedInstanceState)
        view?.mapView?.getMapAsync(this)
        this.createMaps()

        view.toggleButton.setOnClickListener {
            if (this.mapCheckThread != null && this.mapCheckThread!!.isAlive) this.mapCheckThread?.interrupt()
            this.mapCheckThread = null
            this.mapCheckThread = Thread(Runnable { this.updateMaps() })
            this.mapCheckThread!!.start()
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
        if (this.mapCheckThread != null && this.mapCheckThread?.isAlive!!) this.mapCheckThread?.interrupt()
        this.mapCheckThread = null
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

    override fun onMapReady(googleMap: GoogleMap?) {
        this.googleMap = googleMap!!
        this.googleMap.uiSettings.isZoomControlsEnabled = true
        this.googleMap.uiSettings.isCompassEnabled = true
        this.googleMap.uiSettings.isMapToolbarEnabled = true
        this.googleMap.uiSettings.isScrollGesturesEnabled = true
        this.googleMap.uiSettings.isTiltGesturesEnabled = true
        this.googleMap.uiSettings.isRotateGesturesEnabled = true
        this.marker = googleMap.addMarker(this.markerOptions)
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(this.cameraPosition))
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    private fun createMaps() {
        this.markerOptions = MarkerOptions()
        val bitmapDescriptor = bitmapDescriptorFromVector(activity as Context, R.drawable.ic_map_marker_24dp)
        this.markerOptions.icon(bitmapDescriptor)
        this.markerOptions.position(LatLng(defaultLatitude, defaultLongitude))
        this.cameraPosition = CameraPosition.Builder().target(LatLng(defaultLatitude, defaultLongitude)).zoom(10f).build()
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    // TODO: add progress bar to lock out inputs
    private fun updateMaps() {
        try {
            this.connectionHandler()
            if (GlobalApp.BLE?.gpsStatusFlags?.get(GPS_FIX_FLAG_INDEX)!!) GlobalApp.BLE?.fetchGPSData()
            else GlobalApp.BLE?.gpsData = ""
            if (GlobalApp.BLE?.gpsData != null && GlobalApp.BLE?.gpsData != "") {
                var gpsData = GlobalApp.BLE?.gpsData!!
                gpsData = gpsData.substring(gpsData.indexOf('[') + 1, gpsData.indexOf(']'))
                val gpdDataList = gpsData.split(',') as ArrayList<String>
                this.defaultLatitude = gpdDataList[LOCATION_LAT_INDEX].toDouble()
                this.defaultLongitude = gpdDataList[LOCATION_LNG_INDEX].toDouble()
                activity?.runOnUiThread {
                    marker.position = LatLng(defaultLatitude, defaultLongitude)
                    cameraPosition = CameraPosition.Builder().target(LatLng(defaultLatitude, defaultLongitude)).zoom(15f).build()
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
            } else Log.d("MAP", "NO GPS FIX")
            Log.d("MAP", "MAPS UPDATE")
        } catch (e: Throwable) {
            Log.e("MAP", "Update Maps Error")
        }
    }

    // TODO: Lock out inputs
    private fun connectionHandler() {
        try {
            Log.d("MAP", "Connection Handler Starting")
            if (GlobalApp.BLE?.bleAddress == null || GlobalApp.BLE?.bleAddress == "") {
                activity?.runOnUiThread { Toast.makeText(activity, "No Device Paired", Toast.LENGTH_SHORT).show() }
                return
            }
            var connected = false
            val start = System.currentTimeMillis()
            while (!connected && (GlobalApp.BLE?.connectionState != STATE_CONNECTED || GlobalApp.BLE?.bleGATT == null)) {
                connected = GlobalApp.BLE?.connect(GlobalApp.BLE?.bleAddress!!)!!
                if (System.currentTimeMillis() - start > 10 * TIME_OUT) break
            }
            if (GlobalApp.BLE?.connectionState == STATE_CONNECTED || connected) {
                Thread.sleep(1000)
                GlobalApp.BLE?.fetchDeviceStatus()
                Thread.sleep(1000)
                activity?.runOnUiThread { this.updateUIStatusBar() }
            } else Log.d("MAP", "Unable to Connect to Device")
            Log.d("MAP", "Connection Handler Complete")
        } catch (e: Throwable) {
            Log.e("MAP", "Connection Handler Error")
        }
    }

    private fun updateUIStatusBar() {
        val statusBarArray: ArrayList<View>? = ArrayList(0)
        statusBarArray?.add(view?.statusBar1!!)
        statusBarArray?.add(view?.statusBar2!!)
        statusBarArray?.add(view?.statusBar3!!)
        statusBarArray?.add(view?.statusBar4!!)
        statusBarArray?.add(view?.statusBar5!!)
        statusBarArray?.add(view?.statusBar6!!)
        for (i in 0 until NUMBER_OF_FLAGS) {
            val view: View? = statusBarArray?.get(i)
            if (statusBarArray != null && GlobalApp.BLE?.gpsStatusFlags?.get(i)!!) {
                if (view != null) {
                    statusBarArray[i].background = ContextCompat.getDrawable(requireContext(), R.drawable.status_on)
                }
            } else if (!GlobalApp.BLE?.gpsStatusFlags?.get(i)!!) {
                if (view != null) {
                    statusBarArray[i].background = ContextCompat.getDrawable(requireContext(), R.drawable.status_off)
                }
            }
        }
    }

    private fun bleLinkCheck() {
        val main = activity as MainActivity?
        if (main != null && main.checkBTon() && GlobalApp.BLE?.connectionState == STATE_CONNECTED) {
            if (GlobalApp.BLE == null) {
                GlobalApp.BLE = BLEDevice(main as Context, activity as ContextWrapper)
                GlobalApp.BLE!!.initialize()
            }
            if (this.mapCheckThread != null && this.mapCheckThread!!.isAlive) this.mapCheckThread!!.interrupt()
            this.mapCheckThread = null
            this.mapCheckThread = Thread(Runnable { this.connectionHandler() })
            this.mapCheckThread!!.start()
        }
    }
}
