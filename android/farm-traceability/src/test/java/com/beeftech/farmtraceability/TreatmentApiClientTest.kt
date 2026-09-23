package com.beeftech.farmtraceability

import com.beeftech.database.entity.Treatment
import com.beeftech.database.security.TokenProvider
import com.beeftech.farmtraceability.data.TreatmentApiClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class TreatmentApiClientTest {

    private class FakeTokenProvider(private val tokenValue: String?) : TokenProvider {
        override suspend fun token(): String? = tokenValue
    }

    @Test
    fun `syncTreatments returns failure when token is null`() = runTest {
        val apiClient = TreatmentApiClient(
            tokenProvider = FakeTokenProvider(null),
            baseUrl = "http://test-host/"
        )

        val dummyTreatment = Treatment(
            recordguid = "guid-1",
            animalId = "A1",
            disease = "Flu",
            treatmentName = "Meds",
            batchNumber = "B1",
            volumeUsed = "10ml",
            cost = 100.0,
            gpsLat = 0.0,
            gpsLng = 0.0,
            timestamp = 1000L,
            deviceId = "TEST-DEVICE",
            syncStatus = "PENDING",
        syncedAt = null
        )

        val syncResult = apiClient.syncTreatments(listOf(dummyTreatment), deviceId = "TEST-DEVICE")
        assertTrue(syncResult.isFailure)
    }

    @Test
    fun `getReferenceData returns failure when token is null`() = runTest {
        val apiClient = TreatmentApiClient(
            tokenProvider = FakeTokenProvider(null),
            baseUrl = "http://test-host/"
        )

        val result = apiClient.getReferenceData()
        assertTrue(result.isFailure)
    }
}
