package com.example.gps.objects

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.IOException

class sqlitedb (context: Context, factory: SQLiteDatabase.CursorFactory?) : SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "bleGPS.db"
        const val TABLE_NAME = "gpsDevice"
        const val COLUMN_NAME = "mac"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val mkTable = "CREATE TABLE $TABLE_NAME($COLUMN_NAME varchar(18)PRIMARY KEY)"
        db.execSQL(mkTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun clearDBMAC() {
        val db = this.writableDatabase
        val clearTable = "DROP TABLE $TABLE_NAME"
        val mkTable = "CREATE TABLE $TABLE_NAME($COLUMN_NAME varchar(18)PRIMARY KEY)"
        try {
            db.execSQL(clearTable)
            db.execSQL(mkTable)
        } catch (e: Throwable){
            db.execSQL(mkTable)
        }
    }

    fun addMAC(mac: String) {
        val values = ContentValues()
        values.put(COLUMN_NAME, mac)
        val db = this.writableDatabase
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getMAC(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }

}