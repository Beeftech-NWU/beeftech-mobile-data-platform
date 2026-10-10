package com.beeftech.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Local, durable association. Historical links retain their identity across transfers. */
@Entity(
    tableName = "farmer_animal_links",
    foreignKeys = [
        ForeignKey(entity = FarmerEntity::class, parentColumns = ["farmer_id"], childColumns = ["farmer_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Animal::class, parentColumns = ["animalId"], childColumns = ["animal_id"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("farmer_id"), Index("animal_id"), Index(value = ["record_guid"], unique = true)]
)
data class FarmerAnimalLink(
    @PrimaryKey @ColumnInfo(name = "link_id") val linkId: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "farmer_id") val farmerId: String,
    @ColumnInfo(name = "animal_id") val animalId: String,
    @ColumnInfo(name = "record_guid") val recordGuid: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "effective_from") val effectiveFrom: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "effective_to") val effectiveTo: Long? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "PENDING"
)
