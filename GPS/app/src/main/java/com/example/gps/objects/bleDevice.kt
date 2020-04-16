package com.example.gps.objects

import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.ContextWrapper
import android.os.AsyncTask
import android.util.Log
import java.util.*

//https://developer.android.com/reference/android/bluetooth/BluetoothGatt

const val STATE_DISCONNECTED = 0
const val STATE_CONNECTING = 1
const val STATE_CONNECTED = 2
const val TIME_OUT = 5000

val SERVICE_UUID = UUID.fromString("000ffdf4-68d9-4e48-a89a-219e581f0d64")

//TODO: Added characteristic functions read/write
class bleDevice(c: Context, appcontext: ContextWrapper) {

    var context: Context = c
    var applicationcontext: ContextWrapper = appcontext
    var connectionState = STATE_DISCONNECTED

    var bleManager: BluetoothManager? = null
    var bleAdapter: BluetoothAdapter? = null
    var bleGATT: BluetoothGatt? = null

    var device: BluetoothDevice? = null
    var scanResult: ScanResult? = null
    var bleAddress: String? = null

    var service: BluetoothGattService? = null
    var characteristic: BluetoothGattCharacteristic? = null
    var transactionSuccess: Boolean = false

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
                transactionSuccess = true
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onCharacteristicWrite received: $status")
                transactionSuccess = true
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onDescriptorRead received: $status")
                transactionSuccess = true
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("", "onDescriptorWrite received: $status")
                transactionSuccess = true
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
        if (this.bleManager == null) {
            this.bleManager = applicationcontext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (this.bleManager == null) return false
        }
        this.bleAdapter = this.bleManager!!.adapter
        if (this.bleAdapter == null) return false
        return true
    }


    fun connect(address: String?): Boolean {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null || address == null) return false
        if (this.bleAddress != null && address == this.bleAddress && this.bleGATT != null) {
            if (this.bleGATT!!.connect()) {
                this.connectionState = STATE_CONNECTING
                return true
            } else return false
        }
        if (mBluetoothAdapter.getRemoteDevice(address) == null) return false
        this.bleGATT = mBluetoothAdapter.getRemoteDevice(address).connectGatt(context, false, mGattCallback)
        this.bleAddress = address
        this.connectionState = STATE_CONNECTING
        checkSC()
        updateDBMAC(address)
        return true
    }


    fun disconnect() {
        if (this.bleAdapter == null || this.bleGATT == null) return
        this.bleGATT!!.disconnect()
    }


    fun close() {
        if (this.bleGATT == null) return
        this.bleGATT!!.close()
        this.bleGATT = null
    }

    private fun checkSC(timeout: Boolean = true): Boolean {
        var serviceList = GlobalApplication.BLE!!.bleGATT?.services
        val start = System.currentTimeMillis()

        while (serviceList != null && serviceList.size < 1) {
            GlobalApplication.BLE!!.bleGATT?.discoverServices()
            serviceList = GlobalApplication.BLE!!.bleGATT?.services
            if (timeout && System.currentTimeMillis() - start > TIME_OUT) break
        }

        if (serviceList == null || serviceList.size < 1) return false
        for (serviceItem in serviceList) {
            if (serviceItem.uuid == SERVICE_UUID) {
                this.service = serviceItem
                if (serviceItem.characteristics.size >= 1) {
                    this.characteristic = serviceItem.characteristics[0]
                    break
                }
            }
        }
        if (this.service == null || this.characteristic == null) return false
        return true
    }

    fun writeValue(value: ByteArray): Boolean {
        val start = System.currentTimeMillis()
        this.transactionSuccess = false

        val serviceCheck: Boolean = if (this.service == null || this.characteristic == null) {
            GlobalApplication.BLE?.checkSC()!!
        } else true

        if (serviceCheck!!) {
            this.characteristic!!.value = value
            GlobalApplication.BLE?.bleGATT?.writeCharacteristic(this.characteristic)
        } else return false

        while (!this.transactionSuccess) {
            if (System.currentTimeMillis() - start > TIME_OUT) return false
        }
        this.transactionSuccess = false
        return true
    }

    fun readValue(): ByteArray? {
        val start = System.currentTimeMillis()
        this.transactionSuccess = false

        val serviceCheck: Boolean = if (this.service == null || this.characteristic == null) {
            GlobalApplication.BLE?.checkSC()!!
        } else true

        if (serviceCheck) {
            GlobalApplication.BLE?.bleGATT?.readCharacteristic(this.characteristic)
        } else return null

        while (!this.transactionSuccess) {
            if (System.currentTimeMillis() - start > TIME_OUT) return null
        }
        this.transactionSuccess = false
        return this.characteristic?.value
    }

    private fun updateDBMAC(address: String?) {
        val dbHandler = sqlitedb(context, null)
        dbHandler.clearDBMAC()
        if (address != null) dbHandler.addMAC(address)
    }

    fun loadDBMAC(): String {
        val dbHandler = sqlitedb(context, null)
        val cursor = dbHandler.getMAC()
        cursor!!.moveToFirst()
        this.bleAddress = cursor.getString(cursor.getColumnIndex(sqlitedb.COLUMN_NAME))
        return this.bleAddress!!
    }
}