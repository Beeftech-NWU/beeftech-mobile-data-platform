package com.beeftech.database.security

import kotlinx.coroutines.CancellationException

enum class UnauthorizedReason {
    /* The token was refused (expired, or signed by another key): sign in again to get a new one. */
    TOKEN_REJECTED,

    /* The server ended this account's or phone's access (deactivated, PIN reset, unbound, revoked). */
    SESSION_REVOKED;

    companion object {
        /*
         * The server says "Session revoked" or "Device revoked" in the 401 body when access was
         * ended on purpose; anything else is an ordinary rejected token.
         */
        fun fromServerMessage(message: String?): UnauthorizedReason =
            if (message != null &&
                (message.contains("session revoked", ignoreCase = true) ||
                    message.contains("device revoked", ignoreCase = true))
            ) {
                SESSION_REVOKED
            } else {
                TOKEN_REJECTED
            }
    }
}

private val MESSAGE_FIELD = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"")

/**
 * Tells the token provider the server answered 401. Pass the response body so a revocation can
 * be told apart from a plain expired token. Never throws (the caller is already handling a failure).
 */
suspend fun TokenProvider.reportUnauthorized(responseBody: String?) {
    val message = responseBody?.let { MESSAGE_FIELD.find(it)?.groupValues?.get(1) }
    try {
        onUnauthorized(UnauthorizedReason.fromServerMessage(message))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        /* Reporting is best effort. */
    }
}
