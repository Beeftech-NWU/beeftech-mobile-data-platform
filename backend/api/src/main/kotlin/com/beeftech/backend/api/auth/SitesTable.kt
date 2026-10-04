package com.beeftech.backend.api.auth

import org.jetbrains.exposed.sql.Table

object SitesTable : Table("sites") {
    val siteId = varchar("site_id", 64)
    val name = varchar("name", 255)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(siteId)
}
