package com.gps

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.gps.objects.GlobalApp
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import kotlinx.android.synthetic.main.activity_ble.*
import maes.tech.intentanim.CustomIntent
import java.util.*
import kotlin.collections.ArrayList

class BLEActivity : AppCompatActivity() {

    private var scanFlag: Boolean = false
    private var deviceList: ArrayList<BluetoothDevice> = ArrayList()
    private var resultsList: ArrayList<ScanResult> = ArrayList()

    private var progressBar: ProgressBar? = null

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
            Log.d("BLEACT", "SCAN: ${result?.device?.address} - ${result?.device?.name}")
        }
    }

    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            GlobalApp.BLE?.bleManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            GlobalApp.BLE?.bleAdapter = GlobalApp.BLE?.bleManager!!.adapter
            return GlobalApp.BLE?.bleAdapter!!.bluetoothLeScanner
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
        supportActionBar!!.elevation = 0F
        GlobalApp.BLE?.context = this
        GlobalApp.BLE?.applicationContext = applicationContext as ContextWrapper
        select_device_list.layoutManager = LinearLayoutManager(select_device_list.context)
        select_device_list.adapter = BLEScanAdapter(deviceList, resultsList) { partItem: BluetoothDevice -> partItemClicked(partItem) }
        select_device_list.itemAnimator = SlideInLeftAnimator()
        select_device_list.itemAnimator?.addDuration = 200
        select_device_list.addItemDecoration(DividerItemDecoration(select_device_list.context, DividerItemDecoration.VERTICAL))
    }

    override fun onStop() {
        super.onStop()
        this.scanFlag = false
        if (BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled) {
            bluetoothLeScanner.stopScan(bleScanner)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.ble_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Toast.makeText(applicationContext, "BlueTooth is not supported!", Toast.LENGTH_SHORT).show()
            return false
        } else if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            Toast.makeText(applicationContext, "BlueTooth is not enabled!", Toast.LENGTH_SHORT).show()
            return false
        } else {
            GlobalApp.BLE?.initialize()
            GlobalApp.BLE?.disconnect()
            GlobalApp.BLE?.close()
            if (item.itemId == R.id.scan_btn) {
                if (this.scanFlag) {
                    this.bluetoothLeScanner.stopScan(bleScanner)
                    item.title = getString(R.string.scan)
                } else {
                    this.deviceList.removeAll(deviceList)
                    this.resultsList.removeAll(resultsList)
                    select_device_list.adapter?.notifyDataSetChanged()
                    this.bluetoothLeScanner.startScan(bleScanner)
                    item.title = getString(R.string.stop_scan)
                }
                this.scanFlag = !this.scanFlag
            }
            return super.onOptionsItemSelected(item)
        }
    }

    @UiThread
    private fun partItemClicked(partItem: BluetoothDevice) {
        var isConnected = false
        val index = deviceList.indexOf(partItem)
        val serviceUUID: UUID? = resultsList[index].scanRecord?.serviceUuids?.get(0)?.uuid
        progressBar = findViewById(R.id.progress_circularBar)

        Thread(Runnable {
            this.runOnUiThread { progressBar?.visibility = View.VISIBLE }

            try {
                this.bluetoothLeScanner.stopScan(this.bleScanner)
                isConnected = GlobalApp.BLE?.connect(partItem.address.toString())!!
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            Thread.sleep(1000)
            if (isConnected && serviceUUID != null) {
                GlobalApp.BLE?.device = partItem
                GlobalApp.BLE?.scanResult = this.resultsList[index]
                this.runOnUiThread { createDialog(this, "Success!", "Connected To BLE Device!", "OK") }
            } else if (this.scanFlag) {
                this.runOnUiThread { Toast.makeText(applicationContext, "Invalid Device!", Toast.LENGTH_SHORT).show() }
                this.bluetoothLeScanner.startScan(bleScanner)
            } else {
                this.runOnUiThread { Toast.makeText(applicationContext, "Invalid Device!", Toast.LENGTH_SHORT).show() }
            }
            this.runOnUiThread { progressBar?.visibility = View.GONE }
        }).start()
    }

    @UiThread
    private fun createDialog(c: Context, title: String, msg: String, button: String) {
        val builder = AlertDialog.Builder(c)
        builder.setTitle(title)
        builder.setMessage(msg)
        builder.setPositiveButton(button) { _, _ ->
            builder.create().dismiss()
            onSupportNavigateUp()
        }
        builder.create().show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        supportFragmentManager.popBackStack()
        CustomIntent.customType(this, "right-to-left")
        return true
    }
}


// https://stackoverflow.com/questions/32759785/is-it-possible-to-allocate-these-folders-in-another-place