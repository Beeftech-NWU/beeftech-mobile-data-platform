package com.beeftech.database.security

interface TokenProvider {
    suspend fun token(): String?
}
