package com.beeftech.backend.api.auth

class AuthService(private val jwtService: JwtService) {

    fun login(
        username: String,
        password: String
    ): String? {

        return if (
            username == "admin" &&
            password == "admin123"
        ) {
            jwtService.generateToken(username)
        } else {
            null
        }
    }

    fun register(
        username: String,
        password: String
    ): Boolean {
        // TODO Persist user when database is available
        return true
    }
}