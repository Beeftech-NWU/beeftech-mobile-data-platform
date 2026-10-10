package com.beeftech.backend.api

import com.beeftech.backend.api.auth.JwtService
import com.beeftech.backend.api.auth.PinHasher
import com.beeftech.backend.api.auth.Role
import com.beeftech.backend.api.auth.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordScopeRoutesTest {

    private fun uniqueTestDbUrl(): String {
        val tempFile = Files.createTempFile("beeftech-backend-test", ".db")
        tempFile.toFile().deleteOnExit()
        return "jdbc:sqlite:${tempFile}"
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 9, 9)

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
        System.setProperty("beeftech.media.dir", java.nio.file.Files.createTempDirectory("beeftech-media-scope-test").toFile().also { it.deleteOnExit() }.absolutePath)
    }

    private suspend fun HttpClient.login(username: String, pin: String): String {
        val response = post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","pin":"$pin","device_id":"dev-$username"}""")
        }
        val text = response.bodyAsText()
        return Json.parseToJsonElement(text)
            .also { require(it.jsonObject["data"] !is kotlinx.serialization.json.JsonNull) { "login $username failed: $text" } }
            .jsonObject["data"]!!.jsonObject["token"]!!.jsonPrimitive.content
    }

    private fun ApplicationTestBuilder.startApp() {
        System.setProperty("beeftech.db.url", uniqueTestDbUrl())
        application {
            module()
            routing {
                get("/test/managers-only") {
                    call.requireRole(JwtService(), Role.ADMIN, Role.MANAGER)
                        ?: return@get
                    call.respondText("ok")
                }
            }
        }
    }

    /* A second worker on a different site, so scoping has something to exclude. */
    /* The module loads lazily, so make a request first or this would hit the previous test's DB. */
    private suspend fun HttpClient.insertOtherSiteWorker() {
        get("/health")
        UserRepository().insertUser(
            userId = "other-worker",
            username = "other",
            pinHash = PinHasher.hash("40004"),
            role = 3,
            siteId = "other-site"
        )
    }

    private fun calfBody(tag: String, guid: String, breed: String = "Angus") =
        """
        {"deviceId":"d","records":[{"tagNumber":"$tag","animalUuid":"${java.util.UUID.nameUUIDFromBytes(guid.toByteArray(java.nio.charset.StandardCharsets.UTF_8))}",
        "birthdate":1700000000000,"breed":"$breed","gpsLat":-26.1,"gpsLng":27.9,
        "captureAt":1700000100000,"deviceId":"d","recordguid":"$guid"}]}
        """.trimIndent()

    @Test
    fun `calf lists are scoped to worker then site then admin`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()

        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        val other = client.login("other", "40004")

        client.post("/api/calf-registrations/sync") {
            header("Authorization", "Bearer $worker")
            contentType(ContentType.Application.Json)
            setBody(calfBody("MineTag001", "g-mine"))
        }
        client.post("/api/calf-registrations/sync") {
            header("Authorization", "Bearer $other")
            contentType(ContentType.Application.Json)
            setBody(calfBody("OtherTag01", "g-other"))
        }

        /* Stamped from the principal. */
        val stamped = transaction(DatabaseFactory.getDatabase()) {
            exec("SELECT submitted_by_user_id, site_id FROM calf_registrations WHERE recordguid = 'g-mine'") { rs ->
                rs.next()
                rs.getString(2)
            }
        }
        assertEquals("dev-site-1", stamped)

        suspend fun list(token: String) =
            client.get("/api/calf-registrations") { header("Authorization", "Bearer $token") }.bodyAsText()

        val workerList = list(worker)
        assertTrue("MineTag001" in workerList)
        assertFalse("OtherTag01" in workerList)

        val managerList = list(manager)
        assertTrue("MineTag001" in managerList)
        assertFalse("OtherTag01" in managerList)

        val adminList = list(admin)
        assertTrue("MineTag001" in adminList)
        assertTrue("OtherTag01" in adminList)

        assertEquals(
            HttpStatusCode.OK,
            client.get("/api/calf-registrations/MineTag001") { header("Authorization", "Bearer $manager") }.status
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/api/calf-registrations/OtherTag01") { header("Authorization", "Bearer $worker") }.status
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/api/calf-registrations/OtherTag01") { header("Authorization", "Bearer $manager") }.status
        )
    }

    @Test
    fun `worker and manager see no legacy rows without a site but admin does`() = testApplication {
        startApp()
        val client = createClient { }
        client.get("/health")

        transaction(DatabaseFactory.getDatabase()) {
            exec(
                "INSERT INTO calf_registrations (tag_number, birthdate, breed, gps_lat, gps_lng, capture_at, " +
                    "device_id, recordguid, sync_status) VALUES ('LegacyTag1', 1, 'Angus', 0, 0, 1, 'd', 'g-legacy', 'SYNCED')"
            )
        }

        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")

        assertFalse(
            "LegacyTag1" in client.get("/api/calf-registrations") {
                header("Authorization", "Bearer $manager")
            }.bodyAsText()
        )
        assertTrue(
            "LegacyTag1" in client.get("/api/calf-registrations") {
                header("Authorization", "Bearer $admin")
            }.bodyAsText()
        )
    }

    @Test
    fun `treatments and farmers are scoped too`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()

        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")
        val manager = client.login("fmanager", "20002")

        client.post("/api/treatments/sync") {
            header("Authorization", "Bearer $worker")
            contentType(ContentType.Application.Json)
            setBody(
                """{"deviceId":"d","records":[{"animalId":"A-MINE","disease":"x","treatmentName":"t",
                "batchNumber":"b","volumeUsed":"1","cost":1.0,"timestamp":1,"deviceId":"d","recordguid":"t-mine"}]}"""
            )
        }
        client.post("/api/farmers/sync") {
            header("Authorization", "Bearer $other")
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"d","records":[{"farmerId":"farmer-other","clientCode":"OTH001"}]}""")
        }

        val treatments = client.get("/api/treatments") { header("Authorization", "Bearer $manager") }.bodyAsText()
        assertTrue("A-MINE" in treatments)

        val otherTreatments = client.get("/api/treatments") { header("Authorization", "Bearer $other") }.bodyAsText()
        assertFalse("A-MINE" in otherTreatments)

        val managerFarmers = client.get("/api/farmers") { header("Authorization", "Bearer $manager") }.bodyAsText()
        assertFalse("farmer-other" in managerFarmers)

        val ownFarmers = client.get("/api/farmers") { header("Authorization", "Bearer $other") }.bodyAsText()
        assertTrue("farmer-other" in ownFarmers)

        val outOfScope = client.get("/api/farmers/farmer-other") { header("Authorization", "Bearer $worker") }.bodyAsText()
        assertTrue("Farmer not found" in outOfScope)
    }

    @Test
    fun `requireRole returns 403 for a worker and allows a manager`() = testApplication {
        startApp()
        val client = createClient { }

        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")

        val forbidden = client.get("/test/managers-only") { header("Authorization", "Bearer $worker") }
        assertEquals(HttpStatusCode.Forbidden, forbidden.status)
        assertTrue("Forbidden" in forbidden.bodyAsText())

        assertEquals(
            HttpStatusCode.OK,
            client.get("/test/managers-only") { header("Authorization", "Bearer $manager") }.status
        )
        assertEquals(HttpStatusCode.Unauthorized, client.get("/test/managers-only").status)
    }

    @Test
    fun `login response carries the site id`() = testApplication {
        startApp()
        val client = createClient { }

        val body = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"fmanager","pin":"20002","device_id":"dev-x"}""")
        }.bodyAsText()

        val user = Json.parseToJsonElement(body).jsonObject["data"]!!.jsonObject["user"]!!.jsonObject
        assertEquals("dev-site-1", user["site_id"]!!.jsonPrimitive.content)
    }

    private suspend fun HttpClient.syncCalf(token: String, body: String): String =
        post("/api/calf-registrations/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()

    private fun calfRow(guid: String): Triple<String?, String?, String?> =
        transaction(DatabaseFactory.getDatabase()) {
            exec("SELECT breed, submitted_by_user_id, site_id FROM calf_registrations WHERE recordguid = '$guid'") { rs ->
                rs.next()
                Triple(rs.getString(1), rs.getString(2), rs.getString(3))
            }!!
        }

    @Test
    fun `a user on another site cannot overwrite a calf and the owner is left unchanged`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")

        client.syncCalf(worker, calfBody("MineTag001", "g-mine", breed = "Angus"))
        val before = calfRow("g-mine")

        val response = client.syncCalf(other, calfBody("MineTag001", "g-mine", breed = "Hijacked"))

        assertTrue("\"status\":\"ERROR\"" in response, response)
        assertFalse("\"status\":\"SYNCED\"" in response, response)
        assertEquals(before, calfRow("g-mine"))
        assertEquals("Angus", before.first)
    }

    @Test
    fun `a worker can re-sync their own calf, and a manager on the same site can update it without taking ownership`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")

        client.syncCalf(worker, calfBody("MineTag001", "g-mine", breed = "Angus"))
        val owner = calfRow("g-mine")

        assertTrue("\"status\":\"SYNCED\"" in client.syncCalf(worker, calfBody("MineTag001", "g-mine", breed = "Brangus")))
        assertEquals("Brangus", calfRow("g-mine").first)

        assertTrue("\"status\":\"SYNCED\"" in client.syncCalf(manager, calfBody("MineTag001", "g-mine", breed = "Bonsmara")))
        val after = calfRow("g-mine")
        assertEquals("Bonsmara", after.first)
        assertEquals(owner.second, after.second)
        assertEquals(owner.third, after.third)
    }

    @Test
    fun `another site gets not found for media and certificate while the owner site and admin succeed`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        val other = client.login("other", "40004")

        client.syncCalf(worker, calfBody("MineTag001", "g-mine"))

        suspend fun media(token: String) =
            client.put("/api/calf-registrations/MineTag001/photo") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Image.JPEG)
                setBody(jpeg)
            }.status

        suspend fun photo(token: String) =
            client.get("/api/calf-registrations/MineTag001/photo") { header("Authorization", "Bearer $token") }.status

        suspend fun certificate(token: String) =
            client.get("/api/calf-registrations/MineTag001/certificate") { header("Authorization", "Bearer $token") }.status

        assertEquals(HttpStatusCode.NotFound, media(other))
        assertEquals(HttpStatusCode.NotFound, certificate(other))
        assertEquals(HttpStatusCode.OK, certificate(worker))
        assertEquals(HttpStatusCode.OK, certificate(manager))
        assertEquals(HttpStatusCode.OK, media(manager))
        assertEquals(HttpStatusCode.OK, media(admin))
        assertEquals(HttpStatusCode.OK, certificate(admin))

        // The photo is now stored: the owner site can read it, another site cannot.
        assertEquals(HttpStatusCode.OK, photo(worker))
        assertEquals(HttpStatusCode.OK, photo(manager))
        assertEquals(HttpStatusCode.NotFound, photo(other))
    }

    @Test
    fun `a legacy calf with no owner is claimed by the first user to sync it`() = testApplication {
        startApp()
        val client = createClient { }
        client.insertOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")

        client.syncCalf(worker, calfBody("MineTag001", "g-legacy"))
        transaction(DatabaseFactory.getDatabase()) {
            exec("UPDATE calf_registrations SET submitted_by_user_id = NULL, site_id = NULL WHERE recordguid = 'g-legacy'")
        }

        assertTrue("\"status\":\"SYNCED\"" in client.syncCalf(other, calfBody("MineTag001", "g-legacy", breed = "Brangus")))

        val claimed = calfRow("g-legacy")
        assertEquals("Brangus", claimed.first)
        assertEquals("other-worker", claimed.second)
        assertEquals("other-site", claimed.third)
    }
}
