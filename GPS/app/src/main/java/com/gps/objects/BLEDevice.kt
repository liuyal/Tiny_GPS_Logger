package com.gps.objects

import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.util.Log
import android.widget.Toast
import java.util.*

//https://developer.android.com/reference/android/bluetooth/BluetoothGatt

const val GPS_CHECK_CODE: Byte = 0x00
const val GET_GPS_STATUS_CODE: Byte = 0x01
const val SET_GPS_ON_CODE: Byte = 0x02
const val SET_GPS_OFF_CODE: Byte = 0x03
const val SET_GPG_LOGGING_ON_CODE: Byte = 0x04
const val SET_GPG_LOGGING_OFF_CODE: Byte = 0x05
const val SET_GPS_BLE_PRINT_ON_CODE: Byte = 0x06
const val SET_GPS_BLE_PRINT_OFF_CODE: Byte = 0x07
const val GET_GPS_DATA_CODE: Byte = 0x08
const val LIST_LOG_FILES_CODE: Byte = 0x09
const val READ_LOG_FILE_CODE: Byte = 0x0a
const val GET_SDCARD_STATUS_CODE: Byte = 0x0b
const val GPS_REBOOT_CODE: Byte = 0x0c
const val GPS_RESET_CODE: Byte = 0x0d

const val NUMBER_OF_FLAGS: Int = 6
const val GPS_CONNECTION_FLAG_INDEX: Int = 0
const val GPS_ON_FLAG_INDEX: Int = 1
const val GPS_FIX_FLAG_INDEX: Int = 2
const val GPS_SERIAL_PRINT_FLAG_INDEX: Int = 3
const val GPS_BLE_PRINT_FLAG_INDEX: Int = 4
const val GPS_LOGGING_FLAG_INDEX: Int = 5

const val LOCATION_IS_VALID_INDEX: Int = 0
const val LOCATION_IS_UPDATED_INDEX: Int = 1
const val LOCATION_AGE_INDEX: Int = 2
const val LOCATION_LAT_INDEX: Int = 3
const val LOCATION_LNG_INDEX: Int = 4
const val DATE_YEAR_INDEX: Int = 5
const val DATE_MONTH_INDEX: Int = 6
const val DATE_DAY_INDEX: Int = 7
const val TIME_HOUR_INDEX: Int = 8
const val TIME_MINUTE_INDEX: Int = 9
const val TIME_SECOND_INDEX: Int = 10
const val SATELLITES_VALUE_INDEX: Int = 11
const val SPEED_KMPH_INDEX: Int = 12
const val COURSE_DEG_INDEX: Int = 13
const val ALTITUDE_METERS_INDEX: Int = 14
const val HDOP_VALUE_INDEX: Int = 15

const val STATE_DISCONNECTED: Int = 0
const val STATE_CONNECTING: Int = 1
const val STATE_CONNECTED: Int = 2

const val TIME_OUT: Int = 1000
const val DEVICE_CODE_NAME: String = "ESP32_GPS"
const val oneByte: Byte = 1

val SERVICE_UUID: UUID = UUID.fromString("000ffdf4-68d9-4e48-a89a-219e581f0d64")
val CHARACTERISTIC_UUID: UUID = UUID.fromString("44a80b83-c605-4406-8e50-fc42f03b6d38")

class BLEDevice(c: Context, var applicationContext: ContextWrapper) {

    var context: Context = c
    var connectionState = STATE_DISCONNECTED

    var bleManager: BluetoothManager? = null
    var bleAdapter: BluetoothAdapter? = null
    var bleGATT: BluetoothGatt? = null

    var device: BluetoothDevice? = null
    var scanResult: ScanResult? = null
    var bleAddress: String? = null
    var gpsData: String? = null
    var transactionSuccess: Boolean = false
    var gpsStatusFlags: BooleanArray? = BooleanArray(NUMBER_OF_FLAGS)

    private var service: BluetoothGattService? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var dbHandler: SqliteDB? = null


    private val mGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED
                Log.d("BLE", "STATE_CONNECTED")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED
                Log.d("BLE", "STATE_DISCONNECTED")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "onServicesDiscovered received: $status")
            } else {
                Log.d("BLE", "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.d("BLE", "onCharacteristicChanged received")
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "onCharacteristicRead received: $status")
                transactionSuccess = true
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "onCharacteristicWrite received: $status")
                transactionSuccess = true
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "onDescriptorRead received: $status")
                transactionSuccess = true
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "onDescriptorWrite received: $status")
                transactionSuccess = true
            }
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(gatt, txPhy, rxPhy, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "onPhyRead received: $status")
            }
        }

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "onPhyUpdate received: $status")
            }
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            super.onReliableWriteCompleted(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "onReliableWriteCompleted received: $status")
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "onReadRemoteRssi received: $status, RSSI: $rssi")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "onMtuChanged received: $status, MTU: $mtu")
            }
        }
    }

    fun initialize(): Boolean {
        if (this.bleManager == null) {
            this.bleManager = this.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (this.bleManager == null) return false
        }
        this.bleAdapter = this.bleManager!!.adapter
        if (this.bleAdapter == null) return false
        dbHandler = SqliteDB(context, null)
        this.gpsStatusFlags?.fill(false, 0, NUMBER_OF_FLAGS)
        this.loadDBMAC()
        return true
    }

    fun connect(address: String?): Boolean {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null || address == null || !mBluetoothAdapter.isEnabled) return false
        if (this.bleAddress != null && address == this.bleAddress && this.bleGATT != null) {
            return if (this.bleGATT!!.connect()) {
                this.connectionState = STATE_CONNECTING
                true
            } else false
        }
        if (mBluetoothAdapter.getRemoteDevice(address) == null) return false
        this.bleGATT = mBluetoothAdapter.getRemoteDevice(address).connectGatt(context, false, mGattCallback)
        this.bleAddress = address
        this.connectionState = STATE_CONNECTING
        val checkA = serviceChecks()
        val checkB = gpsTagCheck()
        if (!checkA || !checkB) return false
        updateDBMAC(address)
        return true
    }

    fun disconnect() {
        this.gpsStatusFlags?.fill(false, 0, NUMBER_OF_FLAGS)
        this.gpsData = ""
        if (this.bleAdapter == null || this.bleGATT == null) return
        this.bleGATT!!.disconnect()
    }

    fun close() {
        if (this.bleGATT == null) return
        this.bleGATT!!.close()
        this.bleGATT = null
    }

    private fun updateDBMAC(address: String?) {
        dbHandler?.clearTable(SqliteDB.macTable)
        if (address != null) {
            dbHandler?.insertDB(SqliteDB.macTable, arrayListOf(SqliteDB.macColumn), arrayListOf(address))
        }
    }

    private fun loadDBMAC(): String? {
        return try {
            val cursor: Cursor? = dbHandler?.selectFromDB(SqliteDB.macTable)
            cursor!!.moveToFirst()
            this.bleAddress = cursor.getString(cursor.getColumnIndex(SqliteDB.macColumn))
            this.bleAddress!!
        } catch (e: Throwable) {
            this.bleAddress = ""
            this.bleAddress
        }
    }

    private fun serviceChecks(timeout: Boolean = true): Boolean {
        val start = System.currentTimeMillis()
        var serviceList = GlobalApp.BLE!!.bleGATT?.services
        var foundService = false
        var foundCharacteristics = false
        while (serviceList != null && serviceList.size < 1 && !foundService && !foundCharacteristics) {
            GlobalApp.BLE!!.bleGATT?.discoverServices()
            serviceList = GlobalApp.BLE!!.bleGATT?.services
            if (serviceList != null) for (item in serviceList) {
                if (item.uuid == SERVICE_UUID) {
                    foundService = true
                    if (item.characteristics != null) for (characterItem in item.characteristics) {
                        foundCharacteristics = characterItem.uuid == CHARACTERISTIC_UUID
                    }
                } else foundService = false
            }
            if (timeout && System.currentTimeMillis() - start > 3 * TIME_OUT) break
        }
        if (serviceList == null || serviceList.size < 1) return false
        for (serviceItem in serviceList) {
            if (serviceItem.uuid == SERVICE_UUID) {
                this.service = serviceItem
                if (serviceItem.characteristics != null) for (characterItem in serviceItem.characteristics) {
                    if (characterItem.uuid == CHARACTERISTIC_UUID) {
                        this.characteristic = characterItem
                        break
                    }
                }
            }
        }
        if (this.service == null || this.characteristic == null) return false
        return true
    }

    private fun gpsTagCheck(): Boolean {
        writeValue(byteArrayOf(GPS_CHECK_CODE))
        val returnVal = readValue()
        if (returnVal != null && returnVal.toString(Charsets.UTF_8).contains(DEVICE_CODE_NAME, ignoreCase = true)) {
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
        } else return false
        return true
    }

    private fun writeValue(value: ByteArray): Boolean {
        this.transactionSuccess = false
        val start = System.currentTimeMillis()
        val serviceCheck: Boolean = if (this.service == null || this.characteristic == null) {
            GlobalApp.BLE?.serviceChecks()!!
        } else true

        if (serviceCheck) {
            this.characteristic!!.value = value
            GlobalApp.BLE?.bleGATT?.writeCharacteristic(this.characteristic)
        } else return false

        while (!this.transactionSuccess) {
            if (System.currentTimeMillis() - start > 2 * TIME_OUT) return false
        }
        this.transactionSuccess = false
        return true
    }

    private fun readValue(): ByteArray? {
        this.transactionSuccess = false
        val start = System.currentTimeMillis()
        val serviceCheck: Boolean = if (this.service == null || this.characteristic == null) {
            GlobalApp.BLE?.serviceChecks()!!
        } else true

        if (serviceCheck) {
            GlobalApp.BLE?.bleGATT?.readCharacteristic(this.characteristic)
        } else return null

        while (!this.transactionSuccess) {
            if (System.currentTimeMillis() - start > 2 * TIME_OUT) return null
        }
        this.transactionSuccess = false
        return this.characteristic?.value
    }

    private fun Int.toBoolean(): Boolean {
        return this == 1
    }

    fun fetchDeviceStatus(): Boolean {
        writeValue(byteArrayOf(GET_GPS_STATUS_CODE))
        val returnVal = readValue()
        this.gpsStatusFlags?.fill(false, 0, NUMBER_OF_FLAGS)
        if (returnVal != null && returnVal.size == NUMBER_OF_FLAGS) {
            this.gpsStatusFlags?.set(GPS_CONNECTION_FLAG_INDEX, returnVal[GPS_CONNECTION_FLAG_INDEX].toInt().toBoolean())
            this.gpsStatusFlags?.set(GPS_FIX_FLAG_INDEX, returnVal[GPS_FIX_FLAG_INDEX].toInt().toBoolean())
            this.gpsStatusFlags?.set(GPS_ON_FLAG_INDEX, returnVal[GPS_ON_FLAG_INDEX].toInt().toBoolean())
            this.gpsStatusFlags?.set(GPS_SERIAL_PRINT_FLAG_INDEX, returnVal[GPS_SERIAL_PRINT_FLAG_INDEX].toInt().toBoolean())
            this.gpsStatusFlags?.set(GPS_BLE_PRINT_FLAG_INDEX, returnVal[GPS_BLE_PRINT_FLAG_INDEX].toInt().toBoolean())
            this.gpsStatusFlags?.set(GPS_LOGGING_FLAG_INDEX, returnVal[GPS_LOGGING_FLAG_INDEX].toInt().toBoolean())
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
        } else return false
        return true
    }

    fun GPSon(): Boolean {
        writeValue(byteArrayOf(SET_GPS_ON_CODE))
        val returnVal = readValue()
        if (returnVal != null && returnVal[0] == oneByte && returnVal[1] == oneByte) {
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
        } else return false
        return true
    }

    fun GPSoff(): Boolean {
        writeValue(byteArrayOf(SET_GPS_OFF_CODE))
        val returnVal = readValue()
        if (returnVal != null && returnVal[0] == oneByte && returnVal[1] == oneByte) {
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
        } else return false
        return true
    }

    fun GPSloggingOn(): Boolean {
        writeValue(byteArrayOf(SET_GPG_LOGGING_ON_CODE))
        val returnVal = readValue()
        if (returnVal != null && returnVal[0] == oneByte && returnVal[1] == oneByte) {
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
        } else return false
        return true
    }

    fun GPSloggingOff(): Boolean {
        writeValue(byteArrayOf(SET_GPG_LOGGING_OFF_CODE))
        val returnVal = readValue()
        if (returnVal != null && returnVal[0] == oneByte && returnVal[1] == oneByte) {
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
        } else return false
        return true
    }

    fun BLEPrintOn(): Boolean {
        writeValue(byteArrayOf(SET_GPS_BLE_PRINT_ON_CODE))
        val returnVal = readValue()
        if (returnVal != null && returnVal[0] == oneByte && returnVal[1] == oneByte) {
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
        } else return false
        return true
    }

    fun BLEPrintOff(): Boolean {
        writeValue(byteArrayOf(SET_GPS_BLE_PRINT_OFF_CODE))
        val returnVal = readValue()
        if (returnVal != null && returnVal[0] == oneByte && returnVal[1] == oneByte) {
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
        } else return false
        return true
    }

    fun fetchGPSData(): String {
        writeValue(byteArrayOf(GET_GPS_DATA_CODE))
        val returnVal = readValue()
        return if (returnVal != null) {
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
            this.gpsData = returnVal.toString(Charsets.UTF_8)
            returnVal.toString(Charsets.UTF_8)
        } else {
            this.gpsData = ""
            ""
        }
    }

    fun listLogFiles(): String {
        writeValue(byteArrayOf(LIST_LOG_FILES_CODE))
        val returnVal = readValue()
        if (returnVal != null) {
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
        } else return ""
        return returnVal.toString(Charsets.UTF_8)
    }

    fun readLogFile(index: Int): String {
        val indexByte: Byte = index.toByte()
        writeValue(byteArrayOf(READ_LOG_FILE_CODE, indexByte))
        val returnVal = readValue()

        return returnVal?.toString(Charsets.UTF_8) ?: ""
    }

    fun SDCardStatus(): String {
        writeValue(byteArrayOf(GET_SDCARD_STATUS_CODE))
        val returnVal = readValue()
        if (returnVal != null) {
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
        } else return ""
        return returnVal.toString(Charsets.UTF_8)
    }

    fun GPSReboot(): Boolean {
        writeValue(byteArrayOf(GPS_REBOOT_CODE))
        val returnVal = readValue()
        if (returnVal != null && returnVal[0] == oneByte && returnVal[1] == oneByte) {
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
        } else return false
        return true
    }

    fun GPSReset(): Boolean {
        writeValue(byteArrayOf(GPS_RESET_CODE))
        val returnVal = readValue()
        if (returnVal != null && returnVal[0] == oneByte && returnVal[1] == oneByte) {
            Log.d("BLE", returnVal.contentToString())
            Log.d("BLE", returnVal.toString(Charsets.UTF_8))
        } else return false
        return true
    }
}