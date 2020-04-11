package com.example.gps

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.gps.objects.MyApplication
import kotlinx.android.synthetic.main.activity_ble.*
import maes.tech.intentanim.CustomIntent

class BLEActivity : AppCompatActivity() {

    var mApp = MyApplication()
    var scan_flag: Boolean = false
    var device_list  : ArrayList<BluetoothDevice> = ArrayList()

    private val bleScanner = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            Log.d("ScanDeviceActivity", "onScanResult(): ${result?.device?.address} - ${result?.device?.name}")
            if (result != null) {






            }
        }
    }

    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            return bluetoothAdapter.bluetoothLeScanner
        }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> when (grantResults) {
                intArrayOf(PackageManager.PERMISSION_GRANTED) -> {
                    bluetoothLeScanner.startScan(bleScanner)
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.scan_btn) {
            if (scan_flag) {
                bluetoothLeScanner.stopScan(bleScanner)
                item.setTitle("SCAN")
            }
            else {
                bluetoothLeScanner.startScan(bleScanner)
                item.setTitle("STOP")
            }
            scan_flag = !scan_flag
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.ble_menu, menu);
        return super.onCreateOptionsMenu(menu)
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


//        select_device_list.layoutManager = LinearLayoutManager(this)
//        select_device_list.adapter = RecyclerAdapter(list)
//        select_device_list.adapter?.notifyDataSetChanged()
//        select_device_list.addItemDecoration(SimpleDividerItemDecoration(this))
