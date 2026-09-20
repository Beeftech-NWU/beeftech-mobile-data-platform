package com.beeftech.authentication.domain

interface TokenProvider {
    suspend fun token(): String?
}