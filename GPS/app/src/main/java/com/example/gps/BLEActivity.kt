package com.example.gps

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.Button
import com.example.gps.objects.MyApplication
import com.google.android.material.navigation.NavigationView
import maes.tech.intentanim.CustomIntent;

class BLEActivity : AppCompatActivity() {

    var mApp = MyApplication()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        CustomIntent.customType(this, "right-to-left")
        return true
    }




}