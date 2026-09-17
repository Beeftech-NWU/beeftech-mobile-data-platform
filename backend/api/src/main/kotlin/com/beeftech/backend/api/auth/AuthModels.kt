package com.beeftech.backend.api.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String
)

@Serializable
data class ProfileResponse(
    val username: String
)