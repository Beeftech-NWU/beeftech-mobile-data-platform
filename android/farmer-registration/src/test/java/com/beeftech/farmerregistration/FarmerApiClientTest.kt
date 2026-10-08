package com.beeftech.farmerregistration

import android.content.ContextWrapper
import com.beeftech.database.entity.FarmerAddressEntity
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.security.TokenProvider
import com.beeftech.farmerregistration.data.FarmerApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmerApiClientTest {

    private class FakeTokenProvider(private val tokenValue: String?) : TokenProvider {
        override suspend fun token(): String? = tokenValue
    }

    private class DummyContext : ContextWrapper(null)

    @Test
    fun `syncFarmer fails when token is null`() = runTest {
        val apiClient = FarmerApiClient(
            context = DummyContext(),
            tokenProvider = FakeTokenProvider(null),
            baseUrl = "http://test-host"
        )

        val farmer = FarmerEntity(
            farmer_id = "f-1",
            client_code = "C1",
            organisation_name = "Org",
            vat_number = "123",
            email_address = "a@b.com",
            gps_latitude = 0.0,
            gps_longitude = 0.0,
            sync_status = "PENDING"
        )

        val syncResult = apiClient.syncFarmer(farmer, emptyList(), emptyList())
        assertTrue(syncResult.isFailure)
    }

    @Test
    fun `syncFarmer sends the persisted registration fields in the request json`() = runTest {
        var requestJson: String? = null

        val engine = MockEngine { request ->
            requestJson = String(request.body.toByteArray())
            respond(
                content = """{"success":true,"message":"ok","data":{"results":[{"farmerId":"f-1","status":"SYNCED"}]}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val apiClient = FarmerApiClient(
            context = DummyContext(),
            tokenProvider = FakeTokenProvider("token"),
            client = HttpClient(engine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            },
            baseUrl = "http://test-host",
            deviceIdProvider = { "test-device" }
        )

        val farmer = FarmerEntity(
            farmer_id = "f-1",
            client_code = "C1",
            organisation_name = "Org",
            vat_number = null,
            email_address = null,
            gps_latitude = 0.0,
            gps_longitude = 0.0,
            co_reg_id_no = "9608551/07",
            land_ownership = "Owned",
            fa_code_rmis = "FA-RMIS-01",
            gln_number = "GLN-123",
            herd_capacity = 450,
            interest_status = "Interested"
        )

        val address = FarmerAddressEntity(
            address_id = "a-1",
            farmer_id = "f-1",
            address_type = "PRIMARY",
            address_line_1 = "Mark Str 93",
            province = null,
            postal_code = "8600",
            gps_latitude = 0.0,
            gps_longitude = 0.0,
            street_code = "8600",
            postal_address = "POSBUS 117",
            country = "South Africa"
        )

        val result = apiClient.syncFarmer(farmer, listOf(address), emptyList())

        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
        assertEquals("SYNCED", result.getOrThrow().status)

        val record = Json.parseToJsonElement(requestJson!!)
            .jsonObject["records"]!!.jsonArray[0].jsonObject
        assertEquals("9608551/07", record["coRegIdNo"]!!.jsonPrimitive.content)
        assertEquals("Owned", record["landOwnership"]!!.jsonPrimitive.content)
        assertEquals("FA-RMIS-01", record["faCodeRmis"]!!.jsonPrimitive.content)
        assertEquals("GLN-123", record["glnNumber"]!!.jsonPrimitive.content)
        assertEquals("450", record["herdCapacity"]!!.jsonPrimitive.content)
        assertEquals("Interested", record["interestStatus"]!!.jsonPrimitive.content)

        val addr = record["addresses"]!!.jsonArray[0].jsonObject
        assertEquals("8600", addr["streetCode"]!!.jsonPrimitive.content)
        assertEquals("POSBUS 117", addr["postalAddress"]!!.jsonPrimitive.content)
        assertEquals("South Africa", addr["country"]!!.jsonPrimitive.content)
    }
}
