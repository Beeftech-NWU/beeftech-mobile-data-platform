package com.beeftech.farmtraceability

import com.beeftech.database.dao.ReferenceDataDao
import com.beeftech.database.dao.TreatmentDao
import com.beeftech.database.entity.DeviceConfigEntry
import com.beeftech.database.entity.ReferenceItem
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.security.TokenProvider
import com.beeftech.farmtraceability.data.TreatmentApiClient
import com.beeftech.farmtraceability.data.TreatmentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/* The disease and treatment-type pickers read the cache first, and fall back to the live request. */
class TreatmentReferenceCacheTest {

    /* A DAO that fails on use: loading reference data must never touch it. */
    private val unusedTreatmentDao: TreatmentDao =
        Proxy.newProxyInstance(TreatmentDao::class.java.classLoader, arrayOf(TreatmentDao::class.java)) { _, method, _ ->
            error("TreatmentDao.${method.name} must not be called")
        } as TreatmentDao

    private class FakeDao : ReferenceDataDao() {
        val items = mutableListOf<ReferenceItem>()
        var version: String? = null
        var fail = false

        override suspend fun getActive(kind: String): List<ReferenceItem> {
            if (fail) error("database unavailable")
            return items.filter { it.kind == kind && it.active }.sortedBy { it.displayName }
        }
        override suspend fun getAll(kind: String) = items.filter { it.kind == kind }
        override suspend fun upsertItems(items: List<ReferenceItem>) = Unit
        override suspend fun ensureDisease(name: String) = Unit
        override suspend fun ensureCostType(code: String, displayName: String, sortOrder: Int, active: Boolean) = Unit
        override suspend fun updateCostType(code: String, displayName: String, sortOrder: Int, active: Boolean) = Unit
        override suspend fun getConfig(key: String): String? {
            if (fail) error("database unavailable")
            return if (key == DeviceConfigEntry.REFERENCE_DATA_VERSION) version else null
        }
        override suspend fun putConfig(entry: DeviceConfigEntry) = Unit
    }

    /*
     * The API client builds its own HTTP client, so the "server" here is an address nothing listens
     * on: a result that comes back means the cache answered, and a failure means the live request
     * was attempted.
     */
    private fun repository(dao: ReferenceDataDao?): TreatmentRepository {
        val api = TreatmentApiClient(
            tokenProvider = object : TokenProvider {
                override suspend fun token(): String? = "tok"
            },
            baseUrl = "http://127.0.0.1:1/"
        )
        return TreatmentRepository(unusedTreatmentDao, PendingSyncRepository(FakePendingSyncDao()), api, dao)
    }

    private fun item(kind: String, name: String, active: Boolean = true) =
        ReferenceItem(kind, name, name, active, 0, null, 1L)

    @Test
    fun `a filled cache is used instead of the server`() = runTest {
        val dao = FakeDao().apply {
            version = "3"
            items += item(ReferenceItem.KIND_DISEASES, "Rabies")
            items += item(ReferenceItem.KIND_DISEASES, "Anthrax")
            items += item(ReferenceItem.KIND_DISEASES, "Hidden", active = false)
            items += item(ReferenceItem.KIND_TREATMENT_TYPES, "Vaccination")
        }

        val data = repository(dao).loadReferenceData().getOrThrow()

        assertEquals(listOf("Anthrax", "Rabies"), data.diseases)
        assertEquals(listOf("Vaccination"), data.treatmentTypes)
    }

    @Test
    fun `an admin who switched every value off gets empty lists and not the live request`() = runTest {
        val dao = FakeDao().apply {
            version = "4"
            items += item(ReferenceItem.KIND_DISEASES, "Rabies", active = false)
        }

        val result = repository(dao).loadReferenceData()

        /* Success proves the server wasn't asked: it can't be reached. */
        assertTrue(result.isSuccess)
        assertEquals(emptyList<String>(), result.getOrThrow().diseases)
        assertEquals(emptyList<String>(), result.getOrThrow().treatmentTypes)
    }

    @Test
    fun `before the first pull the live request is attempted as it always was`() = runTest {
        assertTrue(repository(FakeDao()).loadReferenceData().isFailure)
    }

    @Test
    fun `without a cache the live request is attempted`() = runTest {
        assertTrue(repository(null).loadReferenceData().isFailure)
    }

    @Test
    fun `a cache that cannot be read falls back to the live request`() = runTest {
        val dao = FakeDao().apply { fail = true }

        assertTrue(repository(dao).loadReferenceData().isFailure)
    }
}
