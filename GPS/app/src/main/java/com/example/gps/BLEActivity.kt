package com.example.gps

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.gps.objects.MyApplication
import com.example.gps.objects.RecyclerAdapter
import maes.tech.intentanim.CustomIntent
import kotlinx.android.synthetic.main.activity_ble.*

class BLEActivity : AppCompatActivity() {

    var mApp = MyApplication()

    private var BLEAdapter: BluetoothAdapter? = null
    private lateinit var Devices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1

    companion object {
        val EXTRA_ADDRESS: String = "Device_address"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        BLEAdapter = BluetoothAdapter.getDefaultAdapter()
        if(BLEAdapter == null) {
            Toast.makeText(applicationContext, "Device doesn't support bluetooth!", Toast.LENGTH_SHORT).show()
            return
        }
        if(!BLEAdapter!!.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        select_device_refresh.setOnClickListener{ ScanDeviceList() }

        // Add back to home button to action bar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }



    private fun ScanDeviceList() {

        val SCAN_PERIOD : Long = 10000

        Devices = BLEAdapter!!.bondedDevices


        val temp = BLEAdapter!!.bluetoothLeScanner

        val list : ArrayList<BluetoothDevice> = ArrayList()

        if (!Devices.isEmpty()) {
            for (device: BluetoothDevice in Devices) {
                list.add(device)
                println(device)
            }
        }
        else {
            Toast.makeText(applicationContext, "No bluetooth devices found", Toast.LENGTH_SHORT).show()
        }

        select_device_list.layoutManager = LinearLayoutManager(this)
        select_device_list.adapter = RecyclerAdapter(list)
        //select_device_list.addItemDecoration(SimpleDividerItemDecoration(this))


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                if (BLEAdapter!!.isEnabled) {
                    Toast.makeText(applicationContext, "Bluetooth has been enabled", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "Bluetooth has been disabled", Toast.LENGTH_SHORT).show()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(applicationContext, "Bluetooth enabling has been canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Intent overide back to main activity
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        CustomIntent.customType(this, "right-to-left")
        return true
    }
}


