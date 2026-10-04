package com.beeftech.management

import com.beeftech.database.entity.DeviceConfigEntry
import com.beeftech.database.repository.SyncPolicyStore
import com.beeftech.database.repository.SyncWarningPolicy
import com.beeftech.database.security.TokenProvider
import com.beeftech.management.data.ManagementApiClient
import com.beeftech.management.data.PolicySyncOutcome
import com.beeftech.management.data.SyncPolicySync
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
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

class SyncPolicySyncTest {

    private val dao = FakeReferenceDataDao()
    private val store = SyncPolicyStore(dao)

    private fun sync(handler: suspend (HttpRequestData) -> Pair<HttpStatusCode, String>): SyncPolicySync {
        val engine = MockEngine { request ->
            val (status, body) = handler(request)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return SyncPolicySync(
            ManagementApiClient(
                tokenProvider = object : TokenProvider {
                    override suspend fun token(): String? = "tok"
                },
                baseUrl = "http://test-host/",
                httpClient = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
            ),
            store,
            now = { 99L }
        )
    }

    private fun policy(version: Long, days: String, wipeDay: Int = 7, hours: Int = 48) =
        """{"success":true,"message":"ok","data":{"version":$version,"warningDays":$days,"wipeDay":$wipeDay,"staleSyncAlertHours":$hours}}"""

    @Test
    fun `the first pull stores the warning days and the version`() = runTest {
        var url = ""

        val outcome = sync { url = it.url.toString(); HttpStatusCode.OK to policy(3, "[1,3,5]") }.pull()

        assertEquals("http://test-host/api/sync-policy", url)
        assertEquals(PolicySyncOutcome.Updated(3), outcome)
        assertEquals(listOf(1, 3, 5), store.current().warningDays)
        assertEquals(3L, store.version())
        assertEquals("1,3,5", dao.config[DeviceConfigEntry.SYNC_WARNING_DAYS])
    }

    @Test
    fun `the same version again changes nothing`() = runTest {
        sync { HttpStatusCode.OK to policy(3, "[1,3,5]") }.pull()
        dao.config.clear()
        dao.config[DeviceConfigEntry.SYNC_POLICY_VERSION] = "3"
        dao.config[DeviceConfigEntry.SYNC_WARNING_DAYS] = "1,3,5"

        val outcome = sync { HttpStatusCode.OK to policy(3, "[2,4,6]") }.pull()

        assertEquals(PolicySyncOutcome.UpToDate(3), outcome)
        assertEquals(listOf(1, 3, 5), store.current().warningDays)
    }

    @Test
    fun `a newer version replaces the days`() = runTest {
        sync { HttpStatusCode.OK to policy(3, "[1,3,5]") }.pull()

        val outcome = sync { HttpStatusCode.OK to policy(4, "[2,3,4]") }.pull()

        assertEquals(PolicySyncOutcome.Updated(4), outcome)
        assertEquals(listOf(2, 3, 4), store.current().warningDays)
        assertEquals(4L, store.version())
    }

    @Test
    fun `an invalid set from the server is stored as the default and its version is remembered`() = runTest {
        listOf("[6,6,6]", "[0,4,6]", "[2,4,7]", "[1,2]", "[]", "[3,2,1]").forEachIndexed { index, days ->
            val outcome = sync { HttpStatusCode.OK to policy(10L + index, days) }.pull()

            assertEquals(PolicySyncOutcome.Updated(10L + index), outcome)
            assertEquals(days, SyncWarningPolicy.DEFAULT, store.current())
            assertEquals(10L + index, store.version())
        }
    }

    @Test
    fun `a wipe day from the server is ignored and changes nothing`() = runTest {
        /* The server reports 3; the app never reads it, so the policy and its levels are unchanged. */
        sync { HttpStatusCode.OK to policy(2, "[2,4,6]", wipeDay = 3) }.pull()

        val current = store.current()

        assertEquals(listOf(2, 4, 6), current.warningDays)
        assertEquals(0, current.levelFor(0))
        assertEquals(3, current.levelFor(6))
        assertEquals(4, current.levelFor(7))
        assertEquals(7, SyncWarningPolicy.WIPE_DAY)
    }

    @Test
    fun `no connection, a missing endpoint and a refusal leave the stored policy as it was`() = runTest {
        sync { HttpStatusCode.OK to policy(3, "[1,3,5]") }.pull()
        val before = dao.config.toMap()

        val offline = sync { throw IOException("offline") }.pull()
        val missing = sync { HttpStatusCode.NotFound to "Not Found" }.pull()
        val signedOut = sync { HttpStatusCode.Unauthorized to """{"success":false,"message":"Session revoked"}""" }.pull()

        assertEquals(PolicySyncOutcome.Failed("No connection"), offline)
        assertEquals(PolicySyncOutcome.Failed("The server has no sync policy yet"), missing)
        assertEquals(PolicySyncOutcome.Failed("Signed out"), signedOut)
        assertEquals(before, dao.config.toMap())
        assertEquals(listOf(1, 3, 5), store.current().warningDays)
    }

    @Test
    fun `before any pull the policy is the default`() = runTest {
        assertSame(SyncWarningPolicy.DEFAULT, store.current())
    }

    @Test
    fun `the user's lock-cleared time is handed on at every successful pull, even when the version is unchanged`() = runTest {
        val seen = mutableListOf<Long?>()
        fun pull(body: String) = SyncPolicySync(
            ManagementApiClient(
                tokenProvider = object : TokenProvider {
                    override suspend fun token(): String? = "tok"
                },
                baseUrl = "http://test-host/",
                httpClient = HttpClient(MockEngine {
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
            ),
            store,
            now = { 99L },
            onLockClearedAt = { seen += it }
        )

        pull("""{"success":true,"message":"ok","data":{"version":3,"warningDays":[2,4,6],"wipeDay":7,"staleSyncAlertHours":48}}""").pull()
        pull("""{"success":true,"message":"ok","data":{"version":3,"warningDays":[2,4,6],"wipeDay":7,"staleSyncAlertHours":48,"syncLockClearedAt":5000}}""").pull()

        /* An older server leaves the field out, which reads as "never cleared". */
        assertEquals(listOf(null, 5000L), seen)
    }

    @Test
    fun `a failed pull never calls the lock callback`() = runTest {
        var called = false
        val failing = SyncPolicySync(
            ManagementApiClient(
                tokenProvider = object : TokenProvider {
                    override suspend fun token(): String? = "tok"
                },
                baseUrl = "http://test-host/",
                httpClient = HttpClient(MockEngine { throw IOException("offline") }) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                }
            ),
            store,
            onLockClearedAt = { called = true }
        )

        failing.pull()

        assertEquals(false, called)
    }
}
