package com.example.gps.objects

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SqliteDB(context: Context, factory: SQLiteDatabase.CursorFactory?) : SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "bleGPS.db"
        const val macTable: String = "macTable"
        const val macColumn: String = "mac"
    }


    override fun onCreate(db: SQLiteDatabase) {
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }


    private fun createTable(table: String) {
        val db = this.writableDatabase
        if (table == macTable){
            db.execSQL("CREATE TABLE $macTable($macColumn varchar(18) PRIMARY KEY)")
        }
    }


    fun clearTable(table: String): Boolean {
        val db = this.writableDatabase
        try {
            db.execSQL("DROP TABLE $table")
            this.createTable(table)
        } catch (e: Throwable){
            this.createTable(table)
        }
        return true
    }


    // TODO: Make into generic functions
    fun insertDB(mac: String) {
        val values = ContentValues()
        val db = this.writableDatabase
        values.put(macColumn, mac)
        db.insert(macTable, null, values)
        db.close()
    }


    // TODO: Make into generic functions
    fun selectFromDB(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $macTable", null)
    }

}