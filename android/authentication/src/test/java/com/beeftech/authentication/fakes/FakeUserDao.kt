package com.beeftech.authentication.fakes

import com.beeftech.database.dao.UserDao
import com.beeftech.database.entity.User

class FakeUserDao : UserDao {
    private val users = mutableMapOf<String, User>()

    override suspend fun insertUser(user: User) {
        users[user.userId] = user
    }

    override suspend fun updateUser(user: User) {
        users[user.userId] = user
    }

    override suspend fun deleteUser(user: User) {
        users.remove(user.userId)
    }

    override suspend fun getUserById(userId: String): User? {
        return users[userId]
    }

    override suspend fun getUserByUsername(username: String): User? {
        return users.values.firstOrNull { it.username == username }
    }

    override suspend fun getAllUsers(): List<User> {
        return users.values.toList()
    }

    override suspend fun updateFailedPinAttempts(userId: String, attempts: Int) {
        users[userId]?.let {
            users[userId] = it.copy(failedPinAttempts = attempts)
        }
    }

    override suspend fun updateFailedSyncAttempts(userId: String, attempts: Int) {
        users[userId]?.let {
            users[userId] = it.copy(failedSyncAttempts = attempts)
        }
    }

    override suspend fun updateLastSync(userId: String, timestamp: Long) {
        users[userId]?.let {
            users[userId] = it.copy(deviceLastSync = timestamp, failedSyncAttempts = 0)
        }
    }

    override suspend fun updatePinHash(userId: String, pinHash: String?) {
        users[userId]?.let {
            users[userId] = it.copy(pinHash = pinHash, failedPinAttempts = 0)
        }
    }
}
