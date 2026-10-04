package com.beeftech.backend.api

import com.beeftech.backend.api.auth.PinHasher
import com.beeftech.backend.api.auth.SitesTable
import com.beeftech.backend.api.auth.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Files
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportRoutesTest {

    @BeforeTest
    fun setUp() {
        System.setProperty("beeftech.seed.dev", "true")
    }

    private fun ApplicationTestBuilder.startApp() {
        val file = Files.createTempFile("beeftech-report-test", ".db")
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

    private suspend fun HttpClient.sync(path: String, token: String, record: String) =
        post(path) {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"d","records":[$record]}""")
        }

    private suspend fun HttpClient.syncCalf(token: String, tag: String, captureAt: Long) =
        sync(
            "/api/calf-registrations/sync", token,
            """{"tagNumber":"$tag","animalUuid":"${UUID.randomUUID()}","birthdate":1700000000000,"breed":"Angus",
            "gpsLat":-26.1,"gpsLng":27.9,"captureAt":$captureAt,"deviceId":"d","recordguid":"g-$tag"}"""
        )

    private suspend fun HttpClient.syncTreatment(token: String, guid: String, animal: String, cost: Double, at: Long) =
        sync(
            "/api/treatments/sync", token,
            """{"animalId":"$animal","disease":"x","treatmentName":"Penicillin","batchNumber":"b","volumeUsed":"1",
            "cost":$cost,"timestamp":$at,"deviceId":"d","recordguid":"$guid"}"""
        )

    private suspend fun HttpClient.syncMortality(token: String, guid: String, cause: String, at: Long) =
        sync(
            "/api/mortalities/sync", token,
            """{"animalId":"A-$guid","causeOfDeath":"$cause","responsibleWorker":"w","timestamp":$at,"recordguid":"$guid"}"""
        )

    private suspend fun HttpClient.report(
        slug: String,
        token: String?,
        query: String = ""
    ): HttpResponse =
        get("/api/reports/$slug$query") { token?.let { header("Authorization", "Bearer $it") } }

    private suspend fun HttpResponse.data(): JsonObject =
        Json.parseToJsonElement(bodyAsText()).jsonObject["data"]!!.jsonObject

    private fun JsonObject.figure(label: String): String =
        this["summary"]!!.jsonArray.map { it.jsonObject }
            .single { it["label"]!!.jsonPrimitive.content == label }["value"]!!.jsonPrimitive.content

    private fun JsonObject.rows(): List<List<String>> =
        this["rows"]!!.jsonArray.map { row -> row.jsonArray.map { it.jsonPrimitive.content } }

    @Test
    fun `workers and anonymous callers are refused`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")

        ReportType.entries.forEach {
            assertEquals(HttpStatusCode.Forbidden, client.report(it.path, worker).status)
            assertEquals(HttpStatusCode.Unauthorized, client.report(it.path, null).status)
        }
    }

    @Test
    fun `a manager is limited to their site and cannot ask for another`() = testApplication {
        startApp()
        val client = createClient { }
        client.seedOtherSiteWorker()
        val worker = client.login("jvdm", "30003")
        val other = client.login("other", "40004")
        val manager = client.login("fmanager", "20002")
        val admin = client.login("admin", "10001")
        val now = System.currentTimeMillis()

        client.syncCalf(worker, "Mine00001", now)
        client.syncCalf(other, "Other0001", now)
        client.syncCalf(other, "Other0002", now)

        assertEquals("1", client.report("calf-registrations", manager).data().figure("Calves registered"))
        assertEquals("3", client.report("calf-registrations", admin).data().figure("Calves registered"))
        assertEquals("2", client.report("calf-registrations", admin, "?siteId=other-site").data().figure("Calves registered"))
        assertEquals(
            HttpStatusCode.Forbidden,
            client.report("calf-registrations", manager, "?siteId=other-site").status
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            client.report("calf-registrations", admin, "?siteId=nope").status
        )
    }

    @Test
    fun `bad ranges, buckets and formats are rejected`() = testApplication {
        startApp()
        val client = createClient { }
        val admin = client.login("admin", "10001")

        listOf(
            "?from=abc",
            "?from=200&to=100",
            "?from=0&to=${400L * 24 * 60 * 60 * 1000}",
            "?bucket=hour",
            "?format=xml"
        ).forEach {
            assertEquals(HttpStatusCode.BadRequest, client.report("mortality", admin, it).status, it)
        }
    }

    @Test
    fun `mortality rate divides by calves registered and handles an empty period`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        val now = System.currentTimeMillis()

        assertEquals("n/a", client.report("mortality", manager).data().figure("Mortality rate"))

        (1..4).forEach { client.syncCalf(worker, "Calf000$it", now) }
        client.syncMortality(worker, "m-1", "Bloat", now)
        client.syncMortality(worker, "m-2", "Bloat", now)
        client.syncMortality(worker, "m-old", "Bloat", now - 90L * 24 * 60 * 60 * 1000)

        val data = client.report("mortality", manager).data()
        assertEquals("2", data.figure("Mortalities"))
        assertEquals("4", data.figure("Calves registered"))
        assertEquals("50.0%", data.figure("Mortality rate"))
        assertEquals(listOf(listOf("Bloat", "2")), data.rows())
    }

    @Test
    fun `voided records are left out and a treatment cost is counted once`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        val now = System.currentTimeMillis()

        client.syncTreatment(worker, "t-1", "A-1", 50.0, now)
        client.syncTreatment(worker, "t-2", "A-2", 30.0, now)
        /* The derived cost row that TreatmentDao.insertWithCost writes (future-checks #37). */
        client.sync(
            "/api/costs/sync", worker,
            """{"animalId":"A-1","costType":"TREATMENT","amount":50.0,"timestamp":$now,
            "sourceEntity":"TREATMENT","sourceRecordId":"t-1","recordguid":"c-derived"}"""
        )
        client.sync(
            "/api/costs/sync", worker,
            """{"animalId":"A-1","costType":"TRANSPORT","amount":200.0,"timestamp":$now,"recordguid":"c-1"}"""
        )

        val perAnimal = client.report("cost-per-animal", manager).data()
        assertEquals("280.00", perAnimal.figure("Total cost"))
        assertEquals(listOf(listOf("A-1", "250.00"), listOf("A-2", "30.00")), perAnimal.rows())

        client.post("/api/records/treatments/t-2/void") {
            header("Authorization", "Bearer $manager")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"Entered in error"}""")
        }

        val cost = client.report("treatment-cost", manager).data()
        assertEquals("1", cost.figure("Treatments"))
        assertEquals("50.00", cost.figure("Total cost"))
        assertEquals(listOf(listOf("Penicillin", "1", "50.00")), cost.rows())
        assertEquals(listOf(listOf("A-1", "250.00")), client.report("cost-per-animal", manager).data().rows())
    }

    @Test
    fun `calf registrations are bucketed and worker productivity names the workers`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        val now = System.currentTimeMillis()

        client.syncCalf(worker, "Calf00001", now)
        client.syncCalf(worker, "Calf00002", now)
        client.syncMortality(worker, "m-1", "Bloat", now)

        val byMonth = client.report("calf-registrations", manager, "?bucket=month").data()
        assertEquals(1, byMonth.rows().size)
        assertEquals("2", byMonth.rows().single()[1])

        val productivity = client.report("worker-productivity", manager).data().rows()
        assertEquals(listOf(listOf("jvdm", "2", "0", "1", "0", "3")), productivity)
    }

    @Test
    fun `csv and pdf downloads are attachments, csv is quoted and formulas are neutralised`() = testApplication {
        startApp()
        val client = createClient { }
        val worker = client.login("jvdm", "30003")
        val manager = client.login("fmanager", "20002")
        val now = System.currentTimeMillis()

        client.syncMortality(worker, "m-1", "=HYPERLINK(\\\"x\\\"), bad", now)

        val csv = client.report("mortality", manager, "?format=csv")
        assertEquals(HttpStatusCode.OK, csv.status)
        assertTrue(csv.headers[HttpHeaders.ContentDisposition]!!.startsWith("attachment"))
        assertEquals(
            "Cause of death,Mortalities\r\n\"'=HYPERLINK(\"\"x\"\"), bad\",1\r\n",
            csv.bodyAsText()
        )

        val pdf = client.report("mortality", manager, "?format=pdf")
        assertEquals(HttpStatusCode.OK, pdf.status)
        assertTrue(pdf.headers[HttpHeaders.ContentDisposition]!!.contains(".pdf"))
        assertEquals("%PDF", pdf.bodyAsBytes().copyOfRange(0, 4).toString(Charsets.US_ASCII))
    }

    @Test
    fun `a long report paginates in the pdf`() {
        val rows = (1..120).map { listOf("Animal-$it", "%d.00".format(it)) }
        val bytes = PdfGenerator.generateReport(
            ReportResponse(
                report = "cost-per-animal", title = "Cost per animal report", from = 0, to = 1, generatedAt = 1,
                summary = listOf(ReportFigure("Animals", "120")), columns = listOf("Animal", "Cost"), rows = rows
            )
        )
        org.apache.pdfbox.pdmodel.PDDocument.load(bytes).use { assertTrue(it.numberOfPages > 1) }
    }
}
