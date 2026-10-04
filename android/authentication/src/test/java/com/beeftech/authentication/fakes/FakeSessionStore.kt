package com.beeftech.authentication.fakes

import com.beeftech.authentication.data.SessionStore
import com.beeftech.authentication.domain.LoggedInUser


class FakeSessionStore :
    SessionStore {

    private var cachedToken:
            String? =
        null

    private var localSessionExpiresAt:
            Long =
        0L

    private var serverTokenExpiresAt:
            Long =
        0L

    private var user:
            LoggedInUser? =
        null


    /* Set by a test to simulate the server having revoked this user's access. */
    var revokedUser: String? = null

    override fun revokedUserId(): String? =
        revokedUser

    override fun save(
        token: String,
        expiresAt: Long,
        user: LoggedInUser
    ) {

        if (revokedUser == user.userId) {
            revokedUser = null
        }

        cachedToken =
            token

        localSessionExpiresAt =
            expiresAt

        serverTokenExpiresAt =
            expiresAt

        this.user =
            user
    }


    override fun saveOffline(
        expiresAt: Long,
        user: LoggedInUser
    ) {

        val now =
            System.currentTimeMillis()

        val keepServerToken =
            this.user
                ?.userId == user.userId &&
                !cachedToken.isNullOrBlank() &&
                !cachedToken
                    .orEmpty()
                    .startsWith(
                        "OFFLINE_TOKEN_"
                    ) &&
                serverTokenExpiresAt > now

        if (!keepServerToken) {

            cachedToken =
                null

            serverTokenExpiresAt =
                0L
        }

        localSessionExpiresAt =
            expiresAt

        this.user =
            user
    }


    override fun currentUser():
            LoggedInUser? {

        return user
    }


    override fun isExpired(
        now: Long
    ): Boolean {

        return now >=
            localSessionExpiresAt
    }


    override fun clear() {

        cachedToken =
            null

        localSessionExpiresAt =
            0L

        serverTokenExpiresAt =
            0L

        user =
            null
    }


    override suspend fun token():
            String? {

        if (isExpired()) {
            return null
        }

        if (
            System.currentTimeMillis() >=
            serverTokenExpiresAt
        ) {
            return null
        }

        val token =
            cachedToken
                ?.trim()

        if (token.isNullOrEmpty()) {
            return null
        }

        if (
            token.startsWith(
                "OFFLINE_TOKEN_"
            )
        ) {
            return null
        }

        return token
    }
}
