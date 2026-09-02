package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CalfRegistration
import com.example.data.model.SyncLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [CalfRegistration::class, SyncLog::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calfRegistrationDao(): CalfRegistrationDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "beeftech_calf_registry.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.calfRegistrationDao(), database.syncLogDao())
                    }
                }
            }
        }

        private suspend fun populateInitialData(
            dao: CalfRegistrationDao,
            syncLogDao: SyncLogDao
        ) {
            val now = System.currentTimeMillis()
            val oneDay = 86400000L

            val sampleCalves = listOf(
                CalfRegistration(
                    animalId = "ZA-26-0812",
                    birthDate = "2026-08-12",
                    breed = "Bonsmara",
                    sex = CalfRegistration.SEX_HEIFER,
                    birthWeightKg = 34.5,
                    calvingEase = CalfRegistration.CALVING_EASE_UNASSISTED,
                    vigor = CalfRegistration.VIGOR_VIGOROUS,
                    hornStatus = CalfRegistration.HORN_POLLED,
                    pastureLocation = "Camp 4 - North Pasture",
                    rfidTag = "982 000182938471",
                    damId = "DAM-BN-440",
                    sireId = "BULL-BN-902",
                    photoPath = null,
                    gpsLat = -25.7461,
                    gpsLng = 28.1881,
                    capturedAt = now - (oneDay * 3),
                    deviceId = "BEEFTECH-TAB-042",
                    recordGuid = UUID.randomUUID().toString(),
                    syncStatus = CalfRegistration.SYNC_STATUS_SYNCED,
                    syncedAt = now - (oneDay * 2)
                ),
                CalfRegistration(
                    animalId = "ZA-26-0815",
                    birthDate = "2026-08-15",
                    breed = "Brahman",
                    sex = CalfRegistration.SEX_BULL,
                    birthWeightKg = 38.0,
                    calvingEase = CalfRegistration.CALVING_EASE_UNASSISTED,
                    vigor = CalfRegistration.VIGOR_VIGOROUS,
                    hornStatus = CalfRegistration.HORN_HORNED,
                    pastureLocation = "Camp 1 - River Meadow",
                    rfidTag = "982 000199482711",
                    damId = "DAM-BR-112",
                    sireId = "BULL-BR-550",
                    photoPath = null,
                    gpsLat = -25.7480,
                    gpsLng = 28.1895,
                    capturedAt = now - (oneDay * 2),
                    deviceId = "BEEFTECH-TAB-042",
                    recordGuid = UUID.randomUUID().toString(),
                    syncStatus = CalfRegistration.SYNC_STATUS_SYNCED,
                    syncedAt = now - oneDay
                ),
                CalfRegistration(
                    animalId = "ZA-26-0820",
                    birthDate = "2026-08-20",
                    breed = "Nguni",
                    sex = CalfRegistration.SEX_HEIFER,
                    birthWeightKg = 29.5,
                    calvingEase = CalfRegistration.CALVING_EASE_UNASSISTED,
                    vigor = CalfRegistration.VIGOR_VIGOROUS,
                    hornStatus = CalfRegistration.HORN_POLLED,
                    pastureLocation = "Camp 2 - West Hill Paddock",
                    rfidTag = "982 000210482910",
                    damId = "DAM-NG-304",
                    sireId = "BULL-NG-101",
                    photoPath = null,
                    gpsLat = -25.7445,
                    gpsLng = 28.1920,
                    capturedAt = now - (oneDay * 1),
                    deviceId = "BEEFTECH-TAB-042",
                    recordGuid = UUID.randomUUID().toString(),
                    syncStatus = CalfRegistration.SYNC_STATUS_PENDING,
                    syncedAt = null
                ),
                CalfRegistration(
                    animalId = "ZA-26-0826",
                    birthDate = "2026-08-26",
                    breed = "Angus",
                    sex = CalfRegistration.SEX_BULL,
                    birthWeightKg = 36.2,
                    calvingEase = CalfRegistration.CALVING_EASE_EASY_PULL,
                    vigor = CalfRegistration.VIGOR_VIGOROUS,
                    hornStatus = CalfRegistration.HORN_POLLED,
                    pastureLocation = "Maternity Kraal",
                    rfidTag = "982 000239102834",
                    damId = "DAM-AN-890",
                    sireId = "BULL-AN-224",
                    photoPath = null,
                    gpsLat = -25.7430,
                    gpsLng = 28.1860,
                    capturedAt = now - (1000 * 60 * 120),
                    deviceId = "BEEFTECH-TAB-042",
                    recordGuid = UUID.randomUUID().toString(),
                    syncStatus = CalfRegistration.SYNC_STATUS_PENDING,
                    syncedAt = null
                )
            )

            for (calf in sampleCalves) {
                dao.insert(calf)
            }

            syncLogDao.insertLog(
                SyncLog(
                    timestamp = now - (oneDay * 2),
                    batchSize = 1,
                    status = "SUCCESS",
                    endpoint = "http://192.168.1.100:8080/api/v1/sync",
                    responseMessage = "200 OK: 1 calf registration pushed",
                    durationMs = 340
                )
            )
            syncLogDao.insertLog(
                SyncLog(
                    timestamp = now - oneDay,
                    batchSize = 1,
                    status = "SUCCESS",
                    endpoint = "http://192.168.1.100:8080/api/v1/sync",
                    responseMessage = "200 OK: 1 calf registration pushed",
                    durationMs = 280
                )
            )
        }
    }
}
