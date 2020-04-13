package com.example.gps.objects

import android.bluetooth.*
import android.content.Context
import android.content.ContextWrapper
import androidx.core.content.ContextCompat.getSystemService

val STATE_DISCONNECTED = 0
val STATE_CONNECTING = 1
val STATE_CONNECTED = 2

class BleService(c: Context, appc: ContextWrapper) {

    var mBluetoothManager: BluetoothManager? = null
    var mBluetoothAdapter: BluetoothAdapter? = null
    var mBluetoothDeviceAddress: String? = null
    var mBluetoothGatt: BluetoothGatt? = null
    var mConnectionState = STATE_DISCONNECTED
    val context: Context = c
    val appc: ContextWrapper = appc


    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) mConnectionState = STATE_CONNECTED
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) mConnectionState = STATE_DISCONNECTED
        }
    }


    fun initialize(): Boolean {
        if (mBluetoothManager == null) {
            mBluetoothManager = appc.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) return false
        }
        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) return false
        return true
    }


    fun connect(address: String?): Boolean {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null || address == null) return false
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress && mBluetoothGatt != null) {
            return if (mBluetoothGatt!!.connect()) {
                mConnectionState = STATE_CONNECTING
                true
            } else false
        }
        if (mBluetoothAdapter.getRemoteDevice(address) == null) return false
        mBluetoothGatt = mBluetoothAdapter.getRemoteDevice(address).connectGatt(context, false, mGattCallback)
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }


    fun disconnect() {
        if (this.mBluetoothAdapter == null || mBluetoothGatt == null) return
        mBluetoothGatt!!.disconnect()
    }


    fun close() {
        if (mBluetoothGatt == null) return
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
    }
}