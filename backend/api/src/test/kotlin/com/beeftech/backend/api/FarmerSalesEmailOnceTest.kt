package com.beeftech.backend.api

import com.beeftech.backend.api.auth.AuthPrincipal
import com.beeftech.backend.api.auth.SitesTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FarmerSalesEmailOnceTest {

    /* Records every payload; [results] decides what each call does, in order (true, false or throw). */
    private class RecordingNotifier(vararg results: Any) : FarmerSalesNotificationService {
        private val queued = ArrayDeque(results.toList())
        val payloads = mutableListOf<FarmerSalesNotificationPayload>()

        override fun notifyRegistration(payload: FarmerSalesNotificationPayload): Boolean {
            payloads += payload
            return when (val next = queued.removeFirstOrNull() ?: true) {
                is Exception -> throw next
                else -> next as Boolean
            }
        }
    }

    private val repository = FarmerRepository()

    private val principal =
        AuthPrincipal(username = "jvdm", userId = "u-1", role = 3, deviceId = "MOB_DEV_1", siteId = "site-rep")

    private val farmer =
        FarmerDto(
            farmerId = "farmer-1",
            clientCode = "KAR001",
            organisationName = "Karoo Vryburg",
            contactName = "Jan Botha",
            contactNumber = "+27 82 555 0101",
            farmSizeHa = 1250.5,
            headCount = 380,
            primaryBreed = "Bonsmara"
        )

    @BeforeTest
    fun setUp() {
        val file = Files.createTempFile("beeftech-sales-once-test", ".db")
        file.toFile().deleteOnExit()
        DatabaseFactory.init("jdbc:sqlite:$file")

        transaction(DatabaseFactory.getDatabase()) {
            SitesTable.insert {
                it[siteId] = "site-rep"
                it[name] = "Rep Farm"
                it[createdAt] = 1L
                it[farmCode] = "REP1"
                it[salesRepEmail] = "rep@example.com"
            }
        }
    }

    private fun sync(service: FarmerService, record: FarmerDto = farmer) =
        runBlocking {
            service.syncRecords(FarmerSyncRequest(deviceId = "MOB_DEV_1", records = listOf(record)), principal)
        }.results.single()

    @Test
    fun `the email carries the event, the site's rep and the new farmer fields`() {
        val notifier = RecordingNotifier()

        assertEquals("SYNCED", sync(FarmerService(repository, notifier)).status)

        val payload = notifier.payloads.single()
        assertEquals("NEW_FARMER_REGISTRATION", payload.event)
        assertEquals("rep@example.com", payload.assignedSalesmanEmail)
        assertEquals("REP1", payload.farmCode)
        assertEquals("Jan Botha", payload.contactName)
        assertEquals("+27 82 555 0101", payload.contactNumber)
        assertEquals(1250.5, payload.farmSizeHa)
        assertEquals(380, payload.headCount)
        assertEquals("Bonsmara", payload.primaryBreed)
    }

    @Test
    fun `a re-sync of the same farmer sends no second email`() {
        val notifier = RecordingNotifier()
        val service = FarmerService(repository, notifier)

        sync(service)
        val notifiedAt = repository.salesNotifiedAt("farmer-1")
        assertNotNull(notifiedAt)

        assertEquals("SYNCED", sync(service, farmer.copy(headCount = 400)).status)

        assertEquals(1, notifier.payloads.size)
        assertEquals(notifiedAt, repository.salesNotifiedAt("farmer-1"))
    }

    @Test
    fun `a failed send keeps the farmer synced and is retried on the next sync`() {
        val notifier = RecordingNotifier(IllegalStateException("SMTP down"), true)
        val service = FarmerService(repository, notifier)

        assertEquals("SYNCED", sync(service).status)
        assertNull(repository.salesNotifiedAt("farmer-1"))

        sync(service)
        assertEquals(2, notifier.payloads.size)
        assertNotNull(repository.salesNotifiedAt("farmer-1"))

        sync(service)
        assertEquals(2, notifier.payloads.size)
    }

    @Test
    fun `nothing sent (no recipient or no smtp) leaves the farmer to be emailed later`() {
        val notifier = RecordingNotifier(false, true)
        val service = FarmerService(repository, notifier)

        sync(service)
        assertNull(repository.salesNotifiedAt("farmer-1"))

        sync(service)
        assertNotNull(repository.salesNotifiedAt("farmer-1"))
        assertEquals(2, notifier.payloads.size)
    }

    @Test
    fun `only one of two claims on the same farmer wins`() {
        sync(FarmerService(repository, RecordingNotifier(false)))

        assertEquals(true, repository.claimSalesNotification("farmer-1", 10L))
        assertEquals(false, repository.claimSalesNotification("farmer-1", 11L))

        /* A release only undoes its own claim. */
        repository.releaseSalesNotification("farmer-1", 11L)
        assertEquals(10L, repository.salesNotifiedAt("farmer-1"))
    }
}
