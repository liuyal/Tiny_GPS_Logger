package com.gps

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.material.navigation.NavigationView
import com.gps.objects.*
import com.mapbox.android.gestures.Utils
import maes.tech.intentanim.CustomIntent

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var appBarConfiguration: AppBarConfiguration

    var statueCheckThread: Thread? = null

    private lateinit var googleMap: GoogleMap
    private lateinit var markerOptions: MarkerOptions
    private lateinit var marker: Marker
    private lateinit var cameraPosition: CameraPosition
    private var defaultLatitude = 49.279793
    private var defaultLongitude = -123.115669

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.elevation = 0F
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_settings -> {
                    val intent = Intent(this, BLEActivity::class.java)
                    startActivity(intent)
                    CustomIntent.customType(this, "left-to-right")
                }
            }
            true
        }
        if (this.checkBTon() && GlobalApp.BLE == null) {
            GlobalApp.BLE = BLEDevice(this, applicationContext as ContextWrapper)
            GlobalApp.BLE!!.initialize()
        } else if (this.checkBTon()) {
            GlobalApp.BLE?.context = this
            GlobalApp.BLE?.applicationContext = applicationContext as ContextWrapper
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_map, R.id.nav_logs, R.id.nav_setting), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onStop() {
        super.onStop()
        try {
            GlobalApp.BLE?.close()
        } catch (e: Throwable) {
            Log.e("MAIN", "GATT Closing ERROR")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        this.googleMap = googleMap!!
        this.googleMap.uiSettings.isZoomControlsEnabled = true
        this.googleMap.uiSettings.isCompassEnabled = true
        this.googleMap.uiSettings.isMapToolbarEnabled = true
        this.googleMap.uiSettings.isScrollGesturesEnabled = true
        this.googleMap.uiSettings.isTiltGesturesEnabled = true
        this.googleMap.uiSettings.isRotateGesturesEnabled = true
        this.marker = googleMap.addMarker( this.markerOptions)
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition( this.cameraPosition))
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    fun createMaps() {
        this.markerOptions = MarkerOptions()
        val bitmapDescriptor = bitmapDescriptorFromVector(this, R.drawable.ic_fiber_manual_record_black_24dp)
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

    fun updateMaps() {
        this.connectionHandler()
        if (GlobalApp.BLE?.gpsStatusFlags?.get(GPS_FIX_FLAG_INDEX)!!) GlobalApp.BLE?.fetchGPSData()
        else GlobalApp.BLE?.gpsData = ""
        if (GlobalApp.BLE?.gpsData != null && GlobalApp.BLE?.gpsData != "") {
            var gpsData = GlobalApp.BLE?.gpsData!!
            gpsData = gpsData.substring(gpsData.indexOf('[') + 1, gpsData.indexOf(']'))
            val gpdDataList = gpsData.split(',') as ArrayList<String>
            this.defaultLatitude = gpdDataList[LOCATION_LAT_INDEX].toDouble()
            this.defaultLongitude = gpdDataList[LOCATION_LNG_INDEX].toDouble()
            runOnUiThread {
                marker.position = LatLng(defaultLatitude, defaultLongitude)
                cameraPosition = CameraPosition.Builder().target(LatLng(defaultLatitude, defaultLongitude)).zoom(15f).build()
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        } else Log.d("MAIN", "NO GPS FIX")
        Log.d("MAIN", "MAPS UPDATE")
    }

    fun connectionHandler() {
        Log.d("MAIN", "Connection Handler Starting")
        if (GlobalApp.BLE?.bleAddress == null || GlobalApp.BLE?.bleAddress == "") {
            this.runOnUiThread { Toast.makeText(applicationContext, "No Device Paired", Toast.LENGTH_SHORT).show() }
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
            this.runOnUiThread {}// updateUIStatusBar() }
        } else Log.d("MAIN", "Unable to Connect to Device")
        Log.d("MAIN", "Connection Handler Complete")
    }

    fun checkBTon(): Boolean {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Toast.makeText(applicationContext, "BlueTooth is not supported!", Toast.LENGTH_SHORT).show()
        } else if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            Toast.makeText(applicationContext, "BlueTooth is not enabled!", Toast.LENGTH_SHORT).show()
        } else if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
            return true
        }
        return false
    }

}
