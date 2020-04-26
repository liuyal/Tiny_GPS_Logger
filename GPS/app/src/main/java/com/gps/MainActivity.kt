package com.gps

import android.bluetooth.BluetoothAdapter
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
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
            if (GlobalApplication.BLE == null) {
                GlobalApplication.BLE = BLEDevice(this, applicationContext as ContextWrapper)
                GlobalApplication.BLE!!.initialize()
            } else {
                GlobalApplication.BLE?.context = this
                GlobalApplication.BLE?.applicationContext = applicationContext as ContextWrapper
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
            GlobalApplication.BLE?.close()
        } catch (e: Throwable) {
            Log.e("MAIN", "GATT Closing ERROR")
        }
    }



    fun checkTask() {
        val macAddress = GlobalApplication.BLE?.loadDBMAC() ?: return
        this.runOnUiThread { updateUIInfo(macAddress) }
        if (GlobalApplication.BLE?.connectionState != STATE_CONNECTED || GlobalApplication.BLE?.bleGATT == null) GlobalApplication.BLE?.connect(macAddress)!!
        Log.d("MAIN", "statusCheckTask start")
        while (true) {
            try {
                if (GlobalApplication.BLE?.connectionState == STATE_CONNECTED || GlobalApplication.BLE?.bleGATT != null) {
                    GlobalApplication.BLE?.fetchDeviceStatus()
                    this.runOnUiThread { updateUIFlags() }
                    if (GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_FIX_FLAG_INDEX)!!) {
                        GlobalApplication.BLE?.fetchGPSData()
                        this.runOnUiThread { updateUICoordinate() }
                        Thread.sleep((5 * TIME_OUT).toLong())
                    }
                } else throw IllegalArgumentException("CONNECTION STOPPED")
            } catch (e: Throwable) {
                if (e.localizedMessage != null) Log.e("", e.localizedMessage!!.toString())
                this.runOnUiThread { disconnectionHandler() }
                break
            }
        }
        Log.d("MAIN", "statusCheckTask Done")
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }



    private fun updateUIInfo(macAddress: String) {
        val macLabel: TextView = findViewById(R.id.mac_text_label)
        val deviceLabel: TextView = findViewById(R.id.device_text_label)
        if (macAddress != "") {
            deviceLabel.text = DEVICE_CODE_NAME
            macLabel.text = macAddress
        } else {
            deviceLabel.text = "-"
            macLabel.text = getString(R.string.initial_ble_mac)
        }
    }

    private fun updateUICoordinate() {
        val gpsTime: TextView = findViewById(R.id.time_text_label)
        val latitude: TextView = findViewById(R.id.lat_text_label)
        val longitude: TextView = findViewById(R.id.long_text_label)
        var gpsData = GlobalApplication.BLE?.gpsData
        if (gpsData != null && gpsData != "") {
            gpsData = gpsData.substring(gpsData.indexOf('[') + 1, gpsData.indexOf(']'))
            val gpdDataList = gpsData.split(',') as ArrayList<String>
            var hour = gpdDataList[6]
            var minute = gpdDataList[7]
            var second = gpdDataList[8]
            if (hour.toInt() < 10) hour = "0" + gpdDataList[6]
            if (minute.toInt() < 10) minute = "0" + gpdDataList[7]
            if (second.toInt() < 10) second = "0" + gpdDataList[8]
            gpsTime.text = getString(R.string.gpsTime, hour, minute, second)
            latitude.text = gpdDataList[3]
            longitude.text = gpdDataList[4]
        } else {
            gpsTime.text = getString(R.string.init_time)
            latitude.text = getString(R.string.init_lat)
            longitude.text = getString(R.string.init_long)
        }
    }

    private fun updateUIFlags() {
        val b1 = GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_CONNECTION_FLAG_INDEX)
        val b2 = GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_ON_FLAG_INDEX)
        val b3 = GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_FIX_FLAG_INDEX)
        val b4 = GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_SERIAL_PRINT_FLAG_INDEX)
        val b5 = GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_BLE_PRINT_FLAG_INDEX)
        val b6 = GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_LOGGING_FLAG_INDEX)
        val statusBar1: View? = findViewById(R.id.statusBar1)
        val statusBar2: View? = findViewById(R.id.statusBar2)
        val statusBar3: View? = findViewById(R.id.statusBar3)
        val statusBar4: View? = findViewById(R.id.statusBar4)
        val statusBar5: View? = findViewById(R.id.statusBar5)
        val statusBar6: View? = findViewById(R.id.statusBar6)
        val connectedLabel: AppCompatImageView? = findViewById(R.id.connected_text_label)
        val gpsLabel: AppCompatImageView? = findViewById(R.id.gps_on_text_label)
        val fixLabel: AppCompatImageView? = findViewById(R.id.fix_text_label)
        val serialLabel: AppCompatImageView? = findViewById(R.id.serial_text_label)
        val bledLabel: AppCompatImageView? = findViewById(R.id.bleb_text_label)
        val logLabel: AppCompatImageView? = findViewById(R.id.log_text_label)

        if (b1!! && statusBar1 != null && connectedLabel != null) {
            statusBar1.background = getDrawable(R.drawable.status_on)
            connectedLabel.setImageResource(R.drawable.ic_check_black_24dp)
            connectedLabel.setColorFilter(Color.parseColor("#00b250"))
        } else if (!b1 && statusBar1 != null && connectedLabel != null) {
            statusBar1.background = getDrawable(R.drawable.status_off)
            connectedLabel.setImageResource(R.drawable.ic_close_black_24dp)
            connectedLabel.setColorFilter(Color.argb(255, 255, 0, 0))
        }
        if (b2!! && statusBar2 != null && gpsLabel != null) {
            statusBar2.background = getDrawable(R.drawable.status_on)
            gpsLabel.setImageResource(R.drawable.ic_check_black_24dp)
            gpsLabel.setColorFilter(Color.parseColor("#00b250"))
        } else if (!b2 && statusBar2 != null && gpsLabel != null) {
            statusBar2.background = getDrawable(R.drawable.status_off)
            gpsLabel.setImageResource(R.drawable.ic_close_black_24dp)
            gpsLabel.setColorFilter(Color.argb(255, 255, 0, 0))
        }
        if (b3!! && statusBar3 != null && fixLabel != null) {
            statusBar3.background = getDrawable(R.drawable.status_on)
            fixLabel.setImageResource(R.drawable.ic_check_black_24dp)
            fixLabel.setColorFilter(Color.parseColor("#00b250"))
        } else if (!b3 && statusBar3 != null && fixLabel != null) {
            statusBar3.background = getDrawable(R.drawable.status_off)
            fixLabel.setImageResource(R.drawable.ic_close_black_24dp)
            fixLabel.setColorFilter(Color.argb(255, 255, 0, 0))
        }
        if (b4!! && statusBar4 != null && serialLabel != null) {
            statusBar4.background = getDrawable(R.drawable.status_on)
            serialLabel.setImageResource(R.drawable.ic_check_black_24dp)
            serialLabel.setColorFilter(Color.parseColor("#00b250"))
        } else if (!b4 && statusBar4 != null && serialLabel != null) {
            statusBar4.background = getDrawable(R.drawable.status_off)
            serialLabel.setImageResource(R.drawable.ic_close_black_24dp)
            serialLabel.setColorFilter(Color.argb(255, 255, 0, 0))
        }
        if (b5!! && statusBar5 != null && bledLabel != null) {
            statusBar5.background = getDrawable(R.drawable.status_on)
            bledLabel.setImageResource(R.drawable.ic_check_black_24dp)
            bledLabel.setColorFilter(Color.parseColor("#00b250"))
        } else if (!b5 && statusBar5 != null && bledLabel != null) {
            statusBar5.background = getDrawable(R.drawable.status_off)
            bledLabel.setImageResource(R.drawable.ic_close_black_24dp)
            bledLabel.setColorFilter(Color.argb(255, 255, 0, 0))
        }
        if (b6!! && statusBar6 != null && logLabel != null) {
            statusBar6.background = getDrawable(R.drawable.status_on)
            logLabel.setImageResource(R.drawable.ic_check_black_24dp)
            logLabel.setColorFilter(Color.parseColor("#00b250"))
        } else if (!b6 && statusBar6 != null && logLabel != null) {
            statusBar6.background = getDrawable(R.drawable.status_off)
            logLabel.setImageResource(R.drawable.ic_close_black_24dp)
            logLabel.setColorFilter(Color.argb(255, 255, 0, 0))
        }
    }

    private fun disconnectionHandler() {
        try {
            GlobalApplication.BLE?.gpsStatusFlags?.fill(false, 0, NUMBER_OF_FLAGS)
            GlobalApplication.BLE?.gpsData = ""
            updateUIFlags()
            updateUICoordinate()
        } catch (e: Throwable) {
            Log.e("MAIN", "Disconnection Error")
        }
        Log.e("MAIN", "Unable to connect to BLE Device")
    }




    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
