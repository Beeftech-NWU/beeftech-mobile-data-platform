package com.beeftech.backend.api

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import java.nio.file.Path

/* Shared by the Phase 4 route tests, so each test file stays about what it checks. */

internal fun newTestDb(): Path =
    Files.createTempFile("beeftech-admin-test", ".db").also { it.toFile().deleteOnExit() }

/* Pass the same file twice to simulate a server restart on the same database. */
internal fun ApplicationTestBuilder.startAdminApp(file: Path = newTestDb()): Path {
    System.setProperty("beeftech.seed.dev", "true")
    System.setProperty("beeftech.db.url", "jdbc:sqlite:$file")
    application { module() }
    return file
}

internal suspend fun HttpClient.adminLoginRaw(
    username: String,
    pin: String,
    device: String = "dev-$username",
    extra: String = ""
): HttpResponse =
    post("/api/auth/login") {
        contentType(ContentType.Application.Json)
        setBody("""{"username":"$username","pin":"$pin","device_id":"$device"$extra}""")
    }

internal suspend fun HttpClient.adminLogin(username: String, pin: String, device: String = "dev-$username"): String {
    val text = adminLoginRaw(username, pin, device).bodyAsText()
    val data = Json.parseToJsonElement(text).jsonObject["data"]
    require(data != null && data !is JsonNull) { "login $username failed: $text" }
    return data.jsonObject["token"]!!.jsonPrimitive.content
}

internal suspend fun HttpClient.adminSend(method: String, path: String, token: String, body: String? = null): HttpResponse {
    val block: HttpRequestBuilder.() -> Unit = {
        header("Authorization", "Bearer $token")
        if (body != null) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }
    return when (method) {
        "GET" -> get(path, block)
        "PATCH" -> patch(path, block)
        "PUT" -> put(path, block)
        else -> post(path, block)
    }
}

internal fun dataOf(text: String): JsonElement = Json.parseToJsonElement(text).jsonObject["data"]!!

internal suspend fun HttpClient.adminRows(path: String, token: String): List<JsonObject> =
    dataOf(adminSend("GET", path, token).bodyAsText()).jsonArray.map { it.jsonObject }

internal fun JsonObject.str(key: String): String? =
    this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content

internal suspend fun HttpClient.adminUserId(token: String, username: String): String =
    dataOf(adminSend("GET", "/api/users", token).bodyAsText()).jsonArray
        .first { it.jsonObject["username"]!!.jsonPrimitive.content == username }
        .jsonObject["user_id"]!!.jsonPrimitive.content
