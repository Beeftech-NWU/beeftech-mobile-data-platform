package com.beeftech.backend.api

import com.beeftech.backend.api.auth.PinHasher
import com.beeftech.backend.api.auth.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VoidRoutesTest {

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    private fun ApplicationTestBuilder.startApp() {
        val file = Files.createTempFile("beeftech-void-test", ".db")
        file.toFile().deleteOnExit()
        System.setProperty("beeftech.db.url", "jdbc:sqlite:$file")
        application { module() }
    }

    private suspend fun HttpClient.login(username: String, pin: String): String {
        val text = post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","pin":"$pin","device_id":"dev-$username"}""")
        }.bodyAsText()
        return Json.parseToJsonElement(text).jsonObject["data"]!!.jsonObject["token"]!!.jsonPrimitive.content
    }

    /* The module loads lazily, so touch it before using repositories. */
    private suspend fun HttpClient.insertOtherSiteWorker() {
        get("/health")
        UserRepository().insertUser("other-worker", "other", PinHasher.hash("40004"), 3, siteId = "other-site")
    }

    private suspend fun HttpClient.syncJson(path: String, token: String, body: String) =
        post(path) {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun HttpClient.syncMortality(token: String, animal: String, guid: String) =
        syncJson(
            "/api/mortalities/sync", token,
            """{"deviceId":"d","records":[{"animalId":"$animal","causeOfDeath":"Bloat","responsibleWorker":"w",
            "timestamp":1,"recordguid":"$guid"}]}"""
        )

    private suspend fun HttpClient.void(token: String, slug: String, id: String, reason: String = "Entered in error"): HttpResponse =
        post("/api/records/$slug/$id/void") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"$reason"}""")
        }

    private suspend fun HttpClient.rows(path: String, token: String): List<JsonObject> =
        Json.parseToJsonElement(get(path) { header("Authorization", "Bearer $token") }.bodyAsText())
            .jsonObject["data"]!!.jsonArray.map { it.jsonObject }

    private suspend fun HttpClient.guids(path: String, token: String) =
        rows(path, token).map { it["recordguid"]!!.jsonPrimitive.content }

    @Test
    fun `a manager voids a record on their site and it disappears from every list`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        client.syncMortality(worker, "A-1", "g-1")
        client.syncMortality(worker, "A-2", "g-2")

        val response = client.void(manager, "mortalities", "g-1")

        assertEquals(HttpStatusCode.OK, response.status)
        listOf(worker, manager, admin).forEach {
            assertEquals(listOf("g-2"), client.guids("/api/mortalities", it))
        }
        assertEquals(emptyList(), client.rows("/api/mortalities/A-1", manager))

        /* Nothing is deleted: the row is kept with who, when and why. */
        val row = transaction(DatabaseFactory.getDatabase()) {
            MortalityTable.selectAll().single { it[MortalityTable.recordguid] == "g-1" }
        }
        assertNotNull(row[MortalityTable.voidedAt])
        assertEquals("Entered in error", row[MortalityTable.voidReason])
        assertNotNull(row[MortalityTable.voidedByUserId])
    }

    @Test
    fun `every record type can be voided and then disappears from its list`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")

        client.syncJson(
            "/api/calf-registrations/sync", worker,
            """{"deviceId":"d","records":[{"tagNumber":"TAG0000001","animalUuid":"${java.util.UUID.randomUUID()}",
            "birthdate":1700000000000,"breed":"Angus","gpsLat":-26.1,"gpsLng":27.9,"captureAt":1700000100000,
            "deviceId":"d","recordguid":"calf-1"}]}"""
        )
        client.syncJson(
            "/api/treatments/sync", worker,
            """{"deviceId":"d","records":[{"animalId":"A-1","disease":"x","treatmentName":"t","batchNumber":"b",
            "volumeUsed":"1","cost":5.0,"timestamp":1,"deviceId":"d","recordguid":"treat-1"}]}"""
        )
        client.syncJson(
            "/api/animal-movements/sync", worker,
            """{"deviceId":"d","records":[{"animalId":"A-1","movementType":"PEN_TO_PEN","responsibleWorker":"w",
            "timestamp":1,"recordguid":"move-1"}]}"""
        )
        client.syncJson(
            "/api/farmers/sync", worker,
            """{"deviceId":"d","records":[{"farmerId":"farmer-1","clientCode":"F001"}]}"""
        )
        client.syncMortality(worker, "A-1", "mort-1")

        listOf(
            Triple("calf-registrations", "calf-1", "/api/calf-registrations"),
            Triple("treatments", "treat-1", "/api/treatments"),
            Triple("animal-movements", "move-1", "/api/animal-movements"),
            Triple("farmers", "farmer-1", "/api/farmers"),
            Triple("mortalities", "mort-1", "/api/mortalities")
        ).forEach { (slug, id, listPath) ->
            assertEquals(HttpStatusCode.OK, client.void(manager, slug, id).status, slug)
            assertEquals(
                false,
                client.get(listPath) { header("Authorization", "Bearer $manager") }.bodyAsText().contains(id),
                "$slug still listed"
            )
        }

        /* Single-record reads treat a voided record as missing too. */
        /* Farmers answer a missing record with their existing "not found" body, not a 404. */
        assertEquals(
            "Farmer not found",
            Json.parseToJsonElement(
                client.get("/api/farmers/farmer-1") { header("Authorization", "Bearer $manager") }.bodyAsText()
            ).jsonObject["message"]!!.jsonPrimitive.content.trimEnd('.')
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/api/calf-registrations/TAG0000001") { header("Authorization", "Bearer $manager") }.status
        )
    }

    @Test
    fun `voided records leave the dashboard counts`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        client.syncJson(
            "/api/treatments/sync", worker,
            """{"deviceId":"d","records":[{"animalId":"A-1","disease":"x","treatmentName":"t","batchNumber":"b",
            "volumeUsed":"1","cost":5.0,"timestamp":1,"deviceId":"d","recordguid":"treat-1"}]}"""
        )

        fun treatments(body: String) =
            Json.parseToJsonElement(body).jsonObject["data"]!!.jsonObject["treatments"]!!.jsonObject

        val before = treatments(
            client.get("/api/dashboard/summary") { header("Authorization", "Bearer $manager") }.bodyAsText()
        )
        assertEquals("1", before["total"]!!.jsonPrimitive.content)

        client.void(manager, "treatments", "treat-1")

        val after = treatments(
            client.get("/api/dashboard/summary") { header("Authorization", "Bearer $manager") }.bodyAsText()
        )
        assertEquals("0", after["total"]!!.jsonPrimitive.content)
        assertEquals(0.0, after["totalCost"]!!.jsonPrimitive.content.toDouble())
    }

    @Test
    fun `a manager cannot void another site's record but an admin can`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()
        val other = client.login("other", "40004")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        client.syncMortality(other, "A-9", "g-other")

        assertEquals(HttpStatusCode.NotFound, client.void(manager, "mortalities", "g-other").status)
        assertEquals(listOf("g-other"), client.guids("/api/mortalities", admin))

        assertEquals(HttpStatusCode.OK, client.void(admin, "mortalities", "g-other").status)
        assertEquals(emptyList(), client.guids("/api/mortalities", admin))
    }

    @Test
    fun `workers and anonymous callers cannot void`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        client.syncMortality(worker, "A-1", "g-1")

        assertEquals(HttpStatusCode.Forbidden, client.void(worker, "mortalities", "g-1").status)
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.post("/api/records/mortalities/g-1/void") {
                contentType(ContentType.Application.Json)
                setBody("""{"reason":"x"}""")
            }.status
        )
        assertEquals(listOf("g-1"), client.guids("/api/mortalities", worker))
    }

    @Test
    fun `a reason is required and a record cannot be voided twice`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        client.syncMortality(worker, "A-1", "g-1")

        assertEquals(HttpStatusCode.BadRequest, client.void(manager, "mortalities", "g-1", reason = "  ").status)
        assertEquals(HttpStatusCode.OK, client.void(manager, "mortalities", "g-1").status)
        assertEquals(HttpStatusCode.Conflict, client.void(manager, "mortalities", "g-1").status)
        assertEquals(HttpStatusCode.NotFound, client.void(manager, "mortalities", "no-such-id").status)
        assertEquals(HttpStatusCode.NotFound, client.void(manager, "no-such-type", "g-1").status)

        /* The rejected attempts wrote no audit rows. */
        val audit = transaction(DatabaseFactory.getDatabase()) { AuditLogTable.selectAll().count() }
        assertEquals(1L, audit)
    }

    @Test
    fun `a retried sync does not bring a voided record back`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        client.syncMortality(worker, "A-1", "g-1")
        client.void(manager, "mortalities", "g-1")

        val retry = client.syncMortality(worker, "A-1", "g-1")

        assertEquals(HttpStatusCode.OK, retry.status)
        assertEquals(emptyList(), client.guids("/api/mortalities", worker))
        val row = transaction(DatabaseFactory.getDatabase()) { MortalityTable.selectAll().single() }
        assertNotNull(row[MortalityTable.voidedAt])
    }

    @Test
    fun `the audit log records who voided what and is scoped by site`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        client.syncMortality(worker, "A-1", "g-mine")
        client.syncMortality(other, "A-2", "g-other")
        client.void(manager, "mortalities", "g-mine", reason = "Wrong animal")
        client.void(admin, "mortalities", "g-other", reason = "Duplicate")

        val managerLog = client.rows("/api/audit-log", manager)
        assertEquals(1, managerLog.size)
        assertEquals("g-mine", managerLog.single()["entityId"]!!.jsonPrimitive.content)
        assertEquals("fmanager", managerLog.single()["actorUsername"]!!.jsonPrimitive.content)
        assertEquals("Wrong animal", managerLog.single()["reason"]!!.jsonPrimitive.content)
        assertEquals("VOID", managerLog.single()["action"]!!.jsonPrimitive.content)

        /* Newest first. */
        assertEquals(
            listOf("g-other", "g-mine"),
            client.rows("/api/audit-log", admin).map { it["entityId"]!!.jsonPrimitive.content }
        )
        assertEquals(1, client.rows("/api/audit-log?limit=1", admin).size)

        assertEquals(
            HttpStatusCode.Forbidden,
            client.get("/api/audit-log") { header("Authorization", "Bearer $worker") }.status
        )
    }

    @Test
    fun `a deactivated manager can no longer void`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        client.syncMortality(worker, "A-1", "g-1")

        val managerId = UserRepository().findByUsername("fmanager")!!.userId
        val deactivated = client.patch("/api/users/$managerId") {
            header("Authorization", "Bearer $admin")
            contentType(ContentType.Application.Json)
            setBody("""{"active":false}""")
        }
        assertEquals(HttpStatusCode.OK, deactivated.status)

        /* Their token is still valid, but the service re-reads the account. */
        assertEquals(HttpStatusCode.Forbidden, client.void(manager, "mortalities", "g-1").status)
        assertEquals(listOf("g-1"), client.guids("/api/mortalities", worker))
    }

    @Test
    fun `the review list includes voided records, is scoped by site and names the submitter`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        client.syncMortality(worker, "A-1", "g-1")
        client.syncMortality(worker, "A-2", "g-2")
        client.syncMortality(other, "A-3", "g-other")
        client.void(manager, "mortalities", "g-1", reason = "Wrong animal")

        fun List<JsonObject>.ids() = map { it["id"]!!.jsonPrimitive.content }

        val managerView = client.rows("/api/records/mortalities", manager)
        assertEquals(setOf("g-1", "g-2"), managerView.ids().toSet())

        val voided = managerView.single { it["id"]!!.jsonPrimitive.content == "g-1" }
        assertEquals("Wrong animal", voided["voidReason"]!!.jsonPrimitive.content)
        assertNotNull(voided["voidedAt"])
        assertEquals("jvdm", voided["submittedByUsername"]!!.jsonPrimitive.content)
        assertEquals("A-1 - Bloat", voided["label"]!!.jsonPrimitive.content)

        assertEquals(
            listOf("g-2"),
            client.rows("/api/records/mortalities?includeVoided=false", manager).ids()
        )
        assertEquals(setOf("g-1", "g-2", "g-other"), client.rows("/api/records/mortalities", admin).ids().toSet())
        assertEquals(1, client.rows("/api/records/mortalities?limit=1", admin).size)
    }

    @Test
    fun `the review list covers every record type and rejects workers and unknown types`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        client.syncJson(
            "/api/farmers/sync", worker,
            """{"deviceId":"d","records":[{"farmerId":"farmer-1","clientCode":"F001","organisationName":"Karoo Beef"}]}"""
        )
        client.syncJson(
            "/api/calf-registrations/sync", worker,
            """{"deviceId":"d","records":[{"tagNumber":"TAG0000001","animalUuid":"${java.util.UUID.randomUUID()}",
            "birthdate":1700000000000,"breed":"Angus","gpsLat":-26.1,"gpsLng":27.9,"captureAt":1700000100000,
            "deviceId":"d","recordguid":"calf-1"}]}"""
        )

        assertEquals("Karoo Beef", client.rows("/api/records/farmers", manager).single()["label"]!!.jsonPrimitive.content)
        assertEquals("TAG0000001", client.rows("/api/records/calf-registrations", manager).single()["label"]!!.jsonPrimitive.content)
        listOf("treatments", "animal-movements", "mortalities").forEach {
            assertEquals(emptyList(), client.rows("/api/records/$it", manager))
        }

        assertEquals(
            HttpStatusCode.Forbidden,
            client.get("/api/records/farmers") { header("Authorization", "Bearer $worker") }.status
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/api/records/nope") { header("Authorization", "Bearer $manager") }.status
        )
    }
}
