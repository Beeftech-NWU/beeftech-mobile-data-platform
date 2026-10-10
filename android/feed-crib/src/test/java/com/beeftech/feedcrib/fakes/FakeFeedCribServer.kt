package com.beeftech.feedcrib.fakes

import com.beeftech.feedcrib.data.FeedCribApiClient
import com.beeftech.feedcrib.data.FeedCribEntrySyncRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** A scriptable backend behind a Ktor [MockEngine]. */
class FakeFeedCribServer {

    /** When true every request fails like a phone with no signal. */
    var offline = false

    /** Answers the sync with this HTTP status instead of 200 (e.g. 500). */
    var syncHttpStatus: HttpStatusCode = HttpStatusCode.OK

    /** Per-record answer for guids listed here: the status and message the server gives. */
    val verdicts = mutableMapOf<String, Pair<String, String?>>()

    /** What the server says for a record with no verdict of its own. */
    var defaultVerdict: Pair<String, String?> = "SYNCED" to null

    /** When set, every request waits here before it is answered, like a slow connection. */
    var gate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

    var downloadJson: String = emptyDownload()

    val syncRequests = mutableListOf<FeedCribEntrySyncRequest>()
    val downloadUrls = mutableListOf<String>()

    private val json = Json { ignoreUnknownKeys = true }

    fun client(): FeedCribApiClient {
        val engine = MockEngine { request ->
            if (offline) throw java.io.IOException("No signal")
            gate?.await()

            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("/api/feed-crib-entries/sync") -> {
                    val body = json.decodeFromString<FeedCribEntrySyncRequest>((request.body as TextContent).text)
                    syncRequests += body

                    val results = body.records.joinToString(",") { record ->
                        val (status, message) = verdicts[record.recordguid] ?: defaultVerdict
                        val messageJson = message?.let { ""","message":"$it"""" }.orEmpty()
                        """{"recordguid":"${record.recordguid}","cribNumber":"${record.cribNumber}","status":"$status","serverSyncedAt":555$messageJson}"""
                    }

                    respond(
                        content = """{"success":true,"message":"ok","data":{"results":[$results]}}""",
                        status = syncHttpStatus,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                request.method == HttpMethod.Get && request.url.encodedPath.endsWith("/api/feed-cribs") -> {
                    downloadUrls += request.url.toString()
                    respond(
                        content = """{"success":true,"message":"ok","data":$downloadJson}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                else -> respond("", HttpStatusCode.NotFound)
            }
        }

        return FeedCribApiClient(
            tokenProvider = FakeTokenProvider("tok"),
            baseUrl = "http://test-host/",
            httpClient = HttpClient(engine) { install(ContentNegotiation) { json() } }
        )
    }

    companion object {
        fun emptyDownload() = """{"siteId":"site-1","cribs":[],"codes":[],"entries":[],"serverTime":1}"""

        /** A site with cribs A01 and A02, the 0-5 codes and one reading some other phone already synced. */
        fun sampleDownload() = """
            {"siteId":"site-1","serverTime":9000,
             "cribs":[
               {"cribNumber":"A01","siteId":"site-1","penDescription":"Pen 1","ration":"Finisher","method":"TMR","description":"Dev crib 1","requiredKg":11.0,"animalsBegin":101,"animalsClose":101,"currentAdi":10.5,"active":true,"updatedAt":8000},
               {"cribNumber":"A02","siteId":"site-1","active":true,"updatedAt":8000}],
             "codes":[
               {"code":0,"label":"Empty"},{"code":1,"label":"Trace"},{"code":2,"label":"Light"},
               {"code":3,"label":"Moderate"},{"code":4,"label":"Heavy"},{"code":5,"label":"Full"}],
             "entries":[
               {"recordguid":"server-1","cribNumber":"A01","readingDate":"2026-10-09","slot":"EVENING","code":2,"adi":10.5,"capturedAt":1000,"deviceId":"other-phone","submittedByUserId":"user-2","syncedAt":2000}]}
        """.trimIndent()
    }
}
