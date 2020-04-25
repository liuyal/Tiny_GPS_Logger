package com.gps

import android.bluetooth.BluetoothAdapter
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.ColorStateList
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
    var mapThread: Thread? = null

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
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_map, R.id.nav_logs), drawerLayout)
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

    // TODO: IF mac is null update UI to show no device flag
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
                    Thread.sleep((10 * TIME_OUT).toLong())
                    if (GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_FIX_FLAG_INDEX)!!) GlobalApplication.BLE?.fetchGPSdata()
                    this.runOnUiThread { updateUICoordinate() }
                    Thread.sleep((10 * TIME_OUT).toLong())
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
        val mac_label: TextView = findViewById(R.id.mac_text_label)
        mac_label.text = macAddress
        val device_text_label: TextView = findViewById(R.id.device_text_label)
        device_text_label.text = DEVICE_CODE_NAME
    }

    // TODO: Parse data and update UIs
    private fun updateUICoordinate() {
        val gpsTime: TextView = findViewById(R.id.time_text_label)
        gpsTime.text = ""
        val latitude: TextView = findViewById(R.id.lat_text_label)
        latitude.text = ""
        val longitude: TextView = findViewById(R.id.long_text_label)
        longitude.text = ""
    }

    private fun updateUIFlags() {
        val B1 = GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_CONNECTION_FLAG_INDEX)
        val B2 = GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_ON_FLAG_INDEX)
        val B3 = GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_FIX_FLAG_INDEX)
        val B4 = GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_SERIAL_PRINT_FLAG_INDEX)
        val B5 = GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_BLE_PRINT_FLAG_INDEX)
        val B6 = GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_LOGGING_FLAG_INDEX)
        val statusBar1: View = findViewById(R.id.statusBar1)
        val statusBar2: View = findViewById(R.id.statusBar2)
        val statusBar3: View = findViewById(R.id.statusBar3)
        val statusBar4: View = findViewById(R.id.statusBar4)
        val statusBar5: View = findViewById(R.id.statusBar5)
        val statusBar6: View = findViewById(R.id.statusBar6)
        val connected_label: AppCompatImageView = findViewById(R.id.connected_text_label)
        val gps_label: AppCompatImageView = findViewById(R.id.gps_on_text_label)
        val fix_label: AppCompatImageView = findViewById(R.id.fix_text_label)
        val serial_label: AppCompatImageView = findViewById(R.id.serial_text_label)
        val blep_label: AppCompatImageView = findViewById(R.id.blep_text_label)
        val log_label: AppCompatImageView = findViewById(R.id.log_text_label)

        if (B1!!) {
            statusBar1.background = getDrawable(R.drawable.status_on)
            connected_label.setImageResource(R.drawable.ic_check_black_24dp)
            connected_label.setColorFilter(Color.parseColor("#00b250"))
        } else {
            statusBar1.background = getDrawable(R.drawable.status_off)
            connected_label.setImageResource(R.drawable.ic_close_black_24dp)
            connected_label.setColorFilter(Color.argb(255, 255, 0, 0))
        }
        if (B2!!) {
            statusBar2.background = getDrawable(R.drawable.status_on)
            gps_label.setImageResource(R.drawable.ic_check_black_24dp)
            gps_label.setColorFilter(Color.parseColor("#00b250"))
        } else {
            statusBar2.background = getDrawable(R.drawable.status_off)
            gps_label.setImageResource(R.drawable.ic_close_black_24dp)
            gps_label.setColorFilter(Color.argb(255, 255, 0, 0))
        }
        if (B3!!) {
            statusBar3.background = getDrawable(R.drawable.status_on)
            fix_label.setImageResource(R.drawable.ic_check_black_24dp)
            fix_label.setColorFilter(Color.parseColor("#00b250"))
        } else {
            statusBar3.background = getDrawable(R.drawable.status_off)
            fix_label.setImageResource(R.drawable.ic_close_black_24dp)
            fix_label.setColorFilter(Color.argb(255, 255, 0, 0))
        }
        if (B4!!) {
            statusBar4.background = getDrawable(R.drawable.status_on)
            serial_label.setImageResource(R.drawable.ic_check_black_24dp)
            serial_label.setColorFilter(Color.parseColor("#00b250"))
        } else {
            statusBar4.background = getDrawable(R.drawable.status_off)
            serial_label.setImageResource(R.drawable.ic_close_black_24dp)
            serial_label.setColorFilter(Color.argb(255, 255, 0, 0))
        }
        if (B5!!) {
            statusBar5.background = getDrawable(R.drawable.status_on)
            blep_label.setImageResource(R.drawable.ic_check_black_24dp)
            blep_label.setColorFilter(Color.parseColor("#00b250"))
        } else {
            statusBar5.background = getDrawable(R.drawable.status_off)
            blep_label.setImageResource(R.drawable.ic_close_black_24dp)
            blep_label.setColorFilter(Color.argb(255, 255, 0, 0))
        }
        if (B6!!) {
            statusBar6.background = getDrawable(R.drawable.status_on)
            log_label.setImageResource(R.drawable.ic_check_black_24dp)
            log_label.setColorFilter(Color.parseColor("#00b250"))
        } else {
            statusBar6.background = getDrawable(R.drawable.status_off)
            log_label.setImageResource(R.drawable.ic_close_black_24dp)
            log_label.setColorFilter(Color.argb(255, 255, 0, 0))
        }
    }

    private fun disconnectionHandler() {
        //Toast.makeText(applicationContext, "Unable to connect to BLE Device!", Toast.LENGTH_SHORT).show()
        GlobalApplication.BLE?.gpsStatusFlags?.fill(false, 0, NUMBER_OF_FLAGS)
        updateUIFlags()
        Log.e("MAIN", "Unable to connect to BLE Device")
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
