package com.beeftech.backend.api.auth

import org.jetbrains.exposed.sql.Table

object SitesTable : Table("sites") {
    val siteId = varchar("site_id", 64)
    val name = varchar("name", 255)
    val createdAt = long("created_at")

    /* An inactive site takes no new users; its existing users and records are untouched. */
    val active = bool("active").default(true)
    val updatedAt = long("updated_at").nullable()

    /* Four characters, A-Z and 0-9, used as the first part of every file and batch name. Null only on a database not yet migrated. */
    val farmCode = varchar("farm_code", 4).nullable().uniqueIndex()

    override val primaryKey = PrimaryKey(siteId)
}
