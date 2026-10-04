package com.beeftech.farmtraceability.repository

import com.beeftech.database.BeefTechDatabase
import com.beeftech.database.RationSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TraceabilityLookupRepository(
    private val database: BeefTechDatabase
) {

    suspend fun getDestinationOptions():
            List<String> =
        withContext(
            Dispatchers.IO
        ) {

            val result =
                mutableListOf<String>()


            val statements =
                listOf(

                    """
                    SELECT
                        'Farm: ' ||
                        COALESCE(
                            NULLIF(
                                TRIM(organisation_name),
                                ''
                            ),
                            NULLIF(
                                TRIM(client_code),
                                ''
                            ),
                            farmer_id
                        )
                    FROM farmers
                    ORDER BY organisation_name
                    """.trimIndent(),

                    """
                    SELECT
                        'Location: ' ||
                        COALESCE(
                            NULLIF(
                                TRIM(location_name),
                                ''
                            ),
                            NULLIF(
                                TRIM(location_code),
                                ''
                            ),
                            location_id
                        )
                    FROM locations
                    ORDER BY location_name
                    """.trimIndent(),

                    """
                    SELECT
                        'Pen: ' ||
                        COALESCE(
                            NULLIF(
                                TRIM(name),
                                ''
                            ),
                            id
                        )
                    FROM pens
                    ORDER BY name
                    """.trimIndent()
                )


            statements.forEach { sql ->

                database
                    .openHelper
                    .readableDatabase
                    .query(sql)
                    .use { cursor ->

                        while (
                            cursor.moveToNext()
                        ) {

                            cursor
                                .getString(0)
                                ?.trim()
                                ?.takeIf {
                                    it.isNotEmpty()
                                }
                                ?.let {
                                    result += it
                                }
                        }
                    }
            }


            result.distinct()
        }


    suspend fun getRationOptions():
            List<String> =
        withContext(
            Dispatchers.IO
        ) {

            var rations =
                database
                    .rationDao()
                    .getActiveRations()


            if (
                rations.isEmpty()
            ) {

                RationSeed.execute(
                    database
                        .openHelper
                        .writableDatabase
                )

                rations =
                    database
                        .rationDao()
                        .getActiveRations()
            }


            rations
                .map {
                    it.name.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()
        }
}
