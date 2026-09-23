package com.beeftech.backend.api.auth

import java.util.UUID

object DevUserSeeder {

    suspend fun seed(userRepository: UserRepository) {
        println("Seeding dev users...")

        val devUsers = listOf(
            DevUser("admin", "10001", 1),
            DevUser("fmanager", "20002", 2),
            DevUser("jvdm", "30003", 3)
        )

        for (user in devUsers) {
            val existing = userRepository.findByUsername(user.username)
            if (existing == null) {
                userRepository.insertUser(
                    userId = UUID.randomUUID().toString(),
                    username = user.username,
                    pinHash = PinHasher.hash(user.pin),
                    role = user.role
                )
                println("Seeded user: ${user.username}")
            }
        }
    }

    private data class DevUser(
        val username: String,
        val pin: String,
        val role: Int
    )
}
