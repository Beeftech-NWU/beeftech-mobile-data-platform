package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.beeftech.database.entity.Supplier

@Dao
interface SupplierDao {

    @Insert
    suspend fun insert(
        supplier: Supplier
    )

    @Query(
        """
        SELECT *
        FROM suppliers
        ORDER BY timestamp DESC
        """
    )
    suspend fun getAll(): List<Supplier>

    @Query(
        """
        SELECT *
        FROM suppliers
        WHERE animalId = :animalId
        ORDER BY timestamp DESC
        """
    )
    suspend fun getByAnimalId(
        animalId: String
    ): List<Supplier>
}