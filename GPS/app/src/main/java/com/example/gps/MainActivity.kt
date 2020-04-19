package com.example.gps

import android.bluetooth.BluetoothAdapter
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.gps.objects.BLEDevice
import com.example.gps.objects.GlobalApplication
import com.example.gps.objects.STATE_CONNECTED
import com.example.gps.objects.TIME_OUT
import com.google.android.material.navigation.NavigationView
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
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_map, R.id.nav_logs), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }


    override fun onStart() {
        super.onStart()
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
            statueCheckThread = Thread(Runnable { backgroundTask() })
            statueCheckThread!!.start()
        }
    }

    override fun onStop() {
        super.onStop()
        statueCheckThread?.interrupt()
        statueCheckThread = null
        GlobalApplication.BLE?.close()
    }

    fun backgroundTask() {
        val macAddress = GlobalApplication.BLE?.loadDBMAC()
        if (GlobalApplication.BLE?.connectionState != STATE_CONNECTED || GlobalApplication.BLE?.bleGATT == null) GlobalApplication.BLE?.connect(macAddress)!!
        Log.d("", "Thread start")
        while (true) {
            try {
                if (GlobalApplication.BLE?.connectionState == STATE_CONNECTED || GlobalApplication.BLE?.bleGATT != null) {
                    GlobalApplication.BLE?.fetchDeviceStatus()
                    Thread.sleep((15 * TIME_OUT).toLong())
                } else throw IllegalArgumentException("CONNECTION STOPPED")
            } catch (e: Throwable) {
                if (e.localizedMessage != null) Log.e("", e.localizedMessage!!.toString())
                break
            }
        }
        Log.d("", "Thread Done")
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
