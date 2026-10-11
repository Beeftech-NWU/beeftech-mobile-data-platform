package com.beeftech.backend.api

import org.jetbrains.exposed.sql.Table

/*
 * Backend farmer registration tables.
 *
 * A farmer is the parent record.
 * Addresses and roles are stored separately and linked
 * through farmerId.
 */

object FarmerTable : Table("farmers") {

    val farmerId =
        varchar(
            "farmer_id",
            255
        )

    val clientCode =
        varchar(
            "client_code",
            255
        )
            .nullable()

    val organisationName =
        varchar(
            "organisation_name",
            255
        )
            .nullable()

    val vatNumber =
        varchar(
            "vat_number",
            255
        )
            .nullable()

    val emailAddress =
        varchar(
            "email_address",
            255
        )
            .nullable()

    val gpsLatitude =
        double(
            "gps_latitude"
        )
            .nullable()

    val gpsLongitude =
        double(
            "gps_longitude"
        )
            .nullable()

    val syncStatus =
        varchar(
            "sync_status",
            50
        )
            .default("SYNCED")

    val syncedAt =
        long(
            "synced_at"
        )
            .nullable()

    override val primaryKey =
        PrimaryKey(
            farmerId
        )
}


object FarmerAddressTable :
    Table("farmer_addresses") {

    val addressId =
        varchar(
            "address_id",
            255
        )

    val farmerId =
        varchar(
            "farmer_id",
            255
        )
            .references(
                FarmerTable.farmerId
            )

    val addressType =
        varchar(
            "address_type",
            255
        )
            .nullable()

    val addressLine1 =
        varchar(
            "address_line_1",
            500
        )
            .nullable()

    val province =
        varchar(
            "province",
            255
        )
            .nullable()

    val postalCode =
        varchar(
            "postal_code",
            50
        )
            .nullable()

    val gpsLatitude =
        double(
            "gps_latitude"
        )
            .nullable()

    val gpsLongitude =
        double(
            "gps_longitude"
        )
            .nullable()

    override val primaryKey =
        PrimaryKey(
            addressId
        )
}


object FarmerRoleTable :
    Table("farmer_roles") {

    val farmerRoleId =
        varchar(
            "farmer_role_id",
            255
        )

    val farmerId =
        varchar(
            "farmer_id",
            255
        )
            .references(
                FarmerTable.farmerId
            )

    val roleId =
        varchar(
            "role_id",
            255
        )

    override val primaryKey =
        PrimaryKey(
            farmerRoleId
        )
}