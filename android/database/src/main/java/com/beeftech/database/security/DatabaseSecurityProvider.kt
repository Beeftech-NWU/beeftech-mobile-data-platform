package com.beeftech.database.security

interface DatabaseSecurityProvider {

    @Throws(DatabaseSecurityException::class)
    fun initializeKey(passcode: String)

    @Throws(DatabaseSecurityException::class)
    fun getDatabasePassphrase(passcode: String): ByteArray

    fun isKeyAvailable(): Boolean
    fun clearKeyFromMemory()

    @Throws(DatabaseSecurityException::class)
    fun invalidateDatabaseKey()
}
