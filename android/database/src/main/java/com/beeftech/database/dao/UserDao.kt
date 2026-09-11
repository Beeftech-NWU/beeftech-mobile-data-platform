package com.beeftech.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beeftech.database.entity.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    @Query("UPDATE users SET failed_sync_attempts = :attempts WHERE user_id = :userId")
    suspend fun updateFailedSyncAttempts(userId: String, attempts: Int)

    @Query("UPDATE users SET device_last_sync = :timestamp, failed_sync_attempts = 0 WHERE user_id = :userId")
    suspend fun updateLastSync(userId: String, timestamp: Long)

    @Query("UPDATE users SET pin_hash = :pinHash WHERE user_id = :userId")
    suspend fun updatePinHash(userId: String, pinHash: String?)
}
