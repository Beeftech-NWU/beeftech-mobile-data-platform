package com.beeftech.calfregistration.fakes

import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json

private val RECORD_GUID = Regex("\"recordguid\":\"([^\"]+)\"")
private val TAG_NUMBER = Regex("\"tagNumber\":\"([^\"]+)\"")

/** A backend that acknowledges every posted record as SYNCED, echoing its guid and tag. */
fun successfulApiClient(): CalfRegistrationApiClient {
    val engine = MockEngine { request ->
        val body = (request.body as TextContent).text
        val guids = RECORD_GUID.findAll(body).map { it.groupValues[1] }.toList()
        val tags = TAG_NUMBER.findAll(body).map { it.groupValues[1] }.toList()
        val results = guids.zip(tags).joinToString(",") { (guid, tag) ->
            """{"recordguid":"$guid","tagNumber":"$tag","status":"SYNCED","serverSyncedAt":555}"""
        }
        respond(
            content = """{"success":true,"message":"ok","data":{"results":[$results]}}""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    return clientFor(engine)
}

/** A backend that always fails with HTTP 500. */
fun failingApiClient(): CalfRegistrationApiClient =
    clientFor(MockEngine { respondError(HttpStatusCode.InternalServerError) })

private fun clientFor(engine: MockEngine) = CalfRegistrationApiClient(
    tokenProvider = FakeTokenProvider("tok"),
    baseUrl = "http://test-host/",
    httpClient = HttpClient(engine) { install(ContentNegotiation) { json() } }
)
