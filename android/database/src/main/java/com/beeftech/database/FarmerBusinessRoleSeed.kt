package com.beeftech.database

import androidx.sqlite.db.SupportSQLiteDatabase

object FarmerBusinessRoleSeed {

    private val roles = listOf(
        1L to "Agent",
        2L to "Buyer",
        3L to "Client",
        4L to "Location",
        5L to "Feedlot",
        6L to "Owner",
        7L to "Supplier",
        8L to "Transporter"
    )

    fun execute(db: SupportSQLiteDatabase) {
        roles.forEach { (roleId, roleName) ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO farmer_business_roles
                    (business_role_id, business_role_name)
                VALUES (?, ?)
                """.trimIndent(),
                arrayOf(roleId, roleName)
            )
        }
    }
}
