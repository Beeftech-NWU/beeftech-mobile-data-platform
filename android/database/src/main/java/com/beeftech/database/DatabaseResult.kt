package com.beeftech.database

sealed class DatabaseResult {

    data class Success(
        val database: BeefTechDatabase
    ) : DatabaseResult()

    data class Error(
        val type: DatabaseErrorType,
        val message: String,
        val cause: Throwable? = null
    ) : DatabaseResult()
}

enum class DatabaseErrorType {
    EMPTY_PASSPHRASE,
    INVALID_PASSPHRASE,
    DATABASE_CORRUPTION,
    DATABASE_OPEN_ERROR,
    UNKNOWN
}