package com.beeftech.backend.api.feedcrib

import org.jetbrains.exposed.sql.Table

/* New table, so SchemaUtils.create makes it on existing databases; no ALTER migration needed. */
object FeedCribTable : Table("feed_crib_readings") {
    val id = long("id").autoIncrement()
    val penName = varchar("pen_name", 255).index()
    val adiValue = double("adi_value")
    val morning = varchar("morning", 255)
    val midDay = varchar("mid_day", 255)
    val evening = varchar("evening", 255)
    val timestamp = long("timestamp")
    val submittedByUserId = varchar("submitted_by_user_id", 64).nullable()
    val siteId = varchar("site_id", 64).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
