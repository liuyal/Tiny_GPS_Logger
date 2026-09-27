package com.gps.objects

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
        const val GPSDataTable: String = "GPSDataTable"
        const val GPSDataColumn: String = "data"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $macTable($macColumn varchar(18) PRIMARY KEY)")
        db.execSQL("CREATE TABLE IF NOT EXISTS $GPSDataTable($GPSDataColumn varchar(80) PRIMARY KEY)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $macTable")
        db.execSQL("DROP TABLE IF EXISTS $GPSDataTable")
        onCreate(db)
    }

    private fun createTable(table: String) {
        val db = this.writableDatabase
        if (table == macTable) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $macTable($macColumn varchar(18) PRIMARY KEY)")
        } else if (table == GPSDataTable) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $GPSDataTable($GPSDataColumn varchar(80) PRIMARY KEY)")
        }
    }

    fun clearTable(table: String): Boolean {
        val db = this.writableDatabase
        try {
            db.execSQL("DROP TABLE IF EXISTS $table")
            this.createTable(table)
        } catch (e: Throwable) {
            this.createTable(table)
        }
        return true
    }

    fun insertDB(table: String, columns: ArrayList<String>, data: ArrayList<String>) {
        val values: ArrayList<ContentValues> = ArrayList()
        val db = this.writableDatabase
        for (i in 0 until data.size) {
            val node = ContentValues()
            node.put(columns[i], data[i])
            values.add(node)
        }
        for (i in 0 until data.size) {
            db.insert(table, null, values[i])
            db.close()
        }
    }

    fun selectFromDB(table: String): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $table", null)
    }
}