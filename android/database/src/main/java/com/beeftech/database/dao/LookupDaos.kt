package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.Breed
import com.beeftech.database.entity.Country
import com.beeftech.database.entity.Device
import com.beeftech.database.entity.Disease
import com.beeftech.database.entity.HideColour
import com.beeftech.database.entity.Medication
import com.beeftech.database.entity.MedicationBatch
import com.beeftech.database.entity.NecropsyCode
import com.beeftech.database.entity.Province

@Dao
interface BreedDao {
    @Query("SELECT * FROM breeds")
    suspend fun getAll(): List<Breed>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(breeds: List<Breed>)
}

@Dao
interface HideColourDao {
    @Query("SELECT * FROM hide_colours")
    suspend fun getAll(): List<HideColour>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(colours: List<HideColour>)
}

@Dao
interface NecropsyCodeDao {
    @Query("SELECT * FROM necropsy_codes")
    suspend fun getAll(): List<NecropsyCode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(codes: List<NecropsyCode>)
}

@Dao
interface DiseaseDao {
    @Query("SELECT * FROM diseases")
    suspend fun getAll(): List<Disease>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(diseases: List<Disease>)
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications")
    suspend fun getAll(): List<Medication>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medications: List<Medication>)
}

@Dao
interface MedicationBatchDao {
    @Query("SELECT * FROM medication_batches")
    suspend fun getAll(): List<MedicationBatch>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(batches: List<MedicationBatch>)
}

@Dao
interface CountryDao {
    @Query("SELECT * FROM countries")
    suspend fun getAll(): List<Country>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(countries: List<Country>)
}

@Dao
interface ProvinceDao {
    @Query("SELECT * FROM provinces")
    suspend fun getAll(): List<Province>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(provinces: List<Province>)
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices")
    suspend fun getAll(): List<Device>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<Device>)
}
