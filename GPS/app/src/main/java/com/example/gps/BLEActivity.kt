package com.example.gps

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gps.objects.BleService
import com.example.gps.objects.GlobalApplication
import com.example.gps.objects.ScanAdapter
import com.example.gps.ui.assets.PopUpWindow
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import kotlinx.android.synthetic.main.activity_ble.*
import maes.tech.intentanim.CustomIntent
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class BLEActivity : AppCompatActivity() {

    private var scanFlag: Boolean = false
    private var deviceList: ArrayList<BluetoothDevice> = ArrayList()
    private var resultsList: ArrayList<ScanResult> = ArrayList()
    private var BLE: BleService? = null


    private val bleScanner = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null) {
                if (result.device !in deviceList) {
                    deviceList.add(result.device)
                    resultsList.add(result)
                    select_device_list.adapter?.notifyItemInserted(deviceList.size - 1)
                }
                if (result.device in deviceList) {
                    resultsList[deviceList.indexOf(result.device)] = result
                    select_device_list.adapter?.notifyItemChanged(deviceList.indexOf(result.device))
                }
            }
            Log.d("[DEBUG]", "SCAN: ${result?.device?.address} - ${result?.device?.name}")
        }
    }


    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            BLE?.mBluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            BLE?.mBluetoothAdapter = BLE?.mBluetoothManager!!.adapter
            return BLE?.mBluetoothAdapter!!.bluetoothLeScanner
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
        select_device_list.layoutManager = LinearLayoutManager(select_device_list.context)
        select_device_list.adapter = ScanAdapter(deviceList, resultsList) { partItem: BluetoothDevice -> partItemClicked(partItem) }
        select_device_list.itemAnimator = SlideInLeftAnimator()
        select_device_list.itemAnimator?.addDuration = 350
        select_device_list.addItemDecoration(DividerItemDecoration(select_device_list.context, DividerItemDecoration.VERTICAL))
    }


    override fun onStart() {
        super.onStart()
        BLE = BleService(this, applicationContext as ContextWrapper)
        BLE?.initialize()
        BLE?.mBluetoothGatt = GlobalApplication.bleGatt
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


    // Selected Scan button
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Toast.makeText(applicationContext, "BlueTooth is not supported!", Toast.LENGTH_SHORT).show()
            return false
        } else if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            Toast.makeText(applicationContext, "BlueTooth is not enabled!", Toast.LENGTH_SHORT).show()
            return false
        } else {
            BLE?.disconnect()
            BLE?.close()
            if (item.itemId == R.id.scan_btn) {
                if (scanFlag) {
                    bluetoothLeScanner.stopScan(bleScanner)
                    item.title = "SCAN"
                } else {
                    deviceList.removeAll(deviceList)
                    resultsList.removeAll(resultsList)
                    select_device_list.adapter?.notifyDataSetChanged()
                    bluetoothLeScanner.startScan(bleScanner)
                    item.title = "STOP"
                }
                scanFlag = !scanFlag
            }
            return super.onOptionsItemSelected(item)
        }
    }


    private fun partItemClicked(partItem: BluetoothDevice) {
        bluetoothLeScanner.stopScan(bleScanner)
        var isConnected = false
        val index = deviceList.indexOf(partItem)
        val serviceUUID: UUID? = resultsList[index].scanRecord?.serviceUuids?.get(0)?.uuid
        select_device_list.findViewHolderForAdapterPosition(index)?.itemView?.findViewById<Button>(R.id.connect_btn)?.setBackgroundResource(R.drawable.btn_pressed)

        Log.d("[DEBUG]", "Select: $index|$partItem|$serviceUUID")

        try {
            isConnected = BLE?.connect(partItem.address.toString())!!
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (isConnected && serviceUUID != null) {
            // TODO: add success popup
            //https://developer.android.com/reference/android/widget/PopupWindow#xml-attributes
            GlobalApplication.gps_device.service_uuid = serviceUUID
            GlobalApplication.result = resultsList[index]
            GlobalApplication.bleGatt = BLE?.mBluetoothGatt!!

            onSupportNavigateUp()
        } else if (scanFlag){
            Toast.makeText(applicationContext, "Invalid Device!", Toast.LENGTH_SHORT).show()
            select_device_list.findViewHolderForAdapterPosition(index)?.itemView?.findViewById<Button>(R.id.connect_btn)?.setBackgroundResource(R.drawable.btn_unpressed)
            bluetoothLeScanner.startScan(bleScanner)
        } else {
            Toast.makeText(applicationContext, "Invalid Device!", Toast.LENGTH_SHORT).show()
            select_device_list.findViewHolderForAdapterPosition(index)?.itemView?.findViewById<Button>(R.id.connect_btn)?.setBackgroundResource(R.drawable.btn_unpressed)
        }
    }


    // back to main activity
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        supportFragmentManager.popBackStack()
        CustomIntent.customType(this, "right-to-left")
        return true
    }
}


