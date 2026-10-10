package com.beeftech.feedcrib.data

import com.beeftech.database.BackendConfig
import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.database.security.TokenProvider
import com.beeftech.database.security.reportUnauthorized
import com.beeftech.database.util.BatchNaming
import com.beeftech.database.util.ProjectCode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Thin wrapper around a Ktor [HttpClient] that talks to the `backend:api` feed crib endpoints:
 * the download of cribs, codes and recent entries, and the batch upload of new entries.
 */
class FeedCribApiClient(
    private val tokenProvider: TokenProvider,
    private val baseUrl: String = BackendConfig.baseUrl,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {

    suspend fun syncEntries(
        records: List<FeedCribEntryEntity>,
        deviceId: String
    ): Result<FeedCribEntrySyncResponse> {

        if (records.isEmpty()) {
            return Result.success(FeedCribEntrySyncResponse(results = emptyList()))
        }

        return try {
            val token = tokenProvider.token()
                ?: return Result.failure(IllegalStateException("No authentication token available"))

            val response = httpClient.post("${baseUrl}api/feed-crib-entries/sync") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(
                    FeedCribEntrySyncRequest(
                        deviceId = deviceId,
                        records = records.map { FeedCribMappers.toDto(it) },
                        batchName = BatchNaming.nameFor(ProjectCode.FEED_CRIB)
                    )
                )
            }

            unwrap(response, "Feed crib sync")
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /** The caller's site: cribs, codes and the entries from the last [days] days (the server caps it at 14). */
    suspend fun fetchCribs(days: Int = DEFAULT_DAYS): Result<FeedCribsResponse> {
        return try {
            val token = tokenProvider.token()
                ?: return Result.failure(IllegalStateException("No authentication token available"))

            val response = httpClient.get("${baseUrl}api/feed-cribs") {
                bearerAuth(token)
                parameter("days", days)
            }

            unwrap(response, "Feed crib download")
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private suspend inline fun <reified T> unwrap(response: HttpResponse, what: String): Result<T> {
        /* A 401 means sign in again; the session store decides what that does. Queued records are untouched. */
        if (response.status == HttpStatusCode.Unauthorized) {
            tokenProvider.reportUnauthorized(runCatching { response.bodyAsText() }.getOrNull())
        }

        if (!response.status.isSuccess()) {
            return Result.failure(IllegalStateException("$what failed with status ${response.status}"))
        }

        val body: FeedCribApiResponse<T> = response.body()

        val data = body.data
            ?: return Result.failure(IllegalStateException("$what failed: ${body.message}"))

        return Result.success(data)
    }

    companion object {
        const val DEFAULT_DAYS = 3
    }
}
