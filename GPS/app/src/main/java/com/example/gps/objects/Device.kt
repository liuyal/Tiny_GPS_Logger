package com.example.gps.objects

import android.bluetooth.BluetoothDevice
import java.util.*

//https://developer.android.com/reference/android/bluetooth/BluetoothDevice
//https://developer.android.com/reference/android/bluetooth/BluetoothAdapter

class Device(var service_uuid: UUID, var characteristic_uuid: UUID, var device: BluetoothDevice?) {

    var gps_connection_flag: Boolean = false
    var gps_fix_flag: Boolean = false
    var gps_flag: Boolean = false
    var gps_serial_print_flag: Boolean = false
    var gps_ble_print_flag: Boolean = false
    var gps_log_flag: Boolean = false

}
