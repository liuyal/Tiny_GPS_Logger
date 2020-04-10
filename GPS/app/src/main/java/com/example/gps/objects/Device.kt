package com.example.gps.objects

import android.app.Application

class Device(service_uuid:String, char_uuid:String, status:BooleanArray) {

    var service_uuid : String = service_uuid
    var char_uuid : String = char_uuid
    var status : BooleanArray = status

    init{}
}

//var gps_device : Device = Device("0", "0", booleanArrayOf(false, false, false, false, false, false))
