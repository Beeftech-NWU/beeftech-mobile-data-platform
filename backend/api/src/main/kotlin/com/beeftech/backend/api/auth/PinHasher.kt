package com.beeftech.backend.api.auth

import org.mindrot.jbcrypt.BCrypt

object PinHasher {
    fun hash(pin: String): String {
        return BCrypt.hashpw(pin, BCrypt.gensalt(10))
    }

    fun verify(pin: String, hash: String): Boolean {
        return try {
            BCrypt.checkpw(pin, hash)
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
