package com.beeftech.backend.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReferenceDataRoutesTest {

    private suspend fun HttpClient.snapshot(token: String, query: String = ""): JsonObject =
        dataOf(adminSend("GET", "/api/reference-data$query", token).bodyAsText()).jsonObject

    private suspend fun HttpClient.version(token: String) = snapshot(token).str("version")!!.toLong()

    private fun JsonObject.list(key: String): JsonArray = this[key]!!.jsonArray

    private fun JsonObject.entry(key: String, match: (JsonObject) -> Boolean): JsonObject =
        list(key).map { it.jsonObject }.first(match)

    private suspend fun HttpClient.create(token: String, kind: String, body: String) =
        adminSend("POST", "/api/reference-data/$kind", token, body)

    private suspend fun HttpClient.setActive(token: String, kind: String, id: String, active: Boolean) =
        adminSend("PATCH", "/api/reference-data/$kind/$id", token, """{"active":$active}""")

    @Test
    fun `anyone signed in reads every value, inactive ones included, and an unauthenticated call is refused`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")

        val data = client.snapshot(worker)

        assertEquals(16, data.list("diseases").size)
        assertEquals(11, data.list("treatmentTypes").size)
        assertEquals(9, data.list("costTypes").size)
        assertEquals("TRANSPORT", data.list("costTypes")[0].jsonObject.str("code"))
        assertEquals("Transport", data.list("costTypes")[0].jsonObject.str("displayName"))
        assertTrue(data.list("diseases").all { it.jsonObject.str("active") == "true" })
        assertEquals("Anthrax", data.list("diseases")[0].jsonObject.str("name"))
        assertEquals("1", data.str("version"))
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/reference-data").status)
    }

    @Test
    fun `ifVersion returns only the version when the device is current and everything when it is stale`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val admin = client.adminLogin("admin", "10001")

        val current = client.snapshot(worker, "?ifVersion=1")
        assertEquals("true", current.str("unchanged"))
        assertEquals("1", current.str("version"))
        assertNull(current.str("diseases"))
        assertNull(current.str("costTypes"))

        client.create(admin, "diseases", """{"name":"Pinkeye"}""")
        val stale = client.snapshot(worker, "?ifVersion=1")
        /* A false flag is the default, so the server leaves it out; clients treat absent as false. */
        assertNull(stale.str("unchanged"))
        assertEquals("2", stale.str("version"))
        assertEquals(17, stale.list("diseases").size)

        assertEquals("true", client.snapshot(worker, "?ifVersion=2").str("unchanged"))
        assertEquals(HttpStatusCode.BadRequest, client.adminSend("GET", "/api/reference-data?ifVersion=abc", worker).status)
    }

    @Test
    fun `only an admin can add or change a value`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val worker = client.adminLogin("jvdm", "30003")
        val manager = client.adminLogin("fmanager", "20002")

        listOf(worker, manager).forEach {
            assertEquals(HttpStatusCode.Forbidden, client.create(it, "diseases", """{"name":"Pinkeye"}""").status)
            assertEquals(HttpStatusCode.Forbidden, client.setActive(it, "diseases", "1", false).status)
        }
        assertEquals(1L, client.version(worker))
    }

    @Test
    fun `adding a disease or treatment type bumps the version, shows up, and rejects bad or duplicate names`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")

        val disease = client.create(admin, "diseases", """{"name":"  Pinkeye  "}""")
        assertEquals(HttpStatusCode.Created, disease.status)
        val body = dataOf(disease.bodyAsText()).jsonObject
        assertEquals("2", body.str("version"))
        assertEquals("Pinkeye", body["item"]!!.jsonObject.str("name"))
        assertEquals("true", body["item"]!!.jsonObject.str("active"))

        assertEquals(HttpStatusCode.Created, client.create(admin, "treatment-types", """{"name":"Hoof trimming"}""").status)
        assertEquals(3L, client.version(admin))
        assertTrue(client.snapshot(admin).list("diseases").any { it.jsonObject.str("name") == "Pinkeye" })
        assertTrue(client.snapshot(admin).list("treatmentTypes").any { it.jsonObject.str("name") == "Hoof trimming" })

        /* None of these change anything, so the version stays put. */
        assertEquals(HttpStatusCode.Conflict, client.create(admin, "diseases", """{"name":"pinkeye"}""").status)
        assertEquals(HttpStatusCode.Conflict, client.create(admin, "diseases", """{"name":"ANTHRAX"}""").status)
        assertEquals(HttpStatusCode.Conflict, client.create(admin, "treatment-types", """{"name":"deworming"}""").status)
        assertEquals(HttpStatusCode.BadRequest, client.create(admin, "diseases", """{"name":"   "}""").status)
        assertEquals(HttpStatusCode.BadRequest, client.create(admin, "diseases", """{}""").status)
        assertEquals(HttpStatusCode.BadRequest, client.create(admin, "diseases", """{"name":"${"x".repeat(101)}"}""").status)
        assertEquals(HttpStatusCode.NotFound, client.create(admin, "pens", """{"name":"A1"}""").status)
        assertEquals(3L, client.version(admin))
    }

    @Test
    fun `adding a cost type checks the code and name and orders it after the others by default`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")

        val created = client.create(admin, "cost-types", """{"code":"VET_CALLOUT","displayName":"Vet call-out"}""")
        assertEquals(HttpStatusCode.Created, created.status)
        val item = dataOf(created.bodyAsText()).jsonObject["item"]!!.jsonObject
        assertEquals("VET_CALLOUT", item.str("id"))
        assertEquals("Vet call-out", item.str("name"))
        assertEquals("9", item.str("sortOrder"))

        val explicit = client.create(admin, "cost-types", """{"code":"AUCTION","displayName":"Auction fees","sortOrder":2}""")
        assertEquals("2", dataOf(explicit.bodyAsText()).jsonObject["item"]!!.jsonObject.str("sortOrder"))

        val costTypes = client.snapshot(admin).list("costTypes").map { it.jsonObject.str("code") }
        assertEquals(11, costTypes.size)
        assertEquals("VET_CALLOUT", costTypes.last())

        listOf("transport", "1ABC", "A", "HAS SPACE", "WAY_TOO_LONG_${"X".repeat(70)}", "").forEach { bad ->
            assertEquals(
                HttpStatusCode.BadRequest,
                client.create(admin, "cost-types", """{"code":"$bad","displayName":"x"}""").status,
                bad
            )
        }
        assertEquals(HttpStatusCode.BadRequest, client.create(admin, "cost-types", """{"code":"OK_CODE","displayName":" "}""").status)
        assertEquals(HttpStatusCode.BadRequest, client.create(admin, "cost-types", """{"code":"OK_CODE"}""").status)
        assertEquals(HttpStatusCode.Conflict, client.create(admin, "cost-types", """{"code":"TRANSPORT","displayName":"Again"}""").status)
        assertEquals(3L, client.version(admin))
    }

    @Test
    fun `deactivating hides a value from the old endpoint, keeps it in the snapshot, and repeating it changes nothing`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")
        val worker = client.adminLogin("jvdm", "30003")
        val rabiesId = client.snapshot(worker).entry("diseases") { it.str("name") == "Rabies" }.str("id")!!

        val off = client.setActive(admin, "diseases", rabiesId, false)
        assertEquals(HttpStatusCode.OK, off.status)
        assertEquals("2", dataOf(off.bodyAsText()).jsonObject.str("version"))

        assertEquals("false", client.snapshot(worker).entry("diseases") { it.str("name") == "Rabies" }.str("active"))
        val oldEndpoint = client.adminSend("GET", "/api/treatments/reference-data", worker).bodyAsText()
        assertFalse("Rabies" in oldEndpoint)
        assertTrue("Anthrax" in oldEndpoint)

        /* Same flag again: 200, no bump, no new audit row. */
        assertEquals(HttpStatusCode.OK, client.setActive(admin, "diseases", rabiesId, false).status)
        assertEquals(2L, client.version(admin))

        val on = client.setActive(admin, "diseases", rabiesId, true)
        assertEquals("3", dataOf(on.bodyAsText()).jsonObject.str("version"))
        assertTrue("Rabies" in client.adminSend("GET", "/api/treatments/reference-data", worker).bodyAsText())
    }

    @Test
    fun `treatment types and cost types can be turned off but the Treatment cost type is protected`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")
        val data = client.snapshot(admin)
        val typeId = data.entry("treatmentTypes") { it.str("name") == "Vaccination" }.str("id")!!

        assertEquals(HttpStatusCode.OK, client.setActive(admin, "treatment-types", typeId, false).status)
        assertEquals(HttpStatusCode.OK, client.setActive(admin, "cost-types", "FEED", false).status)
        assertEquals("false", client.snapshot(admin).entry("costTypes") { it.str("code") == "FEED" }.str("active"))

        val protectedResponse = client.setActive(admin, "cost-types", "TREATMENT", false)
        assertEquals(HttpStatusCode.Conflict, protectedResponse.status)
        assertTrue("Treatment" in protectedResponse.bodyAsText())
        assertEquals("true", client.snapshot(admin).entry("costTypes") { it.str("code") == "TREATMENT" }.str("active"))
        assertEquals(3L, client.version(admin))
    }

    @Test
    fun `unknown kinds, ids and non-numeric ids are not found`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")

        assertEquals(HttpStatusCode.NotFound, client.setActive(admin, "pens", "1", false).status)
        assertEquals(HttpStatusCode.NotFound, client.setActive(admin, "diseases", "99999", false).status)
        assertEquals(HttpStatusCode.NotFound, client.setActive(admin, "diseases", "abc", false).status)
        assertEquals(HttpStatusCode.NotFound, client.setActive(admin, "cost-types", "NOPE", false).status)
        assertEquals(1L, client.version(admin))
    }

    @Test
    fun `a restart neither bumps the version nor undoes an admin's deactivation`() {
        val file = newTestDb()
        var rabiesId = ""

        testApplication {
            startAdminApp(file)
            val client = createClient { }
            val admin = client.adminLogin("admin", "10001")
            rabiesId = client.snapshot(admin).entry("diseases") { it.str("name") == "Rabies" }.str("id")!!
            client.setActive(admin, "diseases", rabiesId, false)
            client.create(admin, "diseases", """{"name":"Pinkeye"}""")
            assertEquals(3L, client.version(admin))
        }

        testApplication {
            startAdminApp(file)
            val client = createClient { }
            val admin = client.adminLogin("admin", "10001")
            val data = client.snapshot(admin)

            assertEquals(3L, data.str("version")!!.toLong())
            assertEquals("false", data.entry("diseases") { it.str("id") == rabiesId }.str("active"))
            assertEquals(17, data.list("diseases").size)
            assertEquals(9, data.list("costTypes").size)
        }
    }

    @Test
    fun `changes are audited with the admin and no site, a no-op and a refusal leave no row`() = testApplication {
        startAdminApp()
        val client = createClient { }
        val admin = client.adminLogin("admin", "10001")
        val manager = client.adminLogin("fmanager", "20002")
        val rabiesId = client.snapshot(admin).entry("diseases") { it.str("name") == "Rabies" }.str("id")!!

        client.create(admin, "diseases", """{"name":"Pinkeye"}""")
        client.create(admin, "cost-types", """{"code":"AUCTION","displayName":"Auction fees"}""")
        client.setActive(admin, "diseases", rabiesId, false)
        client.setActive(admin, "diseases", rabiesId, false)
        client.create(admin, "diseases", """{"name":"pinkeye"}""")
        client.setActive(admin, "cost-types", "TREATMENT", false)
        client.create(manager, "diseases", """{"name":"Blocked"}""")

        val log = client.adminRows("/api/audit-log?limit=50", admin)

        assertEquals(listOf("REFDATA_DEACTIVATE", "REFDATA_CREATE", "REFDATA_CREATE"), log.map { it.str("action") })
        assertEquals(listOf("DISEASE", "COST_TYPE", "DISEASE"), log.map { it.str("entityType") })
        assertEquals(rabiesId, log[0].str("entityId"))
        assertEquals("AUCTION", log[1].str("entityId"))
        assertTrue(log.all { it.str("actorUsername") == "admin" && it.str("siteId") == null })
        assertTrue("true->false" in log[0].str("details")!!)
        assertTrue("Auction fees" in log[1].str("details")!!)
        /* The log is per site, and these have none, so a manager never sees them. */
        assertEquals(0, client.adminRows("/api/audit-log", manager).count { it.str("action")!!.startsWith("REFDATA") })
    }
}
