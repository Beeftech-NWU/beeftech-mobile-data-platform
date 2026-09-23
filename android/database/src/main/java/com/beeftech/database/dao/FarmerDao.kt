package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.FarmerAddressEntity
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.entity.FarmerRoleEntity

@Dao
interface FarmerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmer(
        farmer: FarmerEntity
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmerAddress(
        address: FarmerAddressEntity
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmerRole(
        role: FarmerRoleEntity
    )

    @Query(
        "SELECT * FROM farmers WHERE farmer_id = :farmerId"
    )
    suspend fun getFarmerById(
        farmerId: String
    ): FarmerEntity?

    @Query(
        "SELECT * FROM farmers"
    )
    suspend fun getAllFarmers(): List<FarmerEntity>

    @Query(
        """
        SELECT * FROM farmers
        WHERE sync_status = 'PENDING'
        """
    )
    suspend fun getPendingFarmers(): List<FarmerEntity>

    @Query(
        """
        SELECT * FROM farmer_addresses
        WHERE farmer_id = :farmerId
        """
    )
    suspend fun getAddressesForFarmer(
        farmerId: String
    ): List<FarmerAddressEntity>

    @Query(
        """
        SELECT * FROM farmer_roles
        WHERE farmer_id = :farmerId
        """
    )
    suspend fun getRolesForFarmer(
        farmerId: String
    ): List<FarmerRoleEntity>

    @Query(
        """
        UPDATE farmers
        SET sync_status = :status
        WHERE farmer_id = :farmerId
        """
    )
    suspend fun updateSyncStatus(
        farmerId: String,
        status: String
    )
}