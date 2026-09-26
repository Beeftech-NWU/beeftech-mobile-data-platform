package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.CostType

@Dao
interface CostTypeDao {

    @Query("SELECT * FROM cost_types WHERE is_active = 1 ORDER BY sort_order, code")
    suspend fun getActive(): List<CostType>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(types: List<CostType>)
}
