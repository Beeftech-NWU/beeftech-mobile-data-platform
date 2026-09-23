package com.beeftech.farmtraceability

import com.beeftech.database.entity.AnimalMovementEntity
import com.beeftech.database.security.TokenProvider
import com.beeftech.farmtraceability.data.AnimalMovementApiClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalMovementApiClientTest {

    private class FakeTokenProvider(private val tokenValue: String?) : TokenProvider {
        override suspend fun token(): String? = tokenValue
    }

    @Test
    fun `syncMovements returns failure when token is null`() = runTest {
        val apiClient = AnimalMovementApiClient(
            tokenProvider = FakeTokenProvider(null),
            baseUrl = "http://test-host/"
        )

        val movement = AnimalMovementEntity(
            movementId = "m-1",
            animalId = "A1",
            destinationFarmId = "F1",
            destinationPenId = "P1",
            movementDate = "2023-01-01",
            notes = "Worker1"
        )

        val syncResult = apiClient.syncMovements(listOf(movement), deviceId = "TEST-DEVICE")
        assertTrue(syncResult.isFailure)
    }
}
