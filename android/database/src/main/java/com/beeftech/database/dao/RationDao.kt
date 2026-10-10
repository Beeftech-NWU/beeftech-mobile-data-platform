package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.beeftech.database.entity.RationEntity

@Dao
interface RationDao {

    @Query(
        """
        SELECT *
        FROM rations
        WHERE active = 1
        ORDER BY name COLLATE NOCASE
        """
    )
    suspend fun getActiveRations(): List<RationEntity>
}
