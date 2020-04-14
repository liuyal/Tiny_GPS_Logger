package com.example.gps.objects

import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import java.util.*

//https://developer.android.com/reference/android/bluetooth/BluetoothGatt

const val STATE_DISCONNECTED = 0
const val STATE_CONNECTING = 1
const val STATE_CONNECTED = 2

//TODO: Added characteristic functions read/write
class bleDevice(c: Context, appcontext: ContextWrapper) {

    var context: Context = c
    var applicationcontext: ContextWrapper = appcontext

    var bleManager: BluetoothManager? = null
    var bleAdapter: BluetoothAdapter? = null
    var bleGATT: BluetoothGatt? = null

    var device: BluetoothDevice? = null
    var scanResult: ScanResult? = null
    var bleAddress: String? = null

    var connectionState = STATE_DISCONNECTED
    var service_uuid: UUID? = UUID(0,0)
    var characteristic_uuid: UUID? = UUID(0,0)

    var gps_connection_flag: Boolean = false
    var gps_fix_flag: Boolean = false
    var gps_on_flag: Boolean = false
    var gps_serial_print_flag: Boolean = false
    var gps_ble_print_flag: Boolean = false
    var gps_log_flag: Boolean = false

    private val mGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED
                Log.d("", "BLE STATE_CONNECTED")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED
                Log.d("", "BLE STATE_DISCONNECTED")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onServicesDiscovered received: $status")
            } else {
                Log.d("", "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d("", "onCharacteristicChanged received")
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onCharacteristicRead received: $status")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onCharacteristicWrite received: $status")
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onDescriptorRead received: $status")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onDescriptorWrite received: $status")
            }
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(gatt, txPhy, rxPhy, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onPhyRead received: $status")
            }
        }

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onPhyUpdate received: $status")
            }
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onReliableWriteCompleted received: $status")
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onReadRemoteRssi received: $status, RSSI: $rssi")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onMtuChanged received: $status, MTU: $mtu")
            }
        }
    }


    fun initialize(): Boolean {
        if (bleManager == null) {
            bleManager = applicationcontext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (bleManager == null)  return false
        }
        bleAdapter = bleManager!!.adapter
        if (bleAdapter == null) return false
        return true
    }


    fun connect(address: String?): Boolean {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null || address == null) return false
        if (bleAddress != null && address == bleAddress && bleGATT != null) {
            if (bleGATT!!.connect()) {
                connectionState = STATE_CONNECTING
                return true
            } else return false
        }
        if (mBluetoothAdapter.getRemoteDevice(address) == null) return false
        bleGATT = mBluetoothAdapter.getRemoteDevice(address).connectGatt(context, false, mGattCallback)
        bleAddress = address
        connectionState = STATE_CONNECTING
        return true
    }


    fun disconnect() {
        if (this.bleAdapter == null || bleGATT == null) return
        bleGATT!!.disconnect()
    }


    fun close() {
        if (bleGATT == null) return
        bleGATT!!.close()
        bleGATT = null
    }

}