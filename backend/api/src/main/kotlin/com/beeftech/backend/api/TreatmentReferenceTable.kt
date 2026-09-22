package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

/*
 * Master/reference data used by the Treatment screen.
 *
 * These tables contain selectable values only.
 * Individual animal treatment records remain in TreatmentTable.
 */
object DiseaseTable : Table("diseases") {

    val id =
        integer("id")
            .autoIncrement()

    val name =
        varchar(
            "name",
            255
        )
            .uniqueIndex()

    val active =
        bool("active")
            .default(true)

    override val primaryKey =
        PrimaryKey(id)
}

object TreatmentTypeTable : Table("treatment_types") {

    val id =
        integer("id")
            .autoIncrement()

    val name =
        varchar(
            "name",
            255
        )
            .uniqueIndex()

    val active =
        bool("active")
            .default(true)

    override val primaryKey =
        PrimaryKey(id)
}
