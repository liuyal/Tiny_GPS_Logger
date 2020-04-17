package com.example.gps

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.SystemClock.sleep
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

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
            RunTask(this).execute()
        }
    }

    override fun onStop() {
        super.onStop()
        RunTask(this).cancel(true)
        GlobalApplication.BLE?.close()
    }

    // TODO: modify UI to indicate no matching device
    fun disconnectionHandler() {
        Toast.makeText(applicationContext, "Unable to connect to Default BLE Device!", Toast.LENGTH_SHORT).show()
        Log.d("", "Unable to connect to BLE Device")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    class RunTask(var c: Context) : AsyncTask<Void, Void, String>() {
        private var errorFlag: Boolean = false
        private var connectFlag: Boolean = false

        override fun doInBackground(vararg p0: Void?): String? {
            val macAddress = GlobalApplication.BLE?.loadDBMAC()
            connectFlag = GlobalApplication.BLE?.connect(macAddress)!!
            if (!connectFlag) return null

            while (!isCancelled) {
                Log.e("RECTASK", isCancelled.toString())
                try {
                    if (GlobalApplication.BLE?.connectionState == STATE_CONNECTED) {
//                        if () return null
                        GlobalApplication.BLE?.fetchDeviceStatus()
                        sleep((5 * TIME_OUT).toLong())
                    } else throw IllegalArgumentException("CONNECTION ERROR")
                } catch (e: Throwable) {
                    errorFlag = true
                    return null
                }
            }
            return null
        }

        override fun onProgressUpdate(vararg values: Void?) {
            super.onProgressUpdate(*values)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectFlag) Log.d("", "CONNECTION ERROR!")
            if (errorFlag) Log.d("", "ERROR Fetching Status!")
            // TODO: modify UI
            // TODO: if fail restart option
        }

        override fun onCancelled(result: String?) {
            super.onCancelled(result)
            Log.e("RECTASK", isCancelled.toString())
            Log.d("", "Task Cancelled")
        }
    }

}
