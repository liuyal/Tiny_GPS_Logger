package com.gps

import android.bluetooth.BluetoothAdapter
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    //GlobalApplication.BLE?.fetchGPSdata()
    fun checkTask() {
        val macAddress = GlobalApplication.BLE?.loadDBMAC() ?: return
        if (GlobalApplication.BLE?.connectionState != STATE_CONNECTED || GlobalApplication.BLE?.bleGATT == null) GlobalApplication.BLE?.connect(macAddress)!!
        Log.d("MAIN", "statusCheckTask start")
        while (true) {
            try {
                if (GlobalApplication.BLE?.connectionState == STATE_CONNECTED || GlobalApplication.BLE?.bleGATT != null) {
                    GlobalApplication.BLE?.fetchDeviceStatus()
                    this.runOnUiThread { updateUIFlags() }
                    Thread.sleep((15 * TIME_OUT).toLong())
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

    private fun updateUIFlags() {
        val statusBar1: View = findViewById(R.id.statusBar1)
        val statusBar2: View = findViewById(R.id.statusBar2)
        val statusBar3: View = findViewById(R.id.statusBar3)
        val statusBar4: View = findViewById(R.id.statusBar4)
        val statusBar5: View = findViewById(R.id.statusBar5)
        val statusBar6: View = findViewById(R.id.statusBar6)
        if (GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_CONNECTION_FLAG_INDEX)!!) statusBar1.background = getDrawable(R.drawable.status_on)
        else statusBar1.background = getDrawable(R.drawable.status_off)
        if (GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_FIX_FLAG_INDEX)!!) statusBar2.background = getDrawable(R.drawable.status_on)
        else statusBar2.background = getDrawable(R.drawable.status_off)
        if (GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_ON_FLAG_INDEX)!!) statusBar3.background = getDrawable(R.drawable.status_on)
        else statusBar3.background = getDrawable(R.drawable.status_off)
        if (GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_SERIAL_PRINT_FLAG_INDEX)!!) statusBar4.background = getDrawable(R.drawable.status_on)
        else statusBar4.background = getDrawable(R.drawable.status_off)
        if (GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_BLE_PRINT_FLAG_INDEX)!!) statusBar5.background = getDrawable(R.drawable.status_on)
        else statusBar5.background = getDrawable(R.drawable.status_off)
        if (GlobalApplication.BLE?.gpsStatusFlags?.get(GPS_LOGGING_FLAG_INDEX)!!) statusBar6.background = getDrawable(R.drawable.status_on)
        else statusBar6.background = getDrawable(R.drawable.status_off)
    }

    private fun disconnectionHandler() {
        GlobalApplication.BLE?.gpsStatusFlags?.fill(false, 0, NUMBER_OF_FLAGS)
        updateUIFlags()
        Toast.makeText(applicationContext, "Unable to connect to BLE Device!", Toast.LENGTH_SHORT).show()
        Log.e("MAIN", "Unable to connect to BLE Device")
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
