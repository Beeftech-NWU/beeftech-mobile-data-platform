package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.FarmerAddressEntity
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.entity.FarmerRoleEntity
import kotlinx.coroutines.flow.Flow

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

    /* Newest first. Farmers have no created time, so insertion order (rowid) stands in for it. */
    @Query(
        "SELECT * FROM farmers ORDER BY rowid DESC"
    )
    fun observeAllFarmers(): Flow<List<FarmerEntity>>

    @Query(
        """
        SELECT * FROM farmers
        WHERE sync_status IN ('PENDING', 'PROCESSING')
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


    /*
     * Farmer business role 7 = Supplier.
     *
     * These records are the local/offline source for the
     * Supplier screen dropdown.
     */
    @Query(
        """
        SELECT DISTINCT f.*
        FROM farmers f
        INNER JOIN farmer_roles fr
            ON fr.farmer_id = f.farmer_id
        WHERE fr.role_id = 7
        ORDER BY
            COALESCE(
                f.organisation_name,
                f.client_code,
                f.farmer_id
            )
        """
    )
    suspend fun getSupplierFarmers():
            List<FarmerEntity>

    @Query(
        """
        SELECT DISTINCT f.*
        FROM farmers f
        INNER JOIN farmer_roles fr
            ON fr.farmer_id = f.farmer_id
        WHERE fr.role_id = 7
          AND (
              f.organisation_name =
                  :displayName COLLATE NOCASE
              OR
              f.client_code =
                  :displayName COLLATE NOCASE
          )
        LIMIT 1
        """
    )
    suspend fun findSupplierFarmerByDisplayName(
        displayName: String
    ): FarmerEntity?

}
