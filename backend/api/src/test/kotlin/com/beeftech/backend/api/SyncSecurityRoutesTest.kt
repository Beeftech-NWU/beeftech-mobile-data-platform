package com.beeftech.backend.api

import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncSecurityRoutesTest {

    private fun event(key: String, type: String = "SYNC_WARNING", time: Long = 1_000, userId: String? = null) =
        """{"eventKey":"$key","eventType":"$type","eventTime":$time,"pendingCount":3,"warningDay":2""" +
            (userId?.let { ""","userId":"$it"""" } ?: "") + "}"

    private suspend fun HttpClient.upload(token: String, vararg events: String) =
        adminSend("POST", "/api/sync-security-events", token, """{"events":[${events.joinToString(",")}]}""")

    @Test
    fun `a phone uploads its events and a resend is counted as duplicates, not stored twice`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val admin = client.adminLogin("admin", "10001")

        val first = client.upload(worker, event("k1"), event("k2"))
        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals("2", dataOf(first.bodyAsText()).jsonObject.str("accepted"))

        val again = dataOf(client.upload(worker, event("k1"), event("k3")).bodyAsText()).jsonObject
        assertEquals("1", again.str("accepted"))
        assertEquals("1", again.str("duplicates"))

        assertEquals(3, client.adminRows("/api/sync-security-events", admin).size)
    }

    @Test
    fun `user and phone come from the token and the site from the database`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003", device = "phone-7")
        val admin = client.adminLogin("admin", "10001")

        client.upload(worker, event("k1"))

        val stored = client.adminRows("/api/sync-security-events", admin).single()
        assertEquals("jvdm", stored.str("username"))
        assertEquals("phone-7", stored.str("deviceId"))
        assertEquals("dev-site-1", stored.str("siteId"))
    }

    @Test
    fun `events claiming another user are rejected and the rest are stored`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val admin = client.adminLogin("admin", "10001")
        val adminId = client.adminUserId(admin, "admin")

        val result = dataOf(client.upload(worker, event("mine"), event("theirs", userId = adminId)).bodyAsText()).jsonObject

        assertEquals("1", result.str("accepted"))
        assertEquals("1", result.str("rejected"))
        assertEquals(listOf("jvdm"), client.adminRows("/api/sync-security-events", admin).map { it.str("username") })
    }

    @Test
    fun `more than 200 events in one request is a 400`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")

        val tooMany = Array(201) { event("k$it") }

        assertEquals(HttpStatusCode.BadRequest, client.upload(worker, *tooMany).status)
    }

    @Test
    fun `only an admin reads events and locked accounts, and no token is a 401`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val manager = client.adminLogin("fmanager", "20002")

        for (token in listOf(worker, manager)) {
            assertEquals(HttpStatusCode.Forbidden, client.adminSend("GET", "/api/sync-security-events", token).status)
            assertEquals(HttpStatusCode.Forbidden, client.adminSend("GET", "/api/sync-security-events/locked", token).status)
        }
        assertEquals(HttpStatusCode.Unauthorized, client.adminSend("GET", "/api/sync-security-events", "nope").status)
    }

    @Test
    fun `a locked account shows until an admin clears it, and a newer lock shows it again`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val admin = client.adminLogin("admin", "10001")
        val workerId = client.adminUserId(admin, "jvdm")

        client.upload(worker, event("lock-1", type = "SYNC_POLICY_ACCOUNT_LOCKED", time = 1_000))
        client.upload(worker, event("warn-1", type = "SYNC_WARNING", time = 2_000))

        val locked = client.adminRows("/api/sync-security-events/locked", admin)
        assertEquals(listOf("jvdm"), locked.map { it.str("username") })
        assertEquals("1000", locked.single().str("lockedAt"))

        /* Cleared "now" is far later than the event time, so the account drops off. */
        val cleared = client.adminSend("POST", "/api/users/$workerId/clear-sync-lock", admin)
        assertEquals(HttpStatusCode.OK, cleared.status)
        assertTrue(client.adminRows("/api/sync-security-events/locked", admin).isEmpty())

        /* A lock reported after the clear is locked again. */
        client.upload(worker, event("lock-2", type = "SYNC_POLICY_ACCOUNT_LOCKED", time = System.currentTimeMillis() + 60_000))
        assertEquals(1, client.adminRows("/api/sync-security-events/locked", admin).size)
    }

    @Test
    fun `clearing a lock is admin only, audited, and shows in that user's sync policy`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val manager = client.adminLogin("fmanager", "20002")
        val admin = client.adminLogin("admin", "10001")
        val workerId = client.adminUserId(admin, "jvdm")

        assertEquals(HttpStatusCode.Forbidden, client.adminSend("POST", "/api/users/$workerId/clear-sync-lock", manager).status)
        assertEquals(HttpStatusCode.NotFound, client.adminSend("POST", "/api/users/nobody/clear-sync-lock", admin).status)

        val before = dataOf(client.adminSend("GET", "/api/sync-policy", worker).bodyAsText()).jsonObject
        assertEquals(null, before.str("syncLockClearedAt"))

        client.adminSend("POST", "/api/users/$workerId/clear-sync-lock", admin)

        val after = dataOf(client.adminSend("GET", "/api/sync-policy", worker).bodyAsText()).jsonObject
        assertTrue(after.str("syncLockClearedAt") != null)
        /* The admin's own value is not the worker's. */
        assertEquals(null, dataOf(client.adminSend("GET", "/api/sync-policy", admin).bodyAsText()).jsonObject.str("syncLockClearedAt"))

        val log = client.adminRows("/api/audit-log?action=SYNC_LOCK_CLEAR", admin)
        assertEquals(1, log.size)
        assertEquals(workerId, log.single().str("entityId"))
        assertEquals("dev-site-1", log.single().str("siteId"))
        /* Clearing the lock does not sign the user out. */
        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/sync-policy", worker).status)
    }

    @Test
    fun `events can be filtered by user and paged`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val manager = client.adminLogin("fmanager", "20002")
        val admin = client.adminLogin("admin", "10001")

        client.upload(worker, event("a"), event("b"), event("c"))
        client.upload(manager, event("a"))
        val workerId = client.adminUserId(admin, "jvdm")

        assertEquals(3, client.adminRows("/api/sync-security-events?userId=$workerId", admin).size)
        val page1 = client.adminRows("/api/sync-security-events?limit=2", admin)
        assertEquals(2, page1.size)
        val page2 = client.adminRows("/api/sync-security-events?limit=2&before=${page1.last().str("id")}", admin)
        assertEquals(2, page2.size)
        assertFalse(page1.map { it.str("id") }.any { it in page2.map { r -> r.str("id") } })
        assertEquals(HttpStatusCode.BadRequest, client.adminSend("GET", "/api/sync-security-events?before=x", admin).status)
    }
}
