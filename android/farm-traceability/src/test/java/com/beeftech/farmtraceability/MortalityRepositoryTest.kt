package com.beeftech.farmtraceability

import com.beeftech.database.dao.MortalityDao
import com.beeftech.database.entity.Mortality
import com.beeftech.database.entity.PendingSync
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.database.security.TokenProvider
import com.beeftech.farmtraceability.data.MortalityApiClient
import com.beeftech.farmtraceability.data.MortalityRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MortalityRepositoryTest {

    private class FakeMortalityDao : MortalityDao {
        val rows = mutableListOf<Mortality>()

        override suspend fun insert(mortality: Mortality) {
            rows += mortality
        }

        override suspend fun getAll() = rows.sortedByDescending { it.timestamp }

        override suspend fun getByAnimalId(animalId: String) =
            rows.filter { it.animalId == animalId }.sortedByDescending { it.timestamp }

        override suspend fun findByRecordGuid(recordGuid: String) =
            rows.firstOrNull { it.recordGuid == recordGuid }

        override suspend fun getUnsynced() = rows.filter { it.syncStatus != "SYNCED" }

        override suspend fun markSynced(recordGuid: String, syncedAt: Long): Int {
            val index = rows.indexOfFirst { it.recordGuid == recordGuid }
            if (index < 0) return 0
            rows[index] = rows[index].copy(syncStatus = "SYNCED", syncedAt = syncedAt)
            return 1
        }
    }

    private class Env(handler: (String) -> Pair<HttpStatusCode, String>) {
        val mortalities = FakeMortalityDao()
        val pending = FakePendingSyncDao()
        val repository = MortalityRepository(
            mortalityDao = mortalities,
            pendingSyncRepository = PendingSyncRepository(pending) { "u1" },
            apiClient = MortalityApiClient(
                tokenProvider = object : TokenProvider {
                    override suspend fun token(): String? = "tok"
                },
                baseUrl = "http://test-host/",
                httpClient = HttpClient(
                    MockEngine { request ->
                        val (status, body) = handler(request.url.encodedPath)
                        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                    }
                ) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                }
            ),
            deviceIdProvider = { "phone" }
        )
    }

    private fun syncedFor(vararg guids: String) =
        HttpStatusCode.OK to """{"success":true,"message":"ok","data":{"results":[${
            guids.joinToString(",") { """{"recordguid":"$it","animalId":"A","status":"SYNCED","serverSyncedAt":99}""" }
        }]}}"""

    @Test
    fun `saving stores locally, queues, syncs and clears the queue`() = runTest {
        lateinit var env: Env
        env = Env { _ ->
            val guid = env.mortalities.rows.single().recordGuid
            syncedFor(guid)
        }

        val outcome = env.repository.saveMortality("A-1", " Bloat ", " jvdm ")

        assertNull(outcome.syncErrorMessage)
        val row = env.mortalities.rows.single()
        assertEquals("Bloat", row.causeOfDeath)
        assertEquals("jvdm", row.responsibleWorker)
        assertEquals("SYNCED", row.syncStatus)
        assertEquals(99L, row.syncedAt)
        assertTrue(env.pending.items.isEmpty())
    }

    @Test
    fun `an offline save keeps the record pending and queued`() = runTest {
        val env = Env { _ -> throw IOException("offline") }

        val outcome = env.repository.saveMortality("A-1", "Bloat", "jvdm")

        assertNotNull(outcome.syncErrorMessage)
        val row = env.mortalities.rows.single()
        assertEquals("PENDING", row.syncStatus)
        val queued = env.pending.items.single()
        assertEquals("MORTALITY", queued.entityType)
        assertEquals(row.recordGuid, queued.entityId)
        assertEquals("u1", queued.userId)
    }

    @Test
    fun `a server-reported error keeps the record queued and counts a retry`() = runTest {
        lateinit var env: Env
        env = Env { _ ->
            val guid = env.mortalities.rows.single().recordGuid
            HttpStatusCode.OK to """{"success":true,"message":"ok","data":{"results":[
                {"recordguid":"$guid","animalId":"A-1","status":"ERROR","message":"bad row"}]}}"""
        }

        val outcome = env.repository.saveMortality("A-1", "Bloat", "jvdm")

        assertEquals("bad row", outcome.syncErrorMessage)
        assertEquals("PENDING", env.mortalities.rows.single().syncStatus)
        assertEquals(1, env.pending.items.single().retryCount)
    }

    @Test
    fun `mortalities recorded before sync existed are queued and uploaded`() = runTest {
        lateinit var env: Env
        env = Env { _ -> syncedFor(*env.mortalities.rows.map { it.recordGuid }.toTypedArray()) }
        /* Legacy rows: PENDING in Room (from the migration default) but never queued. */
        env.mortalities.rows += Mortality(animalId = "A-1", causeOfDeath = "Old", timestamp = 1, recordGuid = "legacy-1")
        env.mortalities.rows += Mortality(animalId = "A-2", causeOfDeath = "Older", timestamp = 2, recordGuid = "legacy-2")

        val outcome = env.repository.syncPending()

        assertEquals(2, outcome.syncedCount)
        assertTrue(env.mortalities.rows.all { it.syncStatus == "SYNCED" })
        assertTrue(env.pending.items.isEmpty())
    }

    @Test
    fun `syncing twice does not queue a record twice`() = runTest {
        val env = Env { _ -> throw IOException("offline") }
        env.mortalities.rows += Mortality(animalId = "A-1", causeOfDeath = "Old", timestamp = 1, recordGuid = "legacy-1")

        env.repository.syncPending()
        env.repository.syncPending()

        assertEquals(1, env.pending.items.size)
    }

    @Test
    fun `an already synced row with a leftover queue item just clears the queue`() = runTest {
        var requests = 0
        val env = Env { _ ->
            requests++
            syncedFor()
        }
        env.mortalities.rows += Mortality(
            animalId = "A-1", causeOfDeath = "Done", timestamp = 1, recordGuid = "g", syncStatus = "SYNCED"
        )
        env.pending.insert(
            PendingSync(userId = "u1", entityType = "MORTALITY", entityId = "g", operation = "CREATE", payload = "g", createdAt = 1)
        )

        env.repository.syncPending()

        assertTrue(env.pending.items.isEmpty())
        assertEquals(0, requests)
    }

    @Test
    fun `another users queue is not touched`() = runTest {
        val env = Env { _ -> syncedFor() }
        env.pending.insert(
            PendingSync(userId = "u2", entityType = "MORTALITY", entityId = "theirs", operation = "CREATE", payload = "theirs", createdAt = 1)
        )

        env.repository.syncPending()

        assertEquals(listOf("theirs"), env.pending.items.map { it.entityId })
    }
}
