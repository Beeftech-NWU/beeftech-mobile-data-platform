package com.beeftech.database.security

object TokenProviderRegistry {

    @Volatile
    private var provider: TokenProvider? = null

    fun register(tokenProvider: TokenProvider) {
        provider = tokenProvider
    }

    fun get(): TokenProvider? = provider

    fun clear() {
        provider = null
    }
}
