package com.gps

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.gps.objects.*
import maes.tech.intentanim.CustomIntent

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    var statueCheckThread: Thread? = null

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

    fun checkTaskLoop() {
        Log.d("MAIN", "STATUS CHECK START")
        val progressBar: ProgressBar? = findViewById(R.id.status_progress_bar)
        this.runOnUiThread { progressBar?.visibility = View.VISIBLE }
        if (GlobalApp.BLE?.bleAddress == null || GlobalApp.BLE?.bleAddress == "") {
            this.runOnUiThread { disconnectionUIHandler() }
            Log.d("MAIN", "NO DEVICE")
            return
        }
        this.runOnUiThread { updateUIInfo(GlobalApp.BLE?.bleAddress!!) }
        if (GlobalApp.BLE?.connectionState != STATE_CONNECTED || GlobalApp.BLE?.bleGATT == null) GlobalApp.BLE?.connect(GlobalApp.BLE?.bleAddress!!)!!
        while (true) {
            try {
                if (GlobalApp.BLE?.connectionState == STATE_CONNECTED || GlobalApp.BLE?.bleGATT != null) {
                    GlobalApp.BLE?.fetchDeviceStatus()
                    if (GlobalApp.BLE?.gpsStatusFlags?.get(GPS_FIX_FLAG_INDEX)!!) GlobalApp.BLE?.fetchGPSData()
                    else GlobalApp.BLE?.gpsData = ""
                    this.runOnUiThread { updateUIFlags() }
                    this.runOnUiThread { updateUICoordinate() }
                    this.runOnUiThread { progressBar?.visibility = View.GONE }
                    Thread.sleep((5 * TIME_OUT).toLong())
                } else throw IllegalArgumentException()
            } catch (e: Throwable) {
                break
            }
        }
        Log.d("MAIN", "STATUS CHECK STOPPED")
    }

    private fun updateUIInfo(macAddress: String) {
        val macLabel: TextView? = findViewById(R.id.mac_text_label)
        val deviceLabel: TextView? = findViewById(R.id.device_text_label)
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

    private fun updateUICoordinate() {
        val gpsTime: TextView? = findViewById(R.id.time_text_label)
        val latitude: TextView? = findViewById(R.id.lat_text_label)
        val longitude: TextView? = findViewById(R.id.long_text_label)
        var gpsData = GlobalApp.BLE?.gpsData
        if (gpsData != null && gpsData != "" && gpsTime != null && latitude != null && longitude != null) {
            gpsData = gpsData.substring(gpsData.indexOf('[') + 1, gpsData.indexOf(']'))
            val gpdDataList = gpsData.split(',') as ArrayList<String>
            if (gpdDataList[6].toInt() < 10) gpdDataList[6] = "0" + gpdDataList[6]
            if (gpdDataList[7].toInt() < 10) gpdDataList[7] = "0" + gpdDataList[7]
            if (gpdDataList[8].toInt() < 10) gpdDataList[8] = "0" + gpdDataList[8]
            gpsTime.text = getString(R.string.gpsTime, gpdDataList[6], gpdDataList[7], gpdDataList[8])
            latitude.text = gpdDataList[3]
            longitude.text = gpdDataList[4]
        } else if (gpsData == "") {
            if (gpsTime != null && latitude != null && longitude != null) {
                gpsTime.text = getString(R.string.init_time)
                latitude.text = getString(R.string.init_lat)
                longitude.text = getString(R.string.init_long)
            }
        }
    }

    private fun updateUIFlags() {
        val statusBarArray: ArrayList<View>? = ArrayList(0)
        val iconArray: ArrayList<AppCompatImageView>? = ArrayList(0)
        statusBarArray?.add(findViewById(R.id.statusBar1))
        statusBarArray?.add(findViewById(R.id.statusBar2))
        statusBarArray?.add(findViewById(R.id.statusBar3))
        statusBarArray?.add(findViewById(R.id.statusBar4))
        statusBarArray?.add(findViewById(R.id.statusBar5))
        statusBarArray?.add(findViewById(R.id.statusBar6))
        iconArray?.add(findViewById(R.id.connected_text_label))
        iconArray?.add(findViewById(R.id.gps_on_text_label))
        iconArray?.add(findViewById(R.id.fix_text_label))
        iconArray?.add(findViewById(R.id.serial_text_label))
        iconArray?.add(findViewById(R.id.bleb_text_label))
        iconArray?.add(findViewById(R.id.log_text_label))

        for (i in 0 until NUMBER_OF_FLAGS) {
            val view: View? = statusBarArray?.get(i)
            val icon: AppCompatImageView? = iconArray?.get(i)
            if (statusBarArray != null && GlobalApp.BLE?.gpsStatusFlags?.get(i)!!) {
                if (view != null && icon != null) {
                    statusBarArray[i].background = getDrawable(R.drawable.status_on)
                    iconArray[i].setImageResource(R.drawable.ic_check_black_24dp)
                    iconArray[i].setColorFilter(Color.parseColor("#00b250"))
                }
            } else if (iconArray != null && !GlobalApp.BLE?.gpsStatusFlags?.get(i)!!) {
                if (view != null && icon != null) {
                    statusBarArray[i].background = getDrawable(R.drawable.status_off)
                    iconArray[i].setImageResource(R.drawable.ic_close_black_24dp)
                    iconArray[i].setColorFilter(Color.argb(255, 255, 0, 0))
                }
            }
        }
    }

    fun connectionUIHandler() {
        Log.d("MAIN", "Connection UI Handler Starting")
        var connected = false
        val progressBar: ProgressBar? = findViewById(R.id.status_progress_bar)
        this.runOnUiThread { progressBar?.visibility = View.VISIBLE }
        if (GlobalApp.BLE?.connectionState != STATE_CONNECTED || GlobalApp.BLE?.bleGATT == null) {
            connected = GlobalApp.BLE?.connect(GlobalApp.BLE?.bleAddress!!)!!
        }
        if (connected) {
            GlobalApp.BLE?.fetchDeviceStatus()
            this.runOnUiThread { updateUIInfo(GlobalApp.BLE?.bleAddress!!) }
            this.runOnUiThread { updateUIFlags() }
            this.runOnUiThread { progressBar?.visibility = View.GONE }
        } else {
            this.runOnUiThread { progressBar?.visibility = View.GONE }
            Log.d("MAIN", "Unable to connect to Device")
        }
        Log.d("MAIN", "Connection UI Handler Complete")
    }

    fun disconnectionUIHandler() {
        try {
            val progressBar: ProgressBar? = findViewById(R.id.status_progress_bar)
            progressBar?.visibility = View.GONE
            GlobalApp.BLE?.gpsStatusFlags?.fill(false, 0, NUMBER_OF_FLAGS)
            GlobalApp.BLE?.gpsData = ""
            updateUIInfo("")
            updateUIFlags()
            updateUICoordinate()
        } catch (e: Throwable) {
            Log.e("MAIN", "Disconnection UI update Error")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
