package com.beeftech.authentication.data

import com.beeftech.authentication.data.dto.ApiResponse
import com.beeftech.authentication.data.dto.LoginRequest
import com.beeftech.authentication.data.dto.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Thin wrapper around a Ktor [HttpClient] that talks to `backend:api`'s
 * `POST /api/auth/login` endpoint. Unlike the calf-registration module's
 * API client, this holds no token cache/login-retry logic of its own -
 * that's [SessionStore]'s responsibility once a login succeeds.
 *
 * Returns [LoginOutcome] rather than a plain Result so callers (namely
 * [AuthRepository]) can distinguish a rejected login (bad PIN, locked
 * account, wrong device) from a connectivity failure - only the latter
 * should trigger an offline-login fallback.
 */
class AuthApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) {

    suspend fun login(
        username: String,
        pin: String,
        deviceId: String
    ): LoginOutcome {
        return try {
            val response = httpClient.post("${baseUrl}api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username = username, pin = pin, deviceId = deviceId))
            }

            when (response.status.value) {
                200, 201 -> {
                    val body: ApiResponse<LoginResponse> = response.body()
                    val data = body.data
                    if (data != null) {
                        LoginOutcome.Success(data)
                    } else {
                        LoginOutcome.UnknownError(body.message)
                    }
                }
                401 -> LoginOutcome.BadCredentials
                423 -> {
                    val message = try {
                        val body: ApiResponse<LoginResponse> = response.body()
                        body.message
                    } catch (exception: Exception) {
                        "Account locked"
                    }
                    LoginOutcome.Locked(message)
                }
                409 -> LoginOutcome.WrongDevice
                else -> LoginOutcome.UnknownError(
                    "Login failed with status ${response.status}"
                )
            }
        } catch (exception: IOException) {
            // UnknownHostException, ConnectException, SocketTimeoutException,
            // etc. are all IOException subclasses - this is the "no network"
            // path. NOTE: this is a heuristic, not a guarantee every
            // connectivity-related Ktor/OkHttp failure is an IOException -
            // worth revisiting if offline fallback isn't triggering when
            // it should.
            LoginOutcome.NetworkError(exception)
        } catch (exception: Exception) {
            LoginOutcome.UnknownError(exception.message ?: "Unknown error")
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8081/"
    }
}

sealed class LoginOutcome {
    data class Success(val response: LoginResponse) : LoginOutcome()
    data object BadCredentials : LoginOutcome()
    data class Locked(val message: String) : LoginOutcome()
    data object WrongDevice : LoginOutcome()
    data class NetworkError(val exception: Exception) : LoginOutcome()
    data class UnknownError(val message: String) : LoginOutcome()
}