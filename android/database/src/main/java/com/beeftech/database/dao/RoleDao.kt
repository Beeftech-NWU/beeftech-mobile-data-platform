package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beeftech.database.entity.Role

@Dao
interface RoleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRole(role: Role): Long

    @Update
    suspend fun updateRole(role: Role)

    @Delete
    suspend fun deleteRole(role: Role)

    @Query("SELECT * FROM roles WHERE role_id = :roleId")
    suspend fun getRoleById(roleId: Long): Role?

    @Query("SELECT * FROM roles WHERE role_name = :roleName LIMIT 1")
    suspend fun getRoleByName(roleName: String): Role?

    @Query("SELECT * FROM roles")
    suspend fun getAllRoles(): List<Role>
}
