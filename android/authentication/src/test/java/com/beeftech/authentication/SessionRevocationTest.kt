package com.beeftech.authentication

import com.beeftech.authentication.data.AuthApiClient
import com.beeftech.authentication.data.AuthRepository
import com.beeftech.authentication.data.DeviceInfo
import com.beeftech.authentication.data.LoginApiResult
import com.beeftech.authentication.data.LoginOutcome
import com.beeftech.authentication.fakes.FakeDeviceIdProvider
import com.beeftech.authentication.fakes.FakeSessionStore
import com.beeftech.authentication.fakes.FakeUserDao
import com.beeftech.database.entity.User
import com.beeftech.database.security.PinLockoutManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mindrot.jbcrypt.BCrypt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.IOException

/* A revoked session or phone: login says so, and the cached PIN stops working offline. */
class SessionRevocationTest {

    private lateinit var sessionStore: FakeSessionStore
    private lateinit var userDao: FakeUserDao
    private lateinit var lockoutManager: PinLockoutManager

    private val pinHash: String = BCrypt.hashpw("30003", BCrypt.gensalt(4))
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Before
    fun setUp() {
        sessionStore = FakeSessionStore()
        userDao = FakeUserDao()
        lockoutManager = mock(PinLockoutManager::class.java)
        `when`(lockoutManager.isLockedOut()).thenReturn(false)
    }

    private fun apiClient(deviceInfo: DeviceInfo = DeviceInfo(model = null), handler: suspend (HttpRequestData) -> Unit = {}, respondWith: () -> Pair<HttpStatusCode, String>): AuthApiClient {
        val engine = MockEngine { request ->
            handler(request)
            val (status, body) = respondWith()
            respond(body, status, jsonHeaders)
        }
        return AuthApiClient(
            "http://localhost/",
            HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
            deviceInfo
        )
    }

    private fun offlineClient(): AuthApiClient {
        val engine = MockEngine { throw IOException("offline") }
        return AuthApiClient(
            "http://localhost/",
            HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        )
    }

    private fun repository(api: AuthApiClient) = AuthRepository(
        apiClient = api,
        sessionStore = sessionStore,
        userDao = userDao,
        lockoutManager = lockoutManager,
        deviceIdProvider = FakeDeviceIdProvider("DEV_123")
    )

    private val okLogin = HttpStatusCode.OK to """{"success":true,"message":"ok","data":{"token":"t","expires_at":"2026-12-31T23:59:59Z",
        "user":{"user_id":"u1","username":"jvdm","role":3,"pin_hash":"$pinHash","device_assigned_id":"DEV_123"}}}"""

    @Test
    fun `the login request carries the phone model and app version, and omits them when unknown`() = runTest {
        val bodies = mutableListOf<String>()

        apiClient(DeviceInfo(model = "Pixel 7", appVersion = "1.4.0"), { bodies += String(it.body.toByteArray()) }) { okLogin }
            .login("jvdm", "30003", "DEV_123")
        apiClient(DeviceInfo(model = null, appVersion = null), { bodies += String(it.body.toByteArray()) }) { okLogin }
            .login("jvdm", "30003", "DEV_123")

        assertTrue(bodies[0], "\"device_model\":\"Pixel 7\"" in bodies[0] && "\"app_version\":\"1.4.0\"" in bodies[0])
        /* An older server never sees fields it doesn't know. */
        assertFalse(bodies[1], "device_model" in bodies[1] || "app_version" in bodies[1])
    }

    @Test
    fun `a 403 from login is a revoked phone and not a network problem`() = runTest {
        val result = apiClient { HttpStatusCode.Forbidden to """{"success":false,"message":"This device has been revoked"}""" }
            .login("jvdm", "30003", "DEV_123")

        assertEquals(LoginApiResult.DeviceRevoked, result)
    }

    @Test
    fun `a revoked phone gets no session and no offline fallback`() = runTest {
        userDao.insertUser(User(userId = "u1", username = "jvdm", pinHash = pinHash))
        val repository = repository(apiClient { HttpStatusCode.Forbidden to """{"success":false,"message":"This device has been revoked"}""" })

        val outcome = repository.login("jvdm", "30003")

        assertEquals(LoginOutcome.DeviceRevoked, outcome)
        assertNull(sessionStore.currentUser())
        assertNull(sessionStore.token())
    }

    @Test
    fun `offline login is refused for a user the server revoked and their cached PIN is dropped`() = runTest {
        userDao.insertUser(User(userId = "u1", username = "jvdm", pinHash = pinHash))
        sessionStore.revokedUser = "u1"

        val outcome = repository(offlineClient()).login("jvdm", "30003")

        assertEquals(LoginOutcome.AccessRevoked, outcome)
        assertNull(sessionStore.currentUser())
        assertNull(userDao.getUserById("u1")!!.pinHash)
        /* Still refused afterwards, now because there is no cached PIN to trust. */
        assertTrue(repository(offlineClient()).login("jvdm", "30003") !is LoginOutcome.Success)
    }

    @Test
    fun `another user can still sign in offline when a different user was revoked`() = runTest {
        userDao.insertUser(User(userId = "u1", username = "jvdm", pinHash = pinHash))
        sessionStore.revokedUser = "someone-else"

        val outcome = repository(offlineClient()).login("jvdm", "30003")

        assertTrue(outcome is LoginOutcome.Success)
        assertNotNull(sessionStore.currentUser())
    }

    @Test
    fun `signing in online clears the revoked flag and gives a fresh PIN hash`() = runTest {
        userDao.insertUser(User(userId = "u1", username = "jvdm", pinHash = null))
        sessionStore.revokedUser = "u1"

        val outcome = repository(apiClient { okLogin }).login("jvdm", "30003")

        assertTrue(outcome is LoginOutcome.Success)
        assertNull(sessionStore.revokedUserId())
        assertEquals(pinHash, userDao.getUserById("u1")!!.pinHash)
        /* And offline login works again afterwards. */
        sessionStore.clear()
        assertTrue(repository(offlineClient()).login("jvdm", "30003") is LoginOutcome.Success)
    }
}
