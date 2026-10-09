package com.beeftech.calfregistration.fakes

import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
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

private val REQUEST_DEVICE_ID = Regex("\"deviceId\":\"([^\"]+)\",\"records\"")

/**
 * A backend that acknowledges every record as SYNCED and logs each sync request as
 * (request-level deviceId, raw body) in [requests].
 */
fun recordingApiClient(requests: MutableList<Pair<String, String>>): CalfRegistrationApiClient {
    val engine = MockEngine { request ->
        val body = (request.body as TextContent).text
        requests += REQUEST_DEVICE_ID.find(body)!!.groupValues[1] to body
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

/** A photo the fake backend received. */
class ReceivedPhoto(val tagNumber: String, val bytes: ByteArray)

/**
 * A backend that acknowledges every record as SYNCED and answers photo uploads
 * (`PUT .../{tag}/photo`) with [photoStatus], recording each one in [received].
 */
fun photoApiClient(
    received: MutableList<ReceivedPhoto>,
    photoStatus: HttpStatusCode = HttpStatusCode.OK
): CalfRegistrationApiClient {
    val engine = MockEngine { request ->
        if (request.method == HttpMethod.Put) {
            val tag = request.url.encodedPath.substringBeforeLast("/photo").substringAfterLast("/")
            received += ReceivedPhoto(tag, (request.body as OutgoingContent.ByteArrayContent).bytes())
            respond(
                content = """{"success":true,"message":"x"}""",
                status = photoStatus,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        } else {
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
    }
    return clientFor(engine)
}

/** A backend that answers every posted record with status ERROR and [message], as a rule violation would. */
fun rejectingApiClient(message: String = "Tag already registered"): CalfRegistrationApiClient {
    val engine = MockEngine { request ->
        val body = (request.body as TextContent).text
        val guids = RECORD_GUID.findAll(body).map { it.groupValues[1] }.toList()
        val tags = TAG_NUMBER.findAll(body).map { it.groupValues[1] }.toList()
        val results = guids.zip(tags).joinToString(",") { (guid, tag) ->
            """{"recordguid":"$guid","tagNumber":"$tag","status":"ERROR","message":"$message"}"""
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
