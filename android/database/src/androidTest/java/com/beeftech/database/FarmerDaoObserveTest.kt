package com.beeftech.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.repository.FarmerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/* The farmer list: newest first, and a sync status change is emitted without re-querying. */
@RunWith(AndroidJUnit4::class)
class FarmerDaoObserveTest {

    private lateinit var context: Context

    @Before
    fun setUp() {

        context =
            ApplicationProvider.getApplicationContext()

        DatabaseProvider.close()

        context.deleteDatabase(DATABASE_NAME)

        val result =
            DatabaseProvider.initialize(
                context = context,
                passphrase = ByteArray(32) { (it + 1).toByte() }
            )

        check(result is DatabaseResult.Success) {
            "Database could not be initialized."
        }
    }

    @After
    fun tearDown() {

        DatabaseProvider.close()

        context.deleteDatabase(DATABASE_NAME)
    }

    private fun farmer(id: String) =
        FarmerEntity(
            farmer_id = id,
            client_code = id.uppercase(),
            organisation_name = "Org $id",
            vat_number = null,
            email_address = null,
            gps_latitude = 0.0,
            gps_longitude = 0.0
        )

    @Test
    fun observeAllFarmers_listsNewestFirstAndFollowsStatusChanges() = runBlocking {

        val repository =
            FarmerRepository(DatabaseProvider.getDatabase()!!.farmerDao())

        repository.addFarmer(farmer("f-1"))
        repository.addFarmer(farmer("f-2"))

        val first = repository.observeAllFarmers().first()
        assertEquals(listOf("f-2", "f-1"), first.map { it.farmer_id })
        assertEquals(listOf("PENDING", "PENDING"), first.map { it.sync_status })

        repository.markAsSynced("f-1")

        val updated =
            repository.observeAllFarmers().first { farmers -> farmers.any { it.sync_status == "SYNCED" } }
        assertEquals(listOf("PENDING", "SYNCED"), updated.map { it.sync_status })
    }

    private companion object {
        const val DATABASE_NAME = "beeftech.db"
    }
}
