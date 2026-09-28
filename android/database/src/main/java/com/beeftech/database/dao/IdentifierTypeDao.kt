package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.IdentifierType

@Dao
interface IdentifierTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(type: IdentifierType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(types: List<IdentifierType>)

    @Query("SELECT * FROM identifier_types")
    suspend fun getAll(): List<IdentifierType>

    @Query("SELECT * FROM identifier_types WHERE code = :code")
    suspend fun getByCode(code: String): IdentifierType?
}
