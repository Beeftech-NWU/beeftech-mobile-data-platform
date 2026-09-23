package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beeftech.database.entity.AnimalPurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalPurchaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: AnimalPurchaseEntity)

    @Query("SELECT * FROM animal_purchases WHERE animal_id = :animalId")
    fun getPurchasesForAnimal(animalId: String): Flow<List<AnimalPurchaseEntity>>

    @Query("SELECT * FROM animal_purchases WHERE seller_name = :sellerName")
    fun getPurchasesBySeller(sellerName: String): Flow<List<AnimalPurchaseEntity>>
}
