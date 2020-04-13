package com.example.gps.objects

import android.bluetooth.BluetoothGatt
import android.bluetooth.le.ScanResult
import java.util.*

class GlobalApplication {

    companion object {
        var gps_device: Device = Device(UUID(0, 0), UUID(0, 0), null)
        var result: ScanResult? = null
        var bleGatt: BluetoothGatt? = null
    }

}

