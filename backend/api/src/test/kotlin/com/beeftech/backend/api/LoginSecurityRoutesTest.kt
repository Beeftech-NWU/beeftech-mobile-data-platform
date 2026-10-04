package com.beeftech.backend.api

import com.beeftech.backend.api.auth.LoginSecurityRepository
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginSecurityRoutesTest {

    private suspend fun io.ktor.client.HttpClient.fail(username: String, times: Int) =
        repeat(times) { adminLoginRaw(username, "00000") }

    @Test
    fun `the fifth wrong PIN locks the user and even the right PIN is then refused`() = testApplication {
        startAdminApp()
        val client = createClient { }

        repeat(4) { assertEquals(HttpStatusCode.Unauthorized, client.adminLoginRaw("jvdm", "00000").status) }
        assertEquals(HttpStatusCode.Locked, client.adminLoginRaw("jvdm", "00000").status)
        assertEquals(HttpStatusCode.Locked, client.adminLoginRaw("jvdm", "30003").status)
        /* Another user is unaffected. */
        assertEquals(HttpStatusCode.OK, client.adminLoginRaw("fmanager", "20002").status)
    }

    @Test
    fun `a lockout survives a server restart on the same database`() {
        val file = newTestDb()

        testApplication {
            startAdminApp(file)
            val client = createClient { }
            client.fail("jvdm", 5)
            assertEquals(HttpStatusCode.Locked, client.adminLoginRaw("jvdm", "30003").status)
        }

        testApplication {
            startAdminApp(file)
            val client = createClient { }
            /* The in-memory map of old would have forgotten; the database has not. */
            assertEquals(HttpStatusCode.Locked, client.adminLoginRaw("jvdm", "30003").status)
        }
    }

    @Test
    fun `a successful login clears the failed attempts`() = testApplication {
        startAdminApp()
        val client = createClient { }

        client.fail("jvdm", 4)
        assertEquals(HttpStatusCode.OK, client.adminLoginRaw("jvdm", "30003").status)
        /* Four more wrong ones don't lock, because the count started again. */
        client.fail("jvdm", 4)
        assertEquals(HttpStatusCode.OK, client.adminLoginRaw("jvdm", "30003").status)
    }

    @Test
    fun `after a lock runs out the user gets a fresh set of attempts`() = testApplication {
        startAdminApp()
        val client = createClient { }
        client.get("/health")
        val repo = LoginSecurityRepository()

        val locked = runBlocking { repeat(5) { repo.recordFailure("jvdm", 1_000L, 5, 300_000L) }; repo.state("jvdm")!! }
        assertEquals(301_000L, locked.lockedUntil)

        /* One wrong PIN after the lock expired counts as the first again, not the sixth. */
        val after = runBlocking { repo.recordFailure("jvdm", 400_000L, 5, 300_000L) }
        assertEquals(1, after.failedAttempts)
        assertNull(after.lockedUntil)
    }

    @Test
    fun `resetting a PIN lifts the lockout and so does unlock-login, within scope`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val manager = client.adminLogin("fmanager", "20002")
        val admin = client.adminLogin("admin", "10001")
        val workerId = client.adminUserId(admin, "jvdm")

        client.fail("jvdm", 5)
        assertEquals(HttpStatusCode.Locked, client.adminLoginRaw("jvdm", "30003").status)
        client.adminSend("POST", "/api/users/$workerId/reset-pin", manager, """{"pin":"99999"}""")
        assertEquals(HttpStatusCode.OK, client.adminLoginRaw("jvdm", "99999").status)

        client.fail("jvdm", 5)
        assertEquals(HttpStatusCode.Locked, client.adminLoginRaw("jvdm", "99999").status)
        assertEquals(HttpStatusCode.OK, client.adminSend("POST", "/api/users/$workerId/unlock-login", manager).status)
        assertEquals(HttpStatusCode.OK, client.adminLoginRaw("jvdm", "99999").status)
    }

    @Test
    fun `unlock-login is refused for workers and out-of-scope users and is audited`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val manager = client.adminLogin("fmanager", "20002")
        val admin = client.adminLogin("admin", "10001")
        val workerId = client.adminUserId(admin, "jvdm")
        val managerId = client.adminUserId(admin, "fmanager")

        assertEquals(HttpStatusCode.Forbidden, client.adminSend("POST", "/api/users/$workerId/unlock-login", worker).status)
        /* A manager only reaches workers on their own site, so another manager is not found. */
        assertEquals(HttpStatusCode.NotFound, client.adminSend("POST", "/api/users/$managerId/unlock-login", manager).status)
        assertEquals(HttpStatusCode.NotFound, client.adminSend("POST", "/api/users/nope/unlock-login", admin).status)

        client.adminSend("POST", "/api/users/$workerId/unlock-login", manager)
        val log = client.adminRows("/api/audit-log?action=LOGIN_UNLOCK", admin)
        assertEquals(1, log.size)
        assertEquals(workerId, log.single().str("entityId"))
        assertEquals("fmanager", log.single().str("actorUsername"))
    }

    @Test
    fun `every login outcome writes an event and none holds a PIN`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")
        val workerId = client.adminUserId(admin, "jvdm")

        client.adminLoginRaw("jvdm", "30003")                                           // SUCCESS
        client.adminLoginRaw("jvdm", "11111")                                           // BAD_CREDENTIALS
        client.adminLoginRaw("nobody", "22222")                                         // UNKNOWN_USER
        client.adminLoginRaw("jvdm", "30003", device = "dev-other-phone")               // WRONG_DEVICE
        client.adminSend("POST", "/api/devices/dev-jvdm/revoke", admin, """{"reason":"lost"}""")
        client.adminLoginRaw("jvdm", "30003")                                           // DEVICE_REVOKED
        client.adminSend("POST", "/api/devices/dev-jvdm/reinstate", admin, """{"reason":"ok"}""")
        client.adminSend("PATCH", "/api/users/$workerId", admin, """{"active":false}""")
        client.adminLoginRaw("jvdm", "30003")                                           // INACTIVE
        client.adminSend("PATCH", "/api/users/$workerId", admin, """{"active":true}""")
        client.fail("jvdm", 5)                                                          // 5 x BAD_CREDENTIALS, locks
        client.adminLoginRaw("jvdm", "30003")                                           // LOCKED

        val events = client.adminRows("/api/login-events?limit=500", admin)
        val outcomes = events.map { it.str("outcome") }.toSet()

        assertEquals(
            setOf("SUCCESS", "BAD_CREDENTIALS", "UNKNOWN_USER", "WRONG_DEVICE", "DEVICE_REVOKED", "INACTIVE", "LOCKED"),
            outcomes
        )
        val unknown = events.first { it.str("outcome") == "UNKNOWN_USER" }
        assertEquals("nobody", unknown.str("usernameAttempted"))
        assertNull(unknown.str("userId"))
        val success = events.first { it.str("outcome") == "SUCCESS" && it.str("usernameAttempted") == "jvdm" }
        assertEquals(workerId, success.str("userId"))
        assertEquals("dev-site-1", success.str("siteId"))
        val text = client.adminSend("GET", "/api/login-events?limit=500", admin).bodyAsText()
        listOf("30003", "11111", "22222", "00000", "pin").forEach { assertFalse(it in text, "found $it") }
    }

    @Test
    fun `login events filter, page newest first, and are admin only`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val manager = client.adminLogin("fmanager", "20002")
        val admin = client.adminLogin("admin", "10001")
        client.fail("jvdm", 2)

        val all = client.adminRows("/api/login-events", admin)
        assertEquals(all.map { it.str("id")!!.toLong() }.sortedDescending(), all.map { it.str("id")!!.toLong() })
        assertEquals(2, client.adminRows("/api/login-events?outcome=BAD_CREDENTIALS", admin).size)
        /* jvdm and fmanager signed in, and jvdm failed twice; the admin has no site. */
        assertEquals(4, client.adminRows("/api/login-events?siteId=dev-site-1", admin).size)

        val first = client.adminRows("/api/login-events?limit=2", admin)
        val rest = client.adminRows("/api/login-events?before=${first.last().str("id")}", admin)
        assertEquals(all.size, first.size + rest.size)
        assertTrue(rest.all { it.str("id")!!.toLong() < first.last().str("id")!!.toLong() })

        assertEquals(HttpStatusCode.Forbidden, client.adminSend("GET", "/api/login-events", manager).status)
        assertEquals(HttpStatusCode.Forbidden, client.adminSend("GET", "/api/login-events", worker).status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/login-events").status)
        assertEquals(HttpStatusCode.BadRequest, client.adminSend("GET", "/api/login-events?before=abc", admin).status)
    }

    @Test
    fun `the lockouts list shows who is locked now and clears on unlock`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val manager = client.adminLogin("fmanager", "20002")
        val admin = client.adminLogin("admin", "10001")
        val workerId = client.adminUserId(admin, "jvdm")

        assertEquals(emptyList(), client.adminRows("/api/login-security/lockouts", admin))
        client.fail("jvdm", 5)

        val locked = client.adminRows("/api/login-security/lockouts", admin).single()
        assertEquals("jvdm", locked.str("username"))
        assertEquals("5", locked.str("failedAttempts"))
        assertTrue(locked.str("lockedUntil")!!.toLong() > System.currentTimeMillis())
        assertEquals(HttpStatusCode.Forbidden, client.adminSend("GET", "/api/login-security/lockouts", manager).status)

        client.adminSend("POST", "/api/users/$workerId/unlock-login", admin)
        assertEquals(emptyList(), client.adminRows("/api/login-security/lockouts", admin))
    }
}
