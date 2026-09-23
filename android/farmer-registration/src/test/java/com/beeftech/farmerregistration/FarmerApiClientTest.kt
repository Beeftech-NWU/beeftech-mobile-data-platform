package com.beeftech.farmerregistration

import android.content.ContextWrapper
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.security.TokenProvider
import com.beeftech.farmerregistration.data.FarmerApiClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test

class FarmerApiClientTest {

    private class FakeTokenProvider(private val tokenValue: String?) : TokenProvider {
        override suspend fun token(): String? = tokenValue
    }

    private class DummyContext : ContextWrapper(null)

    @Test
    fun `syncFarmer returns null when token is null`() = runTest {
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
        assertNull(syncResult)
    }
}
