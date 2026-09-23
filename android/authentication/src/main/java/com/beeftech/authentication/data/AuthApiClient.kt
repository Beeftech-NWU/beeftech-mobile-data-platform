package com.beeftech.authentication.data

import com.beeftech.authentication.data.dto.LoginRequestDto
import com.beeftech.authentication.data.dto.LoginResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.channels.UnresolvedAddressException

@Serializable
data class AuthApiResponse<T>(
    val success: Boolean,
    val message: String = "",
    val data: T? = null
)

@Serializable
data class LockedData(
    val remainingSeconds: Long = 0
)

sealed class LoginApiResult {
    data class Success(val response: LoginResponseDto) : LoginApiResult()
    data object Unauthorized : LoginApiResult()
    data class Locked(val remainingSeconds: Long) : LoginApiResult()
    data object WrongDevice : LoginApiResult()
    data class NoNetwork(val cause: Throwable? = null) : LoginApiResult()
    data class Error(val message: String) : LoginApiResult()
}

class AuthApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {

    suspend fun login(username: String, pin: String, deviceId: String): LoginApiResult {
        return try {
            val response = httpClient.post("${baseUrl}api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(
                    LoginRequestDto(
                        username = username,
                        pin = pin,
                        deviceId = deviceId
                    )
                )
            }

            val bodyText = response.bodyAsText()

            when (response.status) {
                HttpStatusCode.OK -> {
                    val envelope = Json { ignoreUnknownKeys = true }.decodeFromString<AuthApiResponse<LoginResponseDto>>(bodyText)
                    val loginData = envelope.data
                    if (envelope.success && loginData != null) {
                        LoginApiResult.Success(loginData)
                    } else {
                        LoginApiResult.Error(envelope.message)
                    }
                }
                HttpStatusCode.Unauthorized -> LoginApiResult.Unauthorized
                HttpStatusCode.Conflict -> LoginApiResult.WrongDevice
                HttpStatusCode.Locked -> {
                    val envelope = Json { ignoreUnknownKeys = true }.decodeFromString<AuthApiResponse<LockedData>>(bodyText)
                    val remainingSeconds = envelope.data?.remainingSeconds ?: 300L
                    LoginApiResult.Locked(remainingSeconds)
                }
                else -> LoginApiResult.Error("Server error: ${response.status.value}")
            }
        } catch (e: Exception) {
            if (e is IOException || e is UnresolvedAddressException || e.cause is IOException) {
                LoginApiResult.NoNetwork(e)
            } else {
                LoginApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8081/"
    }
}
