package com.beeftech.database.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application-wide authenticated-user identity.
 *
 * The StateFlow allows UI/database observers to switch immediately
 * when another user logs in on the same device.
 */
object CurrentUserIdRegistry {

    @Volatile
    private var provider:
            (() -> String?)? =
        null

    private val _currentUserId =
        MutableStateFlow<String?>(
            null
        )

    val currentUserIdFlow:
            StateFlow<String?> =
        _currentUserId
            .asStateFlow()

    fun register(
        provider: () -> String?
    ) {

        this.provider =
            provider

        refresh()
    }

    fun refresh() {

        _currentUserId.value =
            normalize(
                provider
                    ?.invoke()
            )
    }

    fun setCurrentUserId(
        userId: String?
    ) {

        _currentUserId.value =
            normalize(
                userId
            )
    }

    fun currentUserId():
            String? {

        val registeredProvider =
            provider

        if (registeredProvider != null) {

            val resolved =
                normalize(
                    registeredProvider()
                )

            if (
                _currentUserId.value !=
                resolved
            ) {

                _currentUserId.value =
                    resolved
            }

            return resolved
        }

        return _currentUserId.value
    }

    fun clear() {

        provider =
            null

        _currentUserId.value =
            null
    }

    private fun normalize(
        userId: String?
    ): String? {

        return userId
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }
    }
}
