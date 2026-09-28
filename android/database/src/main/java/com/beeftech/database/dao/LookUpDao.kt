package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LookUpDao {

    data class LookupOption(
        val code: String,
        val displayName: String,
        val isActive: Boolean
    )

    @Query("SELECT code, display_name AS displayName, is_active AS isActive FROM breeds WHERE is_active = 1 ORDER BY display_name")
    fun breeds(): Flow<List<LookupOption>>

    @Query("SELECT code, display_name AS displayName, is_active AS isActive FROM hide_colours WHERE is_active = 1 ORDER BY display_name")
    fun hideColours(): Flow<List<LookupOption>>

    @Query("SELECT code, display_name AS displayName, is_active AS isActive FROM medications WHERE is_active = 1 ORDER BY display_name")
    fun medications(): Flow<List<LookupOption>>

    @Query("SELECT code, display_name AS displayName, is_active AS isActive FROM diseases WHERE is_active = 1 ORDER BY display_name")
    fun diseases(): Flow<List<LookupOption>>

    @Query("SELECT code, display_name AS displayName, is_active AS isActive FROM medication_batches WHERE is_active = 1 ORDER BY display_name")
    fun medicationBatches(): Flow<List<LookupOption>>

    @Query("SELECT code, display_name AS displayName, is_active AS isActive FROM movement_types WHERE is_active = 1 ORDER BY display_name")
    fun movementTypes(): Flow<List<LookupOption>>

    @Query("SELECT code, display_name AS displayName, is_active AS isActive FROM rations WHERE is_active = 1 ORDER BY display_name")
    fun rations(): Flow<List<LookupOption>>

    @Query("SELECT code, display_name AS displayName, is_active AS isActive FROM identifier_types WHERE is_active = 1 ORDER BY display_name")
    fun identifierTypes(): Flow<List<LookupOption>>

    @Query("SELECT code, display_name AS displayName, is_active AS isActive FROM countries WHERE is_active = 1 ORDER BY display_name")
    fun countries(): Flow<List<LookupOption>>

    @Query("SELECT code, display_name AS displayName, is_active AS isActive FROM provinces WHERE is_active = 1 ORDER BY display_name")
    fun provinces(): Flow<List<LookupOption>>

    @Query("SELECT code, display_name AS displayName, is_active AS isActive FROM necropsy_codes WHERE is_active = 1 ORDER BY display_name")
    fun necropsyCodes(): Flow<List<LookupOption>>

}