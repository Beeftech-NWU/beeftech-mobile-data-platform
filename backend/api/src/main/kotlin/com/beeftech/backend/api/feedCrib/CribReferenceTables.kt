package com.beeftech.backend.api.feedcrib

import org.jetbrains.exposed.sql.Table

/** Authoritative pen references; Add Crib never creates these implicitly. */
object CribPensTable : Table("feed_pens") {
    val id = varchar("id", 64)
    val siteId = varchar("site_id", 64)
    val description = varchar("description", 255)
    val animalsBegin = integer("animals_begin").nullable()
    val animalsIn = integer("animals_in").nullable()
    val animalsOut = integer("animals_out").nullable()
    val animalsClose = integer("animals_close").nullable()
    val requiredKg = double("required_kg").nullable()
    val active = bool("active").default(true)
    override val primaryKey = PrimaryKey(id)
}

object CribRationsTable : Table("feed_rations") {
    val id = varchar("id", 64)
    val name = varchar("name", 255)
    val active = bool("active").default(true)
    override val primaryKey = PrimaryKey(id)
}
