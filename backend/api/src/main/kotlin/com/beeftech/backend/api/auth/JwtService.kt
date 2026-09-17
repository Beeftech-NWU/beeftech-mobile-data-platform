package com.beeftech.backend.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class JwtService {

    private val secret = "beeftech-secret"

    fun generateToken(username: String): String {

        return JWT.create()
            .withSubject(username)
            .withIssuer("beeftech")
            .withExpiresAt(
                Date(
                    System.currentTimeMillis() +
                            24 * 60 * 60 * 1000
                )
            )
            .sign(
                Algorithm.HMAC256(secret)
            )
    }

    fun validateToken(token: String): String? {

        return try {

            val verifier = JWT
                .require(
                    Algorithm.HMAC256(secret)
                )
                .withIssuer("beeftech")
                .build()

            val decodedJwt = verifier.verify(token)

            decodedJwt.subject

        } catch (e: Exception) {
            null
        }
    }
}