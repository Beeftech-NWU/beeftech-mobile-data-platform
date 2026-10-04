package com.beeftech.management

import com.beeftech.database.entity.DeviceConfigEntry
import com.beeftech.database.entity.ReferenceItem
import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.ReferenceDataSync
import com.beeftech.management.data.ReferenceSyncOutcome
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ReferenceDataSyncTest {

    private val dao = FakeReferenceDataDao()

    private fun sync(handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>): ReferenceDataSync {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return ReferenceDataSync(
            ManagementApiClient(
                tokenProvider = object : TokenProvider {
                    override suspend fun token(): String? = "tok"
                },
                baseUrl = "http://test-host/",
                httpClient = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
            ),
            dao,
            now = { 1234L }
        )
    }

    private val fullBody = """{"success":true,"message":"ok","data":{"version":3,
        "diseases":[{"id":1,"name":"Anthrax","active":true},{"id":2,"name":"Rabies","active":false}],
        "treatmentTypes":[{"id":5,"name":"Vaccination","active":true}],
        "costTypes":[{"code":"AUCTION","displayName":"Auction fees","sortOrder":20,"active":true},
                     {"code":"TREATMENT","displayName":"Treatment","sortOrder":4,"active":false}]}}"""

    private val unchangedBody = """{"success":true,"message":"ok","data":{"version":3,"unchanged":true}}"""

    @Test
    fun `a first pull asks for everything, stores it and remembers the version`() = runTest {
        var url = ""

        val outcome = sync { url = it.url.toString(); HttpStatusCode.OK to fullBody }.pull()

        assertEquals("http://test-host/api/reference-data", url)
        assertEquals(ReferenceSyncOutcome.Updated(3), outcome)
        assertEquals("3", dao.config[DeviceConfigEntry.REFERENCE_DATA_VERSION])
        assertEquals(listOf("Anthrax"), dao.getActive(ReferenceItem.KIND_DISEASES).map { it.displayName })
        assertEquals(listOf("Anthrax", "Rabies"), dao.getAll(ReferenceItem.KIND_DISEASES).map { it.displayName })
        assertEquals(listOf("Vaccination"), dao.getActive(ReferenceItem.KIND_TREATMENT_TYPES).map { it.displayName })
        /* Inactive values still reach the diseases table, so records that use them resolve. */
        assertEquals(setOf("Anthrax", "Rabies"), dao.diseases)
        assertEquals(Triple("Auction fees", 20, true), dao.costTypes["AUCTION"])
        /* The Treatment cost type can't be switched off from the server. */
        assertEquals(true, dao.costTypes["TREATMENT"]!!.third)
    }

    @Test
    fun `a later pull sends the stored version and an unchanged answer applies nothing`() = runTest {
        sync { HttpStatusCode.OK to fullBody }.pull()
        var url = ""

        val outcome = sync { url = it.url.toString(); HttpStatusCode.OK to unchangedBody }.pull()

        assertEquals("http://test-host/api/reference-data?ifVersion=3", url)
        assertEquals(ReferenceSyncOutcome.UpToDate(3), outcome)
        assertEquals(1, dao.applyCount)
    }

    @Test
    fun `a newer version is applied and values the server no longer lists stay on the device`() = runTest {
        sync { HttpStatusCode.OK to fullBody }.pull()
        val newer = """{"success":true,"message":"ok","data":{"version":4,
            "diseases":[{"id":1,"name":"Anthrax","active":false}],"treatmentTypes":[],"costTypes":[]}}"""

        val outcome = sync { HttpStatusCode.OK to newer }.pull()

        assertEquals(ReferenceSyncOutcome.Updated(4), outcome)
        assertEquals("4", dao.config[DeviceConfigEntry.REFERENCE_DATA_VERSION])
        assertEquals(emptyList<String>(), dao.getActive(ReferenceItem.KIND_DISEASES).map { it.displayName })
        /* Rabies and Vaccination weren't in the new snapshot, and nothing was deleted. */
        assertEquals(listOf("Anthrax", "Rabies"), dao.getAll(ReferenceItem.KIND_DISEASES).map { it.displayName })
        assertEquals(listOf("Vaccination"), dao.getActive(ReferenceItem.KIND_TREATMENT_TYPES).map { it.displayName })
        assertTrue("AUCTION" in dao.costTypes)
    }

    @Test
    fun `an older server that leaves lists out adds nothing and removes nothing`() = runTest {
        sync { HttpStatusCode.OK to fullBody }.pull()
        val sparse = """{"success":true,"message":"ok","data":{"version":5}}"""

        val outcome = sync { HttpStatusCode.OK to sparse }.pull()

        assertEquals(ReferenceSyncOutcome.Updated(5), outcome)
        assertEquals(2, dao.getAll(ReferenceItem.KIND_DISEASES).size)
        assertEquals(1, dao.getAll(ReferenceItem.KIND_TREATMENT_TYPES).size)
    }

    @Test
    fun `no connection, a missing endpoint and a refusal leave the cache exactly as it was`() = runTest {
        sync { HttpStatusCode.OK to fullBody }.pull()
        val before = dao.items.toMap() to dao.config.toMap()

        val offline = sync { throw IOException("offline") }.pull()
        val missing = sync { HttpStatusCode.NotFound to "Not Found" }.pull()
        val signedOut = sync { HttpStatusCode.Unauthorized to """{"success":false,"message":"Session revoked"}""" }.pull()
        val forbidden = sync { HttpStatusCode.Forbidden to """{"success":false,"message":"Nope"}""" }.pull()

        assertEquals(ReferenceSyncOutcome.Failed("No connection"), offline)
        assertEquals(ReferenceSyncOutcome.Failed("The server has no reference data yet"), missing)
        assertEquals(ReferenceSyncOutcome.Failed("Signed out"), signedOut)
        assertEquals(ReferenceSyncOutcome.Failed("Nope"), forbidden)
        assertEquals(before, dao.items.toMap() to dao.config.toMap())
        assertEquals(1, dao.applyCount)
    }

    @Test
    fun `before any pull there is no stored version`() = runTest {
        assertNull(dao.getConfig(DeviceConfigEntry.REFERENCE_DATA_VERSION))
        assertFalse(dao.items.isNotEmpty())
    }
}
