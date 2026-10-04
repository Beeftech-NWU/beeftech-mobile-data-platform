package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthPrincipal
import com.beeftech.backend.api.auth.Role
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and

/**
 * Which synced records a caller may read: worker = their own, manager = their
 * site's, admin = everything.
 */
sealed interface RecordScope {

    data object All : RecordScope

    /* A null siteId matches nothing: a manager without a site sees no site-scoped records. */
    data class Site(val siteId: String?) : RecordScope

    data class User(val userId: String) : RecordScope
}

/* An unknown or missing role gets the least privilege. */
fun AuthPrincipal.recordScope(): RecordScope =
    when (roleEnum) {
        Role.ADMIN -> RecordScope.All
        Role.MANAGER -> RecordScope.Site(siteId)
        else -> RecordScope.User(userId)
    }

/*
 * Pass the table's voided_at column to hide voided records, which is what every
 * list, single-record read and dashboard count does.
 */
fun RecordScope.predicate(
    submittedByUserId: Column<String?>,
    siteId: Column<String?>,
    voidedAt: Column<Long?>? = null
): Op<Boolean> {

    val inScope: Op<Boolean> =
        when (this) {
            RecordScope.All -> Op.TRUE
            is RecordScope.Site ->
                if (this.siteId == null) Op.FALSE else siteId eq this.siteId
            is RecordScope.User -> submittedByUserId eq userId
        }

    return if (voidedAt == null) inScope else inScope and voidedAt.isNull()
}
