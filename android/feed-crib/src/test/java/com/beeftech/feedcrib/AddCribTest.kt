package com.beeftech.feedcrib

import com.beeftech.feedcrib.data.*
import com.beeftech.feedcrib.fakes.*
import com.beeftech.database.repository.PendingSyncRepository
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class AddCribTest {
    private val dao = FakeFeedCribDao()
    private val server = FakeFeedCribServer()
    private var authorised = true
    private val queue = PendingSyncRepository(FakePendingSyncDao()) { "admin-1" }
    private val repository = FeedCribRepository(dao, queue, server.client(),
        { FeedCribCaptureContext("device-1", 1_790_000_000_000) },
        userIdProvider = { "admin-1" }, canManageCribs = { authorised })
    private val form = AddCribForm(" a009 ", "site-1", "pen-1", "ration-1", "1.5")

    @Test fun offlineCreationStoresUuidMetadataAndNoReading() = runTest {
        assertTrue(repository.refreshSetup().isSuccess)
        server.offline = true
        assertTrue(repository.addCrib(form).isEmpty())
        val crib = dao.cribs.value.single()
        assertEquals("A009", crib.cribNumber)
        assertEquals("PENDING", crib.syncStatus)
        assertEquals(crib.cribUuid, java.util.UUID.fromString(crib.cribUuid).toString())
        assertEquals("admin-1", crib.createdBy)
        assertEquals("device-1", crib.creationDeviceId)
        assertNull(crib.creationGpsLat)
        assertNull(crib.creationGpsLng)
        assertNull(crib.animalsBegin)
        assertTrue(dao.entries.value.isEmpty())
        assertEquals("Pen 1", repository.observeCribs().first().single().penDescription)
        // Recreating repository models closing and reopening the capture screen.
        val reopened = FeedCribRepository(dao, queue, server.client(), { FeedCribCaptureContext("device-1") },
            userIdProvider = { "admin-1" }, canManageCribs = { true })
        assertNotNull(reopened.findCrib("a009"))
        assertFalse(reopened.addCrib(form.copy(code = "A009")).isEmpty())
    }

    @Test fun permissionsMissingSetupAndInvalidFieldsAreRejectedWithoutPartialRows() = runTest {
        assertTrue(repository.addCrib(form).keys.containsAll(listOf("site", "pen", "ration")))
        repository.refreshSetup()
        authorised = false
        assertNotNull(repository.addCrib(form)["form"])
        authorised = true
        val errors = repository.addCrib(form.copy(code = "", penId = "other-site-pen", adi = "NaN"))
        assertTrue(errors.keys.containsAll(listOf("code", "pen", "adi")))
        assertTrue(dao.cribs.value.isEmpty())
    }

    @Test fun refreshNeverDeletesPendingCribOrItsReadingAndParentUploadsFirst() = runTest {
        repository.refreshSetup()
        repository.addCrib(form)
        dao.codes.value = listOf(com.beeftech.database.entity.CribReadingCodeEntity(1, "Code 1"))
        server.offline = true
        val crib = dao.cribs.value.single()
        val saved = repository.saveEntry(crib.cribUuid, 1, 1.6)
        assertTrue(saved.saved)
        assertEquals(crib.cribUuid, saved.entry!!.cribUuid)
        server.offline = false
        // A successful empty server download cannot erase a locally created crib.
        assertTrue(repository.refreshCribs().isSuccess)
        assertEquals(crib.cribUuid, dao.cribs.value.single().cribUuid)
        assertEquals(1, dao.entries.value.size)
        repository.syncPending()
        assertEquals(listOf("cribs", "readings"), server.requestOrder)
        assertEquals("SYNCED", dao.cribs.value.single().syncStatus)
        assertEquals("SYNCED", dao.entries.value.single().syncStatus)
        assertEquals(1.5, dao.cribs.value.single().initialAdi!!, 0.0)
        repository.syncPending()
        assertEquals(1, server.creationRequests.size)
    }

    @Test fun conflictPreservesCribAndReadingsWithoutUploadingToAnotherCrib() = runTest {
        repository.refreshSetup()
        repository.addCrib(form)
        dao.codes.value = listOf(com.beeftech.database.entity.CribReadingCodeEntity(1, "Code 1"))
        server.offline = true
        val crib = dao.cribs.value.single()
        repository.saveEntry(crib.cribUuid, 1, 1.5)
        server.offline = false
        server.creationStatus = "CONFLICT"
        assertTrue(repository.syncPending().errorMessagesByRecordGuid.isNotEmpty())
        assertEquals("CONFLICT", dao.cribs.value.single().syncStatus)
        assertEquals("PENDING", dao.entries.value.single().syncStatus)
        assertEquals(0, dao.entries.value.single().syncAttempts)
        assertTrue(server.syncRequests.isEmpty())
        repository.syncPending()
        assertEquals(1, server.creationRequests.size)
        assertEquals(1, dao.entries.value.size)
    }

    @Test fun duplicateTagIsRejectedAndCodeIsScopedToSite() = runTest {
        repository.refreshSetup()
        assertTrue(repository.addCrib(form.copy(rfid = " abc ")).isEmpty())
        assertNotNull(repository.addCrib(form.copy(code = "other", rfid = "ABC"))["rfid"])
        assertNotNull(repository.addCrib(form.copy(code = " A009 "))["code"])
        val setup = repository.cachedSetup().copy(
            sites = listOf(CribSite("site-2", "Second site")),
            pens = listOf(CribPen("pen-2", "site-2", "Second pen")))
        assertTrue(validateCribForm(form.copy(siteId = "site-2", penId = "pen-2"), setup, dao.cribs.value).isEmpty())
    }

    @Test fun explicitConflictResolutionRetainsUuidAndReadings() = runTest {
        repository.refreshSetup()
        repository.addCrib(form.copy(rfid = "ABC"))
        val uuid = dao.cribs.value.single().cribUuid
        dao.codes.value = listOf(com.beeftech.database.entity.CribReadingCodeEntity(1, "Code 1"))
        server.offline = true
        repository.saveEntry(uuid, 1, 1.5)
        server.offline = false
        server.creationStatus = "CONFLICT"
        repository.syncPending()
        assertTrue(repository.resolveConflict(uuid, "A010", "OTHER").isEmpty())
        assertEquals(uuid, dao.cribs.value.single().cribUuid)
        assertEquals("A010", dao.entries.value.single().cribNumber)
        assertEquals(uuid, dao.entries.value.single().cribUuid)
        server.creationStatus = "SYNCED"
        repository.syncPending()
        assertEquals("SYNCED", dao.entries.value.single().syncStatus)
    }

    @Test fun refusedCribIsNotResentAutomaticallyButManualSyncRetriesIt() = runTest {
        repository.refreshSetup()
        repository.addCrib(form)
        dao.codes.value = listOf(com.beeftech.database.entity.CribReadingCodeEntity(1, "Code 1"))
        val crib = dao.cribs.value.single()
        server.offline = true
        repository.saveEntry(crib.cribUuid, 1, 1.5)
        server.offline = false
        server.creationStatus = "REJECTED"
        assertTrue(repository.syncPending().errorMessagesByRecordGuid.isNotEmpty())
        assertEquals("REJECTED", dao.cribs.value.single().syncStatus)
        assertEquals(1, server.creationRequests.size)
        assertTrue(server.syncRequests.isEmpty())
        assertFalse(repository.hasRetryableWork())

        repository.syncPending()
        assertEquals(1, server.creationRequests.size)

        server.creationStatus = "SYNCED"
        repository.syncPending(retryRejected = true)
        assertEquals(2, server.creationRequests.size)
        assertEquals("SYNCED", dao.cribs.value.single().syncStatus)
        assertEquals("SYNCED", dao.entries.value.single().syncStatus)
    }

    @Test fun conflictedCribWithABlockedReadingIsNotRetryableWork() = runTest {
        repository.refreshSetup()
        repository.addCrib(form)
        dao.codes.value = listOf(com.beeftech.database.entity.CribReadingCodeEntity(1, "Code 1"))
        val crib = dao.cribs.value.single()
        server.offline = true
        repository.saveEntry(crib.cribUuid, 1, 1.5)
        assertTrue(repository.hasRetryableWork())
        server.offline = false
        server.creationStatus = "CONFLICT"
        repository.syncPending()
        assertFalse(repository.hasRetryableWork())
    }

    @Test fun syncedCribTheServerNoLongerListsIsRemovedButCreationDetailsSurviveOtherwise() = runTest {
        repository.refreshSetup()
        repository.addCrib(form)
        val uuid = dao.cribs.value.single().cribUuid
        repository.syncPending()
        assertEquals("SYNCED", dao.cribs.value.single().syncStatus)
        assertEquals("admin-1", dao.cribs.value.single().createdBy)
        dao.applyDownload(emptyList(), emptyList(), emptyList(), "2026-01-01", "site-1")
        assertTrue(dao.cribs.value.none { it.cribUuid == uuid })
    }

    @Test fun adiConfigurationSupportsDifferentStepsAndLimitsWithoutFormula() {
        val rules = CribAdiRules(minimum = 0.0, maximum = 2.0, adjustmentStep = 0.001)
        assertEquals("1.501", adjustInitialAdi("1.5", 1, rules))
        assertEquals("0", adjustInitialAdi("0", -1, rules))
        assertEquals("2", adjustInitialAdi("2", 1, rules))
        assertEquals("0.001", adjustInitialAdi("NaN", 1, rules))
        val setup = CribSetup(listOf(CribSite("site-1", "Site")), listOf(CribPen("pen-1", "site-1", "Pen")),
            listOf(CribRation("ration-1", "Ration")), adiRules = rules)
        assertNotNull(validateCribForm(form.copy(adi = "2.1"), setup, emptyList())["adi"])
    }
}

