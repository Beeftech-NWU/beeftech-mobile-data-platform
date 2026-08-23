package com.example.registernewcalf

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CalfDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE calf_registration (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                animal_id TEXT UNIQUE,
                birth_date DATE,
                breed TEXT,
                dam_id TEXT,
                sire_id TEXT,
                photo_path TEXT,
                video_path TEXT,
                gps_lat REAL,
                gps_lng REAL,
                captured_at DATETIME,
                device_id TEXT,
                record_guid TEXT UNIQUE,
                sync_status TEXT,
                synced_at DATETIME
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS calf_registration")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "calf_db.sqlite"
        private const val DATABASE_VERSION = 1
    }
}
