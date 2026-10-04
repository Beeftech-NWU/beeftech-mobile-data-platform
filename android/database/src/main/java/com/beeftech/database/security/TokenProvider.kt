package com.beeftech.database.security

interface TokenProvider {
    suspend fun token(): String?

    /*
     * Called when the server answered 401 to a request made with this provider's token.
     * The default does nothing, so existing providers and fakes keep working.
     *
     * An implementation may drop the server token (so workers wait for an online login) or end
     * the session. It must never touch the records waiting to sync: a 401 means "sign in
     * again", not "discard your data".
     */
    suspend fun onUnauthorized(reason: UnauthorizedReason) {}
}
