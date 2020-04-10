package com.example.gps.objects

import android.app.Application

class MyApplication {

    companion object {
        var gps_device : Device = Device("0", "0", booleanArrayOf(false, false, false, false, false, false))
    }

}

