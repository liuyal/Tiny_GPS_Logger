package com.gps

import android.bluetooth.BluetoothAdapter
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.gps.objects.*
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
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Toast.makeText(applicationContext, "BlueTooth is not supported!", Toast.LENGTH_SHORT).show()
        } else if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            Toast.makeText(applicationContext, "BlueTooth is not enabled!", Toast.LENGTH_SHORT).show()
        } else if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
            if (GlobalApp.BLE == null) {
                GlobalApp.BLE = BLEDevice(this, applicationContext as ContextWrapper)
                GlobalApp.BLE!!.initialize()
            } else {
                GlobalApp.BLE?.context = this
                GlobalApp.BLE?.applicationContext = applicationContext as ContextWrapper
            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_map, R.id.nav_logs, R.id.nav_setting), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onStart() {
        super.onStart()
        this.updateUIDeviceInfo()
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

    override fun onMapReady(googleMap: GoogleMap?) {
        this.googleMap = googleMap!!
        this.googleMap.uiSettings.isZoomControlsEnabled = true
        this.googleMap.uiSettings.isCompassEnabled = true
        this.googleMap.uiSettings.isMapToolbarEnabled = true
        this.googleMap.uiSettings.isScrollGesturesEnabled = true
        this.googleMap.uiSettings.isTiltGesturesEnabled = true
        this.googleMap.uiSettings.isRotateGesturesEnabled = true
        marker = googleMap.addMarker(markerOptions)
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    fun createMaps() {
        markerOptions = MarkerOptions()
        markerOptions.position(LatLng(defaultLatitude, defaultLongitude))
        cameraPosition = CameraPosition.Builder().target(LatLng(defaultLatitude, defaultLongitude)).zoom(10f).build()
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

    fun checkTaskLoop() {
        Log.d("MAIN", "STATUS CHECK START")
        val delay = 5 // TODO: configurable delay (in settings frag)
        this.runOnUiThread { findViewById<ProgressBar>(R.id.status_progress_bar)?.visibility = View.VISIBLE }
        if (GlobalApp.BLE?.bleAddress == null || GlobalApp.BLE?.bleAddress == "") {
            this.runOnUiThread {
                disconnectionUIHandler()
                Toast.makeText(applicationContext, "No Device Paired", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (GlobalApp.BLE?.connectionState != STATE_CONNECTED || GlobalApp.BLE?.bleGATT == null) GlobalApp.BLE?.connect(GlobalApp.BLE?.bleAddress!!)!!
        this.runOnUiThread {
            updateUIDeviceInfo()
            findViewById<ImageButton>(R.id.action_button_A).setColorFilter(Color.GREEN)
            findViewById<ImageButton>(R.id.action_button_B).setColorFilter(ResourcesCompat.getColor(resources, R.color.DodgerBlue, null))
        }
        Thread.sleep(1000)
        while (true) {
            try {
                if (GlobalApp.BLE?.connectionState == STATE_CONNECTED || GlobalApp.BLE?.bleGATT != null) {
                    GlobalApp.BLE?.fetchDeviceStatus()
                    if (GlobalApp.BLE?.gpsStatusFlags?.get(GPS_FIX_FLAG_INDEX)!!) GlobalApp.BLE?.fetchGPSData()
                    else GlobalApp.BLE?.gpsData = ""
                    this.runOnUiThread {
                        updateUIStatusInfo()
                        updateUIStatusBar()
                        updateUICoordinateInfo()
                        findViewById<ProgressBar>(R.id.status_progress_bar)?.visibility = View.GONE
                    }
                    Thread.sleep((delay * TIME_OUT).toLong())
                } else throw IllegalArgumentException()
            } catch (e: Throwable) {
                break
            }
        }
        Log.d("MAIN", "STATUS CHECK STOPPED")
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

        if (connected || GlobalApp.BLE?.connectionState == STATE_CONNECTED) {
            GlobalApp.BLE?.fetchDeviceStatus()
            this.runOnUiThread { updateUIStatusBar() }
        } else Log.d("MAIN", "Unable to Connect to Device")

        Log.d("MAIN", "Connection Handler Complete")
    }

    fun connectionUIHandler() {
        Log.d("MAIN", "Connection UI Handler Starting")
        this.runOnUiThread { findViewById<ProgressBar>(R.id.status_progress_bar)?.visibility = View.VISIBLE }
        if (GlobalApp.BLE?.bleAddress == null || GlobalApp.BLE?.bleAddress == "") {
            this.runOnUiThread {
                disconnectionUIHandler()
                Toast.makeText(applicationContext, "No Device Paired", Toast.LENGTH_SHORT).show()
            }
            return
        }
        var connected = false
        val start = System.currentTimeMillis()
        while (!connected && (GlobalApp.BLE?.connectionState != STATE_CONNECTED || GlobalApp.BLE?.bleGATT == null)) {
            connected = GlobalApp.BLE?.connect(GlobalApp.BLE?.bleAddress!!)!!
            if (System.currentTimeMillis() - start > 10 * TIME_OUT) break
        }
        if (connected || GlobalApp.BLE?.connectionState == STATE_CONNECTED) {
            GlobalApp.BLE?.fetchDeviceStatus()
            Thread.sleep(1000)
            this.runOnUiThread {
                updateUIDeviceInfo()
                updateUIStatusInfo()
                updateUIStatusBar()
                findViewById<ImageButton>(R.id.action_button_B).setColorFilter(ResourcesCompat.getColor(resources, R.color.DodgerBlue, null))
            }
        } else {
            this.runOnUiThread {
                findViewById<ImageButton>(R.id.action_button_A).setColorFilter(Color.WHITE)
                findViewById<ImageButton>(R.id.action_button_B).setColorFilter(Color.WHITE)
            }
            Log.d("MAIN", "Unable to Connect to Device")
        }
        this.runOnUiThread { findViewById<ProgressBar>(R.id.status_progress_bar)?.visibility = View.GONE }
        Log.d("MAIN", "Connection UI Handler Complete")
    }

    fun disconnectionUIHandler() {
        try {
            findViewById<ImageButton>(R.id.action_button_A).setColorFilter(Color.WHITE)
            findViewById<ImageButton>(R.id.action_button_B).setColorFilter(Color.WHITE)
            findViewById<ProgressBar>(R.id.status_progress_bar)?.visibility = View.GONE
            GlobalApp.BLE?.disconnect()
            updateUIStatusInfo()
            updateUIStatusBar()
            updateUICoordinateInfo()
        } catch (e: Throwable) {
            Log.e("MAIN", "Disconnection UI Update Error")
        }
    }

    private fun updateUIStatusBar() {
        val statusBarArray: ArrayList<View>? = ArrayList(0)
        statusBarArray?.add(findViewById(R.id.statusBar1))
        statusBarArray?.add(findViewById(R.id.statusBar2))
        statusBarArray?.add(findViewById(R.id.statusBar3))
        statusBarArray?.add(findViewById(R.id.statusBar4))
        statusBarArray?.add(findViewById(R.id.statusBar5))
        statusBarArray?.add(findViewById(R.id.statusBar6))
        for (i in 0 until NUMBER_OF_FLAGS) {
            val view: View? = statusBarArray?.get(i)
            if (statusBarArray != null && GlobalApp.BLE?.gpsStatusFlags?.get(i)!!) {
                if (view != null) {
                    statusBarArray[i].background = getDrawable(R.drawable.status_on)
                }
            } else if (!GlobalApp.BLE?.gpsStatusFlags?.get(i)!!) {
                if (view != null) {
                    statusBarArray[i].background = getDrawable(R.drawable.status_off)
                }
            }
        }
    }

    private fun updateUIDeviceInfo() {
        val macLabel: TextView? = findViewById(R.id.mac_text_label)
        val deviceLabel: TextView? = findViewById(R.id.device_text_label)
        val macAddress = GlobalApp.BLE?.loadDBMAC()!!
        if (macAddress != "" && macLabel != null && deviceLabel != null) {
            deviceLabel.text = DEVICE_CODE_NAME
            macLabel.text = macAddress
        } else {
            if (macLabel != null && deviceLabel != null) {
                deviceLabel.text = getString(R.string.initial_ble_name)
                macLabel.text = getString(R.string.initial_ble_mac)
            }
        }
    }

    private fun updateUIStatusInfo() {
        val iconArray: ArrayList<AppCompatImageView>? = ArrayList(0)
        iconArray?.add(findViewById(R.id.connected_text_label))
        iconArray?.add(findViewById(R.id.gps_on_text_label))
        iconArray?.add(findViewById(R.id.fix_text_label))
        iconArray?.add(findViewById(R.id.serial_text_label))
        iconArray?.add(findViewById(R.id.bleb_text_label))
        iconArray?.add(findViewById(R.id.log_text_label))
        for (i in 0 until NUMBER_OF_FLAGS) {
            val icon: AppCompatImageView? = iconArray?.get(i)
            if (GlobalApp.BLE?.gpsStatusFlags?.get(i)!!) {
                if (icon != null) {
                    iconArray[i].setImageResource(R.drawable.ic_check_black_24dp)
                    iconArray[i].setColorFilter(Color.parseColor("#00b250"))
                }
            } else if (iconArray != null && !GlobalApp.BLE?.gpsStatusFlags?.get(i)!!) {
                if (icon != null) {
                    iconArray[i].setImageResource(R.drawable.ic_close_black_24dp)
                    iconArray[i].setColorFilter(Color.argb(255, 255, 0, 0))
                }
            }
        }
    }

    private fun updateUICoordinateInfo() {
        val gpsDate: TextView? = findViewById(R.id.date_text_label)
        val gpsTime: TextView? = findViewById(R.id.time_text_label)
        val latitude: TextView? = findViewById(R.id.lat_text_label)
        val longitude: TextView? = findViewById(R.id.long_text_label)
        var gpsData = GlobalApp.BLE?.gpsData
        if (gpsData != null && gpsData != "") {
            gpsData = gpsData.substring(gpsData.indexOf('[') + 1, gpsData.indexOf(']'))
            val gpdDataList = gpsData.split(',') as ArrayList<String>
            if (gpdDataList[TIME_HOUR_INDEX].toInt() < 10) gpdDataList[TIME_HOUR_INDEX] = "0" + gpdDataList[TIME_HOUR_INDEX]
            if (gpdDataList[TIME_MINUTE_INDEX].toInt() < 10) gpdDataList[TIME_MINUTE_INDEX] = "0" + gpdDataList[TIME_MINUTE_INDEX]
            if (gpdDataList[TIME_SECOND_INDEX].toInt() < 10) gpdDataList[TIME_SECOND_INDEX] = "0" + gpdDataList[TIME_SECOND_INDEX]
            gpsDate?.text = gpdDataList[DATE_VALUE_INDEX]
            gpsTime?.text = getString(R.string.gpsTime, gpdDataList[TIME_HOUR_INDEX], gpdDataList[TIME_MINUTE_INDEX], gpdDataList[TIME_SECOND_INDEX])
            latitude?.text = gpdDataList[LOCATION_LAT_INDEX]
            longitude?.text = gpdDataList[LOCATION_LNG_INDEX]
        } else if (gpsData == null || gpsData == "") {
            gpsDate?.text = getString(R.string.init_date)
            gpsTime?.text = getString(R.string.init_time)
            latitude?.text = getString(R.string.init_lat)
            longitude?.text = getString(R.string.init_long)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
