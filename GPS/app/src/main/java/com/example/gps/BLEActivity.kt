package com.example.gps

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gps.objects.MyApplication
import com.example.gps.objects.RecyclerAdapter
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import kotlinx.android.synthetic.main.activity_ble.*
import maes.tech.intentanim.CustomIntent


class BLEActivity : AppCompatActivity() {

    //var mApp = MyApplication()
    private var scanFlag: Boolean = false
    var deviceList : ArrayList<BluetoothDevice> = ArrayList()
    var resultsList : ArrayList<ScanResult> = ArrayList()

    private val bleScanner = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            //Log.d("[DEBUG]", "SCAN: ${result?.device?.address} - ${result?.device?.name}")
            if (result != null) {
                if (result.device !in deviceList) {
                    deviceList.add(result.device)
                    resultsList.add(result)
                    select_device_list.adapter?.notifyItemInserted(deviceList.size-1)
                }
                // TODO: If in list replace with new data
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

        select_device_list.apply {
            layoutManager = LinearLayoutManager(this.context)
            adapter = RecyclerAdapter(deviceList, resultsList) { partItem: BluetoothDevice -> partItemClicked(partItem) }
        }
        select_device_list.addItemDecoration(DividerItemDecoration(select_device_list.context, DividerItemDecoration.VERTICAL))
        select_device_list.itemAnimator = SlideInLeftAnimator()
        select_device_list.itemAnimator?.apply { addDuration = 350}
    }

    override fun onStop() {
        super.onStop()
        bluetoothLeScanner.stopScan(bleScanner)
        scanFlag = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.ble_menu, menu);
        return super.onCreateOptionsMenu(menu)
    }

    // Selected Scan bottom
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.scan_btn) {
            if (scanFlag) {
                bluetoothLeScanner.stopScan(bleScanner)
                item.title = "SCAN"
            } else {
                bluetoothLeScanner.startScan(bleScanner)
                item.title = "STOP"
            }
            scanFlag = !scanFlag
        }
        return super.onOptionsItemSelected(item)
    }

    private fun partItemClicked(partItem: BluetoothDevice ) {
        val index = deviceList.indexOf(partItem)
        val currentHolder = select_device_list.findViewHolderForAdapterPosition(index)
        currentHolder?.itemView?.findViewById<Button>(R.id.connect_btn)?.setBackgroundResource(R.drawable.btn_pressed)

        // TODO: Added ble connection functions
        // TODO: Added device to mApp

        println(partItem)
        println(index)
    }

    // Intent back to main activity
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        supportFragmentManager.popBackStack()
        CustomIntent.customType(this, "right-to-left")
        return true
    }
}



