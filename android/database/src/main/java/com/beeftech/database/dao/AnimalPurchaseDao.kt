package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalPurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalPurchaseDao {

    @Insert(
        onConflict =
            OnConflictStrategy.REPLACE
    )
    suspend fun insertPurchase(
        purchase: AnimalPurchaseEntity
    )


    @Query(
        """
        SELECT *
        FROM animal_purchases
        WHERE animal_id = :animalId
        ORDER BY purchase_date DESC
        """
    )
    fun getPurchasesForAnimal(
        animalId: String
    ): Flow<List<AnimalPurchaseEntity>>


    @Query(
        """
        SELECT *
        FROM animal_purchases
        WHERE seller_name = :sellerName
        ORDER BY purchase_date DESC
        """
    )
    fun getPurchasesBySeller(
        sellerName: String
    ): Flow<List<AnimalPurchaseEntity>>


    @Query(
        """
        SELECT *
        FROM animal_purchases
        WHERE record_guid = :recordGuid
        LIMIT 1
        """
    )
    suspend fun findByRecordGuid(
        recordGuid: String
    ): AnimalPurchaseEntity?


    /*
     * A purchase is considered the SAME transaction when:
     *
     * - it belongs to the same animal
     * - it has the same purchase date
     * - it has the same purchase batch
     * - and it refers to the same registered supplier farmer
     *
     * For an external supplier, name + GLN are used instead.
     *
     * This prevents repeatedly pressing Save from creating
     * multiple copies of the same traceability event.
     */
    @Query(
        """
        SELECT *
        FROM animal_purchases
        WHERE animal_id = :animalId
          AND purchase_date = :purchaseDate

          AND LOWER(
                TRIM(
                    IFNULL(
                        purchase_batch_number,
                        ''
                    )
                )
              ) =
              LOWER(
                TRIM(
                    IFNULL(
                        :purchaseBatchNumber,
                        ''
                    )
                )
              )

          AND (
                (
                    IFNULL(
                        TRIM(:supplierFarmerId),
                        ''
                    ) != ''

                    AND supplier_farmer_id =
                        :supplierFarmerId
                )

                OR

                (
                    IFNULL(
                        TRIM(:supplierFarmerId),
                        ''
                    ) = ''

                    AND IFNULL(
                            TRIM(supplier_farmer_id),
                            ''
                        ) = ''

                    AND LOWER(
                            TRIM(seller_name)
                        ) =
                        LOWER(
                            TRIM(:sellerName)
                        )

                    AND IFNULL(
                            TRIM(gln_number),
                            ''
                        ) =
                        IFNULL(
                            TRIM(:glnNumber),
                            ''
                        )
                )
          )

        LIMIT 1
        """
    )
    suspend fun findEquivalentPurchase(
        animalId: String,
        supplierFarmerId: String?,
        sellerName: String,
        glnNumber: String?,
        purchaseDate: Long,
        purchaseBatchNumber: String?
    ): AnimalPurchaseEntity?


    @Query(
        """
        DELETE FROM animal_purchases
        WHERE purchase_id = :purchaseId
        """
    )
    suspend fun deletePurchaseById(
        purchaseId: String
    ): Int
}
