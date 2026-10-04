package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthPrincipal
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.auth.UserRepository

sealed interface AuditResult<out T> {
    data class Ok<T>(val value: T) : AuditResult<T>
    data class Forbidden(val message: String) : AuditResult<Nothing>
}

/**
 * Reads the audit log: every site for an admin, their own site for a manager.
 * As elsewhere, the caller's role, site and active flag come from the database,
 * not the token.
 */
class AuditService(
    private val userRepository: UserRepository,
    private val repository: AuditRepository
) {

    suspend fun list(principal: AuthPrincipal, filter: AuditFilter, limit: Int?): AuditResult<List<AuditLogEntryDto>> {
        val record = userRepository.findById(principal.userId)
        val role = Role.fromId(record?.role)
        if (record == null || !record.active || (role != Role.ADMIN && role != Role.MANAGER)) {
            return AuditResult.Forbidden("Forbidden")
        }

        val siteScope = if (role == Role.ADMIN) {
            SiteScope.Any
        } else {
            /* A manager without a site sees no entries, as with scoped record reads. */
            val ownSite = record.siteId ?: return AuditResult.Ok(emptyList())
            if (filter.siteId != null && filter.siteId != ownSite) {
                return AuditResult.Forbidden("Managers can only view their own site")
            }
            SiteScope.Only(ownSite)
        }

        return AuditResult.Ok(
            repository.query(filter, siteScope, (limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT))
        )
    }

    private companion object {
        const val DEFAULT_LIMIT = 100
        const val MAX_LIMIT = 500
    }
}
