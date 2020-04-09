package com.example.tiny_gps

import android.app.ListActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.example.tiny_gps.com.example.tiny_gps.ApClass
import android.os.Handler

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

//var box : ApClass = ApClass("0", "0", booleanArrayOf(false, false, false, false, false, false))

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        (this as AppCompatActivity).supportActionBar?.title = "GPS"


    }
















}






























