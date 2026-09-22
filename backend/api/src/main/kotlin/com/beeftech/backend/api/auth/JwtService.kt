package com.beeftech.backend.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

data class AuthPrincipal(
    val username: String,
    val userId: String,
    val role: Int?,
    val deviceId: String?
)

class JwtService {

    private val secret: String = System.getenv("BEEFTECH_JWT_SECRET") ?: run {
        println("WARNING: BEEFTECH_JWT_SECRET environment variable not set. Falling back to dev secret.")
        "beeftech-secret"
    }

    fun generateToken(
        username: String,
        userId: String? = null,
        role: Int? = null,
        deviceId: String? = null
    ): String {

        val builder = JWT.create()
            .withSubject(username)
            .withIssuer("beeftech")
            .withExpiresAt(
                Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
            )

        if (userId != null) {
            builder.withClaim("user_id", userId)
        }

        if (role != null) {
            builder.withClaim("role", role)
        }

        if (deviceId != null) {
            builder.withClaim("device_id", deviceId)
        }

        return builder.sign(Algorithm.HMAC256(secret))
    }

    fun validateToken(token: String): String? {

        return try {

            val verifier = JWT
                .require(Algorithm.HMAC256(secret))
                .withIssuer("beeftech")
                .build()

            val decodedJwt = verifier.verify(token)

            decodedJwt.subject

        } catch (e: Exception) {
            null
        }
    }

    fun decode(token: String): AuthPrincipal? {

        return try {

            val verifier = JWT
                .require(Algorithm.HMAC256(secret))
                .withIssuer("beeftech")
                .build()

            val decodedJwt = verifier.verify(token)

            val username = decodedJwt.subject ?: return null
            val userId = decodedJwt.getClaim("user_id").asString() ?: username
            val role = if (decodedJwt.getClaim("role").isNull) null else decodedJwt.getClaim("role").asInt()
            val deviceId = decodedJwt.getClaim("device_id").asString()

            AuthPrincipal(
                username = username,
                userId = userId,
                role = role,
                deviceId = deviceId
            )

        } catch (e: Exception) {
            null
        }
    }
}
