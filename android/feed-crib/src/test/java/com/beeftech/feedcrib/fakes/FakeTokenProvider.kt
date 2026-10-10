package com.beeftech.feedcrib.fakes

import com.beeftech.database.security.TokenProvider

class FakeTokenProvider(
    private val tokenValue: String? = "test-token"
) : TokenProvider {

    override suspend fun token(): String? = tokenValue
}
