package com.beeftech.authentication.fakes

import com.beeftech.authentication.data.SessionStore
import com.beeftech.authentication.domain.LoggedInUser

class FakeSessionStore : SessionStore {
    private var cachedToken: String? = null
    private var cachedExpiresAt: Long = 0L
    private var user: LoggedInUser? = null

    override fun save(token: String, expiresAt: Long, user: LoggedInUser) {
        this.cachedToken = token
        this.cachedExpiresAt = expiresAt
        this.user = user
    }

    override fun currentUser(): LoggedInUser? {
        return user
    }

    override fun isExpired(now: Long): Boolean {
        return now >= cachedExpiresAt
    }

    override fun clear() {
        cachedToken = null
        cachedExpiresAt = 0L
        user = null
    }

    override suspend fun token(): String? {
        if (isExpired()) return null
        return cachedToken
    }
}
