package com.beeftech.backend.api

import com.beeftech.backend.api.auth.PinHasher
import com.beeftech.backend.api.auth.UserRepository
import com.beeftech.backend.api.auth.UsersTable
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncPolicyRoutesTest {

    private suspend fun HttpClient.policy(token: String): JsonObject =
        dataOf(adminSend("GET", "/api/sync-policy", token).bodyAsText()).jsonObject

    private suspend fun HttpClient.putPolicy(token: String, days: String, hours: Int) =
        adminSend("PUT", "/api/sync-policy", token, """{"warningDays":$days,"staleSyncAlertHours":$hours}""")

    private fun JsonObject.days() = this["warningDays"]!!.jsonArray.map { it.jsonPrimitive.content.toInt() }

    @Test
    fun `anyone signed in reads the defaults, and the wipe day is reported as fixed`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")

        val policy = client.policy(worker)

        assertEquals(listOf(2, 4, 6), policy.days())
        assertEquals("7", policy.str("wipeDay"))
        assertEquals("48", policy.str("staleSyncAlertHours"))
        assertEquals("1", policy.str("version"))
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/sync-policy").status)
    }

    @Test
    fun `an admin changes the warning days and the stale hours, the version moves and it is audited`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")
        val worker = client.adminLogin("jvdm", "30003")

        val saved = client.putPolicy(admin, "[1,3,5]", 24)

        assertEquals(HttpStatusCode.OK, saved.status)
        val policy = dataOf(saved.bodyAsText()).jsonObject
        assertEquals(listOf(1, 3, 5), policy.days())
        assertEquals("24", policy.str("staleSyncAlertHours"))
        assertEquals("2", policy.str("version"))
        assertEquals("7", policy.str("wipeDay"))
        /* A phone pulling it sees the same. */
        assertEquals(listOf(1, 3, 5), client.policy(worker).days())

        val log = client.adminRows("/api/audit-log?action=SYNC_POLICY_UPDATE", admin)
        assertEquals(1, log.size)
        assertEquals("SYNC_POLICY", log.single().str("entityType"))
        assertEquals("admin", log.single().str("actorUsername"))
        assertEquals(null, log.single().str("siteId"))
        assertTrue("2,4,6->1,3,5" in log.single().str("details")!!)
        assertTrue("48->24" in log.single().str("details")!!)
    }

    @Test
    fun `saving the same values again changes nothing, no version bump and no audit row`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")

        client.putPolicy(admin, "[1,3,5]", 24)
        val again = client.putPolicy(admin, "[1,3,5]", 24)
        val defaults = client.putPolicy(admin, "[1,3,5]", 24)

        assertEquals(HttpStatusCode.OK, again.status)
        assertEquals("2", dataOf(defaults.bodyAsText()).jsonObject.str("version"))
        assertEquals(1, client.adminRows("/api/audit-log?action=SYNC_POLICY_UPDATE", admin).size)
    }

    @Test
    fun `saving the defaults on a fresh database changes nothing`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")

        val saved = client.putPolicy(admin, "[2,4,6]", 48)

        assertEquals(HttpStatusCode.OK, saved.status)
        assertEquals("1", dataOf(saved.bodyAsText()).jsonObject.str("version"))
        assertEquals(0, client.adminRows("/api/audit-log?action=SYNC_POLICY_UPDATE", admin).size)
    }

    @Test
    fun `bad warning days are refused and nothing is saved`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")

        val bad = listOf(
            "[]", "[2]", "[2,4]", "[2,4,6,7]", "[1,2,3,4]",
            "[4,2,6]", "[2,2,6]", "[6,6,6]", "[3,2,1]",
            "[0,4,6]", "[2,4,7]", "[2,4,8]", "[-1,4,6]", "[2,4,70]"
        )
        bad.forEach { days ->
            val response = client.putPolicy(admin, days, 48)
            assertEquals(HttpStatusCode.BadRequest, response.status, days)
        }
        assertTrue("wiped on day 7" in client.putPolicy(admin, "[2,4,7]", 48).bodyAsText())

        assertEquals(listOf(2, 4, 6), client.policy(admin).days())
        assertEquals("1", client.policy(admin).str("version"))
        assertEquals(0, client.adminRows("/api/audit-log?action=SYNC_POLICY_UPDATE", admin).size)
    }

    @Test
    fun `stale hours outside 12 to 336 are refused and the edges are accepted`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")

        listOf(0, 11, 337, 1000, -5).forEach {
            assertEquals(HttpStatusCode.BadRequest, client.putPolicy(admin, "[2,4,6]", it).status, "$it")
        }
        assertEquals(HttpStatusCode.OK, client.putPolicy(admin, "[2,4,6]", 12).status)
        assertEquals(HttpStatusCode.OK, client.putPolicy(admin, "[2,4,6]", 336).status)
        assertEquals("336", client.policy(admin).str("staleSyncAlertHours"))
    }

    @Test
    fun `a body without the fields is a client error and managers and workers cannot change the policy`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")
        val manager = client.adminLogin("fmanager", "20002")
        val worker = client.adminLogin("jvdm", "30003")

        listOf(manager, worker).forEach {
            assertEquals(HttpStatusCode.Forbidden, client.putPolicy(it, "[1,3,5]", 24).status)
        }
        assertEquals(HttpStatusCode.Unauthorized, client.adminSend("PUT", "/api/sync-policy", "bad", """{}""").status)
        assertTrue(client.adminSend("PUT", "/api/sync-policy", admin, """{"warningDays":[1,3,5]}""").status.value in 400..499)
        assertEquals(listOf(2, 4, 6), client.policy(admin).days())
    }

    @Test
    fun `the policy survives a restart`() {
        val file = newTestDb()

        testApplication {
            startAdminApp(file)
            val client = createClient { }
            val admin = client.adminLogin("admin", "10001")
            client.putPolicy(admin, "[1,2,3]", 72)
            client.putPolicy(admin, "[1,2,4]", 72)
        }

        testApplication {
            startAdminApp(file)
            val client = createClient { }
            val worker = client.adminLogin("jvdm", "30003")

            val policy = client.policy(worker)

            assertEquals(listOf(1, 2, 4), policy.days())
            assertEquals("72", policy.str("staleSyncAlertHours"))
            assertEquals("3", policy.str("version"))
        }
    }

    @Test
    fun `the dashboard's stale-sync alert uses the configured hours`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val manager = client.adminLogin("fmanager", "20002")
        val admin = client.adminLogin("admin", "10001")
        UserRepository().insertUser("slowpoke", "slowpoke", PinHasher.hash("11111"), 3, "dev-phone", "dev-site-1")
        val twentyHoursAgo = System.currentTimeMillis() - 20L * 60 * 60 * 1000
        transaction(DatabaseFactory.getDatabase()) {
            UsersTable.update({ UsersTable.username eq "slowpoke" }) { it[deviceLastSync] = twentyHoursAgo }
        }

        fun alerts(): List<String> =
            dataOf(client.adminSendBlocking(manager, "/api/dashboard/summary")).jsonObject["alerts"]!!.jsonArray
                .map { it.jsonObject["message"]!!.jsonPrimitive.content }

        /* 20 hours is inside the default 48. */
        assertEquals(emptyList(), alerts())

        client.putPolicy(admin, "[2,4,6]", 12)

        val flagged = alerts()
        assertEquals(1, flagged.size)
        assertTrue("slowpoke hasn't synced in over 12 hours" in flagged.single(), flagged.single())

        client.putPolicy(admin, "[2,4,6]", 48)
        assertEquals(emptyList(), alerts())
    }

    /* The alerts are read inside a non-suspending helper, so run the call to completion here. */
    private fun HttpClient.adminSendBlocking(token: String, path: String): String =
        kotlinx.coroutines.runBlocking { adminSend("GET", path, token).bodyAsText() }
}
