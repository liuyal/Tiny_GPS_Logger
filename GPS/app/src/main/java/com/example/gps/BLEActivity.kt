package com.example.gps

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.Button
import com.example.gps.objects.MyApplication
import com.google.android.material.navigation.NavigationView

class BLEActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

    }




}