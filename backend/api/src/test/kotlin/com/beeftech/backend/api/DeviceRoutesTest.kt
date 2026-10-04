package com.beeftech.backend.api

import com.beeftech.backend.api.auth.PinHasher
import com.beeftech.backend.api.auth.SitesTable
import com.beeftech.backend.api.auth.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceRoutesTest {

    /* The module loads lazily, so touch it before using repositories. */
    private suspend fun HttpClient.seedOtherSiteWorker() {
        get("/health")
        transaction(DatabaseFactory.getDatabase()) {
            SitesTable.insert {
                it[siteId] = "other-site"
                it[name] = "Other"
                it[createdAt] = 0L
            }
        }
        UserRepository().insertUser("other-worker", "other", PinHasher.hash("40004"), 3, siteId = "other-site")
    }

    private fun revokeBody(reason: String) = """{"reason":"$reason"}"""

    @Test
    fun `a login records the phone with its model and version and later logins keep what an older app omits`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")

        client.adminLoginRaw("jvdm", "30003", extra = ""","device_model":"Pixel 7","app_version":"1.4.0"""")
        val phone = client.adminRows("/api/devices", admin).first { it.str("deviceId") == "dev-jvdm" }
        assertEquals("Pixel 7", phone.str("model"))
        assertEquals("1.4.0", phone.str("appVersion"))
        assertEquals("ACTIVE", phone.str("status"))
        assertEquals("jvdm", phone.str("lastUsername"))
        assertEquals("dev-site-1", phone.str("siteId"))
        assertEquals("[\"jvdm\"]", phone["boundUsernames"].toString())

        /* An older app sends neither; the stored values stay. */
        client.adminLoginRaw("jvdm", "30003")
        val again = client.adminRows("/api/devices", admin).first { it.str("deviceId") == "dev-jvdm" }
        assertEquals("Pixel 7", again.str("model"))
        assertEquals("1.4.0", again.str("appVersion"))
    }

    @Test
    fun `a phone that never sent a model shows none`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")
        client.adminLoginRaw("jvdm", "30003")

        val phone = client.adminRows("/api/devices", admin).first { it.str("deviceId") == "dev-jvdm" }

        assertNull(phone.str("model"))
        assertNull(phone.str("appVersion"))
    }

    @Test
    fun `admin lists every phone and a manager only their own site's`() = testApplication {
        startAdminApp()
        val client = createClient { }
        client.seedOtherSiteWorker()
        val worker = client.adminLogin("jvdm", "30003")
        client.adminLogin("other", "40004")
        val manager = client.adminLogin("fmanager", "20002")
        val admin = client.adminLogin("admin", "10001")

        assertEquals(
            setOf("dev-jvdm", "dev-other", "dev-fmanager", "dev-admin"),
            client.adminRows("/api/devices", admin).map { it.str("deviceId") }.toSet()
        )
        assertEquals(
            setOf("dev-jvdm", "dev-fmanager"),
            client.adminRows("/api/devices", manager).map { it.str("deviceId") }.toSet()
        )
        assertEquals(listOf("dev-other"), client.adminRows("/api/devices?siteId=other-site", admin).map { it.str("deviceId") })
        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/devices?siteId=dev-site-1", manager).status)
        assertEquals(HttpStatusCode.Forbidden, client.adminSend("GET", "/api/devices?siteId=other-site", manager).status)
        assertEquals(HttpStatusCode.Forbidden, client.adminSend("GET", "/api/devices", worker).status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/devices").status)
        assertEquals(HttpStatusCode.BadRequest, client.adminSend("GET", "/api/devices?status=bogus", admin).status)
    }

    @Test
    fun `revoking needs an admin and a reason and rejects unknown and already revoked phones`() = testApplication {
        startAdminApp()
        val client = createClient { }
        client.adminLogin("jvdm", "30003")
        val manager = client.adminLogin("fmanager", "20002")
        val admin = client.adminLogin("admin", "10001")

        assertEquals(HttpStatusCode.Forbidden, client.adminSend("POST", "/api/devices/dev-jvdm/revoke", manager, revokeBody("lost")).status)
        assertEquals(HttpStatusCode.BadRequest, client.adminSend("POST", "/api/devices/dev-jvdm/revoke", admin, revokeBody("  ")).status)
        assertEquals(HttpStatusCode.BadRequest, client.adminSend("POST", "/api/devices/dev-jvdm/revoke", admin, revokeBody("x".repeat(501))).status)
        assertEquals(HttpStatusCode.NotFound, client.adminSend("POST", "/api/devices/nope/revoke", admin, revokeBody("lost")).status)

        val revoked = client.adminSend("POST", "/api/devices/dev-jvdm/revoke", admin, revokeBody("  Phone lost  "))
        assertEquals(HttpStatusCode.OK, revoked.status)
        val device = dataOf(revoked.bodyAsText()).let { it as kotlinx.serialization.json.JsonObject }
        assertEquals("REVOKED", device.str("status"))
        assertEquals("Phone lost", device.str("revokeReason"))
        assertNotEquals(null, device.str("revokedAt"))
        assertEquals(HttpStatusCode.Conflict, client.adminSend("POST", "/api/devices/dev-jvdm/revoke", admin, revokeBody("again")).status)
        assertEquals(listOf("dev-jvdm"), client.adminRows("/api/devices?status=REVOKED", admin).map { it.str("deviceId") })
    }

    @Test
    fun `a revoked phone is logged out for everyone, can't sign in, and reinstating restores it`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val admin = client.adminLogin("admin", "10001")
        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/calf-registrations", worker).status)

        client.adminSend("POST", "/api/devices/dev-jvdm/revoke", admin, revokeBody("Stolen"))

        val rejected = client.adminSend("GET", "/api/calf-registrations", worker)
        assertEquals(HttpStatusCode.Unauthorized, rejected.status)
        assertTrue("Device revoked" in rejected.bodyAsText())
        assertEquals(HttpStatusCode.Unauthorized, client.adminSend("GET", "/api/profile", worker).status)
        /* Signing in from it is refused for the bound worker and for anyone else. */
        assertEquals(HttpStatusCode.Forbidden, client.adminLoginRaw("jvdm", "30003").status)
        assertEquals(HttpStatusCode.Forbidden, client.adminLoginRaw("fmanager", "20002", device = "dev-jvdm").status)
        /* Other phones are unaffected. */
        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/profile", admin).status)

        client.adminSend("POST", "/api/devices/dev-jvdm/reinstate", admin, revokeBody("Found it"))

        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/calf-registrations", worker).status)
        assertEquals(HttpStatusCode.OK, client.adminLoginRaw("jvdm", "30003").status)
        assertEquals("ACTIVE", client.adminRows("/api/devices", admin).first { it.str("deviceId") == "dev-jvdm" }.str("status"))
    }

    @Test
    fun `reinstating needs an admin, a reason and a revoked phone`() = testApplication {
        startAdminApp()
        val client = createClient { }
        client.adminLogin("jvdm", "30003")
        val manager = client.adminLogin("fmanager", "20002")
        val admin = client.adminLogin("admin", "10001")

        assertEquals(HttpStatusCode.Conflict, client.adminSend("POST", "/api/devices/dev-jvdm/reinstate", admin, revokeBody("ok")).status)
        client.adminSend("POST", "/api/devices/dev-jvdm/revoke", admin, revokeBody("lost"))
        assertEquals(HttpStatusCode.Forbidden, client.adminSend("POST", "/api/devices/dev-jvdm/reinstate", manager, revokeBody("ok")).status)
        assertEquals(HttpStatusCode.BadRequest, client.adminSend("POST", "/api/devices/dev-jvdm/reinstate", admin, revokeBody("")).status)
        assertEquals(HttpStatusCode.NotFound, client.adminSend("POST", "/api/devices/nope/reinstate", admin, revokeBody("ok")).status)
    }

    @Test
    fun `revoke and reinstate are audited with the reason and the phone's site`() = testApplication {
        startAdminApp()
        val client = createClient { }
        client.adminLoginRaw("jvdm", "30003", extra = ""","device_model":"Pixel 7"""")
        val manager = client.adminLogin("fmanager", "20002")
        val admin = client.adminLogin("admin", "10001")
        client.adminSend("POST", "/api/devices/dev-jvdm/revoke", admin, revokeBody("Stolen"))
        client.adminSend("POST", "/api/devices/dev-jvdm/reinstate", admin, revokeBody("Recovered"))
        client.adminSend("POST", "/api/devices/dev-jvdm/revoke", manager, revokeBody("nope"))

        val log = client.adminRows("/api/audit-log?entityType=DEVICE", admin)

        assertEquals(listOf("DEVICE_REINSTATE", "DEVICE_REVOKE"), log.map { it.str("action") })
        assertEquals(listOf("Recovered", "Stolen"), log.map { it.str("reason") })
        assertTrue(log.all { it.str("entityId") == "dev-jvdm" && it.str("siteId") == "dev-site-1" })
        assertTrue("Pixel 7" in log[0].str("details")!!)
        /* The manager's own site's log shows them too, but a failed attempt wrote nothing. */
        assertEquals(2, client.adminRows("/api/audit-log?entityType=DEVICE", manager).size)
    }
}
