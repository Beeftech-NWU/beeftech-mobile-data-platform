package com.beeftech.farmtraceability.data

import com.beeftech.database.util.BatchNaming
import com.beeftech.database.util.ProjectCode
import com.beeftech.database.security.TokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TraceabilityEventUpload(
    val entityType: String,
    val recordGuid: String,
    val animalId: String? = null,
    val capturedAt: Long,
    val payload: String
)

@Serializable
data class TraceabilityEventSyncRequest(
    val records:
        List<TraceabilityEventUpload>,
    /* [FarmCode]-[Project]-[YYYYMMDD]-[HHMMSS]-[DeviceID]; null until this device has the farm code. */
    val batchName: String? = null
)

@Serializable
data class TraceabilityEventSyncResult(
    val recordGuid: String,
    val status: String,
    val serverSyncedAt: Long? = null,
    val message: String? = null
)

@Serializable
data class TraceabilityEventSyncResponse(
    val results:
        List<TraceabilityEventSyncResult>
)

@Serializable
private data class TraceabilityEnvelope<T>(
    val success: Boolean,
    val message: String = "",
    val data: T? = null
)

class TraceabilityOutboxApiClient(
    private val tokenProvider:
        TokenProvider,

    private val baseUrl: String =
        DEFAULT_BASE_URL,

    private val httpClient:
        HttpClient =
        HttpClient(OkHttp) {

            install(
                ContentNegotiation
            ) {

                json(
                    Json {
                        ignoreUnknownKeys =
                            true
                    }
                )
            }
        }
) {

    suspend fun syncEvents(
        records:
            List<TraceabilityEventUpload>
    ): Result<
            TraceabilityEventSyncResponse
            > {

        if (records.isEmpty()) {

            return Result.success(
                TraceabilityEventSyncResponse(
                    results =
                        emptyList()
                )
            )
        }

        return try {

            val token =
                tokenProvider.token()
                    ?: return Result.failure(
                        IllegalStateException(
                            "No authentication token available"
                        )
                    )

            val response =
                httpClient.post(
                    "${baseUrl}api/traceability-events/sync"
                ) {

                    contentType(
                        ContentType.Application.Json
                    )

                    header(
                        "Authorization",
                        "Bearer $token"
                    )

                    setBody(
                        TraceabilityEventSyncRequest(
                            records =
                                records,
                            batchName =
                                BatchNaming.nameFor(ProjectCode.TRACE_EVENT)
                        )
                    )
                }

            if (
                !response.status
                    .isSuccess()
            ) {

                return Result.failure(
                    IllegalStateException(
                        "Traceability sync failed with status " +
                            response.status
                    )
                )
            }

            val envelope:
                    TraceabilityEnvelope<
                        TraceabilityEventSyncResponse
                        > =
                response.body()

            val data =
                envelope.data
                    ?: return Result.failure(
                        IllegalStateException(
                            envelope.message
                                .ifBlank {
                                    "Traceability sync returned no data."
                                }
                        )
                    )

            Result.success(
                data
            )

        } catch (
            exception: Exception
        ) {

            Result.failure(
                exception
            )
        }
    }

    companion object {

        const val DEFAULT_BASE_URL =
            "https://beeftech-backend.onrender.com/"
    }
}
