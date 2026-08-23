package com.example.registernewcalf

import android.database.sqlite.SQLiteDatabase

fun getAllCalves(dbHelper: CalfDatabaseHelper): List<String> {
    val db= dbHelper.readableDatabase
    val cursor = db.rawQuery("SELECT animal_id, breed, birth_date FROM calf_registration", null)

    val calves = mutableListOf<String>()
    while (cursor.moveToNext()) {
        val animalId = cursor.getString(0)
        val breed = cursor.getString(1)
        val birthDate = cursor.getString(2)
        calves.add("ID: $animalId | Breed: $breed | Birth: $birthDate")
    }

    cursor.close()
    db.close()
    return calves
}
fun insertCalf(
    dbHelper: CalfDatabaseHelper,
    animalId: String,
    birthDate: String,
    breed: String,
    damId: String?,
    sireId: String?,
    photoPath: String? = null,
    videoPath: String? = null,
    gpsLat: Double? = null,
    gpsLng: Double? = null
): Boolean {
    if (animalId.isBlank() || birthDate.isBlank() || breed.isBlank()) {
        println("Validation failed: animalId, birthDate, and breed are required")
        return false
    }
    val db= dbHelper.writableDatabase

    // Check for duplicate animal_id
    val cursor = db.rawQuery("SELECT * FROM calf_registration WHERE animal_id = ?", arrayOf(animalId))
    val exists = cursor.count > 0
    cursor.close()

    if (exists) {
        println("Duplicate calf ID: $animalId")
        db.close()
        return false
    }

    val sql = """
        INSERT INTO calf_registration 
        (animal_id, birth_date, breed, dam_id, sire_id, photo_path, video_path, gps_lat, gps_lng, captured_at, device_id, record_guid, sync_status) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), 'device123', hex(randomblob(16)), 'PENDING')
    """
    val stmt = db.compileStatement(sql)
    stmt.bindString(1, animalId)
    stmt.bindString(2, birthDate)
    stmt.bindString(3, breed)
    stmt.bindString(4, damId ?: "")
    stmt.bindString(5, sireId ?: "")
    stmt.bindString(6, photoPath ?: "")
    stmt.bindString(7, videoPath ?: "")
    if (gpsLat != null) stmt.bindDouble(8, gpsLat) else stmt.bindNull(8)
    if (gpsLng != null) stmt.bindDouble(9, gpsLng) else stmt.bindNull(9)
    stmt.executeInsert()

    db.close()
    println("Calf saved successfully:$animalId")
    return true
}
