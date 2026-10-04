package com.beeftech.backend.api

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.beeftech.backend.api.auth.UsersTable
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/* Tokens are checked against the database on every request, so admin actions take effect at once. */
class RevocationRoutesTest {

    /* A token as the server issued them before iat_ms existed. */
    private fun oldStyleToken(username: String, userId: String, role: Int, device: String): String =
        JWT.create()
            .withSubject(username)
            .withIssuer("beeftech")
            .withClaim("user_id", userId)
            .withClaim("role", role)
            .withClaim("device_id", device)
            .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
            .sign(Algorithm.HMAC256(System.getenv("BEEFTECH_JWT_SECRET") ?: "beeftech-secret"))

    @Test
    fun `deactivating a user ends their token at once on every kind of route`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val admin = client.adminLogin("admin", "10001")
        val workerId = client.adminUserId(admin, "jvdm")
        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/calf-registrations", worker).status)
        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/profile", worker).status)

        client.adminSend("PATCH", "/api/users/$workerId", admin, """{"active":false}""")

        /* A scoped sync-style route, and a route that used to only check the signature. */
        val scoped = client.adminSend("GET", "/api/calf-registrations", worker)
        assertEquals(HttpStatusCode.Unauthorized, scoped.status)
        assertTrue("Session revoked" in scoped.bodyAsText())
        assertEquals(HttpStatusCode.Unauthorized, client.adminSend("GET", "/api/profile", worker).status)
        assertEquals(HttpStatusCode.Unauthorized, client.adminSend("GET", "/api/treatments/reference-data", worker).status)
        assertEquals(HttpStatusCode.Unauthorized, client.adminSend("GET", "/api/calf-registrations/T-1/certificate", worker).status)
        assertEquals(HttpStatusCode.Unauthorized, client.adminLoginRaw("jvdm", "30003").status)

        /* Reactivating lets them sign in again, but the old token stays dead. */
        client.adminSend("PATCH", "/api/users/$workerId", admin, """{"active":true}""")
        assertEquals(HttpStatusCode.Unauthorized, client.adminSend("GET", "/api/profile", worker).status)
        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/profile", client.adminLogin("jvdm", "30003")).status)
    }

    @Test
    fun `unbinding a phone and resetting a PIN end the old tokens while a fresh login works`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val manager = client.adminLogin("fmanager", "20002")
        val admin = client.adminLogin("admin", "10001")
        val workerId = client.adminUserId(admin, "jvdm")

        val beforeUnbind = client.adminLogin("jvdm", "30003")
        client.adminSend("POST", "/api/users/$workerId/unbind-device", manager)
        assertEquals(HttpStatusCode.Unauthorized, client.adminSend("GET", "/api/profile", beforeUnbind).status)
        val newPhone = client.adminLogin("jvdm", "30003", device = "dev-new-phone")
        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/profile", newPhone).status)

        client.adminSend("POST", "/api/users/$workerId/reset-pin", manager, """{"pin":"99999"}""")
        assertEquals(HttpStatusCode.Unauthorized, client.adminSend("GET", "/api/profile", newPhone).status)
        val afterReset = client.adminLogin("jvdm", "99999", device = "dev-new-phone")
        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/profile", afterReset).status)
    }

    @Test
    fun `a token from before iat_ms existed works until a cut-off is set for that user`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")
        val workerId = client.adminUserId(admin, "jvdm")
        val old = oldStyleToken("jvdm", workerId, 3, "dev-jvdm")

        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/profile", old).status)
        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/calf-registrations", old).status)

        /* Any cut-off rejects it, because it carries no issue time to compare. */
        transaction(DatabaseFactory.getDatabase()) {
            UsersTable.update({ UsersTable.userId eq workerId }) { it[tokensValidAfter] = 1L }
        }
        assertEquals(HttpStatusCode.Unauthorized, client.adminSend("GET", "/api/profile", old).status)
        /* A token issued now is later than the cut-off. */
        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/profile", client.adminLogin("jvdm", "30003")).status)
    }

    @Test
    fun `a token for a user that no longer exists is rejected`() = testApplication {
        startAdminApp()
        val client = createClient { }
        client.adminSend("GET", "/health", "x")

        val ghost = oldStyleToken("ghost", "no-such-id", 3, "dev-ghost")

        assertEquals(HttpStatusCode.Unauthorized, client.adminSend("GET", "/api/profile", ghost).status)
    }

    @Test
    fun `a role change applies to the next request without logging in again`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val admin = client.adminLogin("admin", "10001")
        val workerId = client.adminUserId(admin, "jvdm")
        assertEquals(HttpStatusCode.Forbidden, client.adminSend("GET", "/api/users", worker).status)

        client.adminSend("PATCH", "/api/users/$workerId", admin, """{"role":2}""")

        /* The token still says WORKER; the database says MANAGER. */
        assertEquals(HttpStatusCode.OK, client.adminSend("GET", "/api/users", worker).status)
    }

    @Test
    fun `any authenticated request counts as last contact, throttled`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val admin = client.adminLogin("admin", "10001")
        val workerId = client.adminUserId(admin, "jvdm")

        fun lastSync() = transaction(DatabaseFactory.getDatabase()) {
            UsersTable.select(UsersTable.deviceLastSync).where { UsersTable.userId eq workerId }.single()[UsersTable.deviceLastSync]
        }

        /* Just logged in, so within the throttle window and left alone. */
        val atLogin = lastSync()!!
        client.adminSend("GET", "/api/profile", worker)
        assertEquals(atLogin, lastSync())

        /* A week-old value is stale, so the next request refreshes it. */
        transaction(DatabaseFactory.getDatabase()) {
            UsersTable.update({ UsersTable.userId eq workerId }) { it[deviceLastSync] = 1_000L }
        }
        client.adminSend("GET", "/api/profile", worker)
        assertTrue(lastSync()!! > 1_000L)
    }
}
