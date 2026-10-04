package com.beeftech.backend.api

import com.beeftech.backend.api.auth.PinHasher
import com.beeftech.backend.api.auth.SitesTable
import com.beeftech.backend.api.auth.UserRepository
import com.beeftech.backend.api.auth.UsersTable
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.nio.file.Files
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashboardRoutesTest {

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    private fun ApplicationTestBuilder.startApp() {
        val file = Files.createTempFile("beeftech-dashboard-test", ".db")
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
    private suspend fun HttpClient.seedOtherSiteWorker() {
        get("/health")
        transaction(DatabaseFactory.getDatabase()) {
            SitesTable.insert {
                it[siteId] = "other-site"
                it[name] = "Other"
                it[createdAt] = 0L
            }
        }
        UserRepository().insertUser(
            "other-worker", "other", PinHasher.hash("40004"), 3, siteId = "other-site"
        )
    }

    private suspend fun HttpClient.syncCalf(token: String, tag: String, captureAt: Long) {
        post("/api/calf-registrations/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                """{"deviceId":"d","records":[{"tagNumber":"$tag","animalUuid":"${UUID.randomUUID()}",
                "birthdate":1700000000000,"breed":"Angus","gpsLat":-26.1,"gpsLng":27.9,
                "captureAt":$captureAt,"deviceId":"d","recordguid":"g-$tag"}]}"""
            )
        }
    }

    private suspend fun HttpClient.syncTreatment(token: String, guid: String, cost: Double) {
        post("/api/treatments/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                """{"deviceId":"d","records":[{"animalId":"A-$guid","disease":"x","treatmentName":"t",
                "batchNumber":"b","volumeUsed":"1","cost":$cost,"timestamp":${System.currentTimeMillis()},
                "deviceId":"d","recordguid":"$guid"}]}"""
            )
        }
    }

    private suspend fun HttpClient.summary(token: String, query: String = ""): Pair<HttpStatusCode, JsonObject?> {
        val response = get("/api/dashboard/summary$query") { header("Authorization", "Bearer $token") }
        val data = if (response.status == HttpStatusCode.OK) {
            Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]!!.jsonObject
        } else null
        return response.status to data
    }

    private fun JsonObject.count(section: String, key: String) =
        this[section]!!.jsonObject[key]!!.jsonPrimitive.content.toDouble()

    @Test
    fun `workers and missing tokens cannot see the dashboard`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        assertEquals(HttpStatusCode.Forbidden, client.summary(worker).first)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/dashboard/summary").status)
    }

    @Test
    fun `manager sees their site and admin sees all sites or one`() = testApplication {
        startApp()
        val client = createClient { }
        client.seedOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        val now = System.currentTimeMillis()
        val tenDaysAgo = now - 10 * 24 * 60 * 60 * 1000L

        client.syncCalf(worker, "Mine00001", now)
        client.syncCalf(worker, "Mine00002", tenDaysAgo)
        client.syncCalf(other, "Other0001", now)
        client.syncTreatment(worker, "t-mine", 12.5)
        client.syncTreatment(other, "t-other", 100.0)

        val managerSummary = client.summary(manager).second!!
        assertEquals(2.0, managerSummary.count("calves", "total"))
        assertEquals(1.0, managerSummary.count("calves", "last7Days"))
        assertEquals(1.0, managerSummary.count("treatments", "total"))
        assertEquals(12.5, managerSummary.count("treatments", "totalCost"))
        assertEquals("dev-site-1", managerSummary["siteId"]!!.jsonPrimitive.content)
        assertEquals(1.0, managerSummary.count("team", "activeWorkers"))

        val adminAll = client.summary(admin).second!!
        assertEquals(3.0, adminAll.count("calves", "total"))
        assertEquals(112.5, adminAll.count("treatments", "totalCost"))
        assertEquals(2.0, adminAll.count("team", "activeWorkers"))

        val adminOther = client.summary(admin, "?siteId=other-site").second!!
        assertEquals(1.0, adminOther.count("calves", "total"))
        assertEquals(100.0, adminOther.count("treatments", "totalCost"))
    }

    @Test
    fun `mortalities, movements, costs and feed are counted per scope and voided records are left out`() = testApplication {
        startApp()
        val client = createClient { }
        client.seedOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        val now = System.currentTimeMillis()
        val tenDaysAgo = now - 10 * 24 * 60 * 60 * 1000L

        suspend fun sync(path: String, token: String, record: String) =
            client.post(path) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody("""{"deviceId":"d","records":[$record]}""")
            }

        sync("/api/mortalities/sync", worker, """{"animalId":"A-1","causeOfDeath":"x","timestamp":$now,"recordguid":"m-1"}""")
        sync("/api/mortalities/sync", worker, """{"animalId":"A-2","causeOfDeath":"x","timestamp":$tenDaysAgo,"recordguid":"m-2"}""")
        sync("/api/mortalities/sync", other, """{"animalId":"A-3","causeOfDeath":"x","timestamp":$now,"recordguid":"m-other"}""")
        sync("/api/animal-movements/sync", worker, """{"animalId":"A-1","movementType":"PEN_TO_PEN","responsibleWorker":"w","timestamp":$now,"recordguid":"mv-1"}""")
        sync("/api/costs/sync", worker, """{"animalId":"A-1","costType":"TRANSPORT","amount":200.0,"timestamp":$now,"recordguid":"c-1"}""")
        sync("/api/costs/sync", worker, """{"animalId":"A-1","costType":"TREATMENT","amount":50.0,"timestamp":$now,"sourceEntity":"TREATMENT","sourceRecordId":"t-1","recordguid":"c-derived"}""")
        sync("/api/costs/sync", other, """{"animalId":"A-3","costType":"FEED","amount":1000.0,"timestamp":$now,"recordguid":"c-other"}""")
        client.post("/api/feed-crib") {
            header("Authorization", "Bearer $worker")
            contentType(ContentType.Application.Json)
            setBody("""{"penName":"P1","adiValue":1.5,"morning":"1","midDay":"2","evening":"3","timestamp":$now}""")
        }

        val managerSummary = client.summary(manager).second!!
        assertEquals(2.0, managerSummary.count("mortalities", "total"))
        assertEquals(1.0, managerSummary.count("mortalities", "last7Days"))
        assertEquals(1.0, managerSummary.count("movements", "total"))
        /* The treatment-derived cost is left out: the treatment already counts it. */
        assertEquals(1.0, managerSummary.count("costs", "total"))
        assertEquals(200.0, managerSummary.count("costs", "totalAmount"))
        assertEquals(1.0, managerSummary.count("feedReadings", "total"))

        val adminAll = client.summary(admin).second!!
        assertEquals(3.0, adminAll.count("mortalities", "total"))
        assertEquals(1200.0, adminAll.count("costs", "totalAmount"))

        val adminOther = client.summary(admin, "?siteId=other-site").second!!
        assertEquals(1.0, adminOther.count("mortalities", "total"))
        assertEquals(0.0, adminOther.count("movements", "total"))
        assertEquals(1000.0, adminOther.count("costs", "totalAmount"))

        client.post("/api/records/mortalities/m-1/void") {
            header("Authorization", "Bearer $manager")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"dup"}""")
        }
        client.post("/api/records/animal-movements/mv-1/void") {
            header("Authorization", "Bearer $manager")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"dup"}""")
        }
        val after = client.summary(manager).second!!
        assertEquals(1.0, after.count("mortalities", "total"))
        assertEquals(0.0, after.count("mortalities", "last7Days"))
        assertEquals(0.0, after.count("movements", "total"))
    }

    @Test
    fun `manager cannot ask for another site and an empty site is all zeros`() = testApplication {
        startApp()
        val client = createClient { }
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")

        assertEquals(HttpStatusCode.Forbidden, client.summary(manager, "?siteId=other-site").first)
        assertEquals(HttpStatusCode.OK, client.summary(manager, "?siteId=dev-site-1").first)

        val empty = client.summary(admin, "?siteId=no-such-site").second!!
        assertEquals(0.0, empty.count("calves", "total"))
        assertEquals(0.0, empty.count("treatments", "totalCost"))
        assertEquals(0.0, empty.count("team", "activeWorkers"))
    }

    @Test
    fun `stale and never-synced workers raise alerts and inactive ones do not`() = testApplication {
        startApp()
        val client = createClient { }
        client.seedOtherSiteWorker()
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        client.login("jvdm", "30003") // just synced, so no alert

        val repo = UserRepository()
        repo.insertUser("stale", "stale", PinHasher.hash("11111"), 3, "dev-phone", "dev-site-1")
        repo.insertUser("gone", "gone", PinHasher.hash("22222"), 3, "dev-phone-2", "dev-site-1", active = false)
        transaction(DatabaseFactory.getDatabase()) {
            UsersTable.update({ UsersTable.userId eq "stale" }) {
                it[deviceLastSync] = System.currentTimeMillis() - 72 * 60 * 60 * 1000L
            }
        }
        /* The other-site worker never claimed a device, so there is nothing to alert on. */
        val managerAlerts = client.summary(manager).second!!["alerts"]!!.jsonArray
        assertEquals(listOf("stale"), managerAlerts.map { it.jsonObject["username"]!!.jsonPrimitive.content })
        assertEquals("STALE_SYNC", managerAlerts.single().jsonObject["type"]!!.jsonPrimitive.content)

        val adminSummary = client.summary(admin).second!!
        assertTrue(adminSummary["alerts"]!!.jsonArray.size == 1)
        assertEquals(1.0, adminSummary.count("team", "inactiveWorkers"))
    }
}
