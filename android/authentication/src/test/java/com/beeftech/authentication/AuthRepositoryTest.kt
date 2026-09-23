package com.beeftech.authentication

import com.beeftech.authentication.data.AuthApiClient
import com.beeftech.authentication.data.AuthRepository
import com.beeftech.authentication.data.LoginOutcome
import com.beeftech.authentication.fakes.FakeDeviceIdProvider
import com.beeftech.authentication.fakes.FakeSessionStore
import com.beeftech.authentication.fakes.FakeUserDao
import com.beeftech.database.entity.User
import com.beeftech.database.security.PinLockoutManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mindrot.jbcrypt.BCrypt
import java.io.IOException

class AuthRepositoryTest {

    private lateinit var sessionStore: FakeSessionStore
    private lateinit var userDao: FakeUserDao
    private lateinit var lockoutManager: PinLockoutManager
    private lateinit var deviceIdProvider: FakeDeviceIdProvider

    @Before
    fun setUp() {
        sessionStore = FakeSessionStore()
        userDao = FakeUserDao()
        lockoutManager = mock(PinLockoutManager::class.java)
        deviceIdProvider = FakeDeviceIdProvider("DEV_123")

        `when`(lockoutManager.isLockedOut()).thenReturn(false)
    }

    private fun createApiClient(engine: MockEngine): AuthApiClient {
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return AuthApiClient("http://localhost/", client)
    }

    @Test
    fun `online success caches the user row and session`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                        "success": true,
                        "message": "Login successful",
                        "data": {
                            "token": "test-jwt-token",
                            "expires_at": "2026-12-31T23:59:59Z",
                            "user": {
                                "user_id": "u1",
                                "username": "jvdm",
                                "role": 3,
                                "pin_hash": "${BCrypt.hashpw("30003", BCrypt.gensalt(10))}",
                                "device_assigned_id": "DEV_123"
                            }
                        }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repository = AuthRepository(
            apiClient = createApiClient(engine),
            sessionStore = sessionStore,
            userDao = userDao,
            lockoutManager = lockoutManager,
            deviceIdProvider = deviceIdProvider
        )

        val outcome = repository.login("jvdm", "30003")

        assertTrue(outcome is LoginOutcome.Success)
        val user = (outcome as LoginOutcome.Success).user
        assertEquals("u1", user.userId)
        assertEquals("jvdm", user.username)

        assertNotNull(sessionStore.currentUser())
        assertEquals("test-jwt-token", sessionStore.token())

        val cachedUser = userDao.getUserByUsername("jvdm")
        assertNotNull(cachedUser)
        assertEquals("u1", cachedUser!!.userId)
        assertNotNull(cachedUser.pinHash)
    }

    @Test
    fun `online 401 maps to BadCredentials`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"success":false, "message":"Incorrect username or PIN"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repository = AuthRepository(
            apiClient = createApiClient(engine),
            sessionStore = sessionStore,
            userDao = userDao,
            lockoutManager = lockoutManager,
            deviceIdProvider = deviceIdProvider
        )

        val outcome = repository.login("jvdm", "wrong_pin")

        assertEquals(LoginOutcome.BadCredentials, outcome)
        verify(lockoutManager).recordFailedAttempt()
    }

    @Test
    fun `online 409 maps to WrongDevice`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"success":false, "message":"This phone is registered to another worker"}""",
                status = HttpStatusCode.Conflict,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repository = AuthRepository(
            apiClient = createApiClient(engine),
            sessionStore = sessionStore,
            userDao = userDao,
            lockoutManager = lockoutManager,
            deviceIdProvider = deviceIdProvider
        )

        val outcome = repository.login("admin", "10001")

        assertEquals(LoginOutcome.WrongDevice, outcome)
        verify(lockoutManager, never()).recordFailedAttempt()
    }

    @Test
    fun `offline login with cached hash succeeds`() = runTest {
        val engine = MockEngine { throw IOException("No network") }

        val pinHash = BCrypt.hashpw("30003", BCrypt.gensalt(10))
        userDao.insertUser(
            User(
                userId = "u1",
                username = "jvdm",
                pinHash = pinHash,
                role = 3,
                deviceAssignedId = "DEV_123"
            )
        )

        val repository = AuthRepository(
            apiClient = createApiClient(engine),
            sessionStore = sessionStore,
            userDao = userDao,
            lockoutManager = lockoutManager,
            deviceIdProvider = deviceIdProvider
        )

        val outcome = repository.login("jvdm", "30003")

        assertTrue(outcome is LoginOutcome.Success)
        val user = (outcome as LoginOutcome.Success).user
        assertEquals("u1", user.userId)
        assertEquals("jvdm", user.username)
        assertNotNull(sessionStore.currentUser())
    }

    @Test
    fun `offline login with wrong PIN fails`() = runTest {
        val engine = MockEngine { throw IOException("No network") }

        val pinHash = BCrypt.hashpw("30003", BCrypt.gensalt(10))
        userDao.insertUser(
            User(
                userId = "u1",
                username = "jvdm",
                pinHash = pinHash,
                role = 3,
                deviceAssignedId = "DEV_123"
            )
        )

        val repository = AuthRepository(
            apiClient = createApiClient(engine),
            sessionStore = sessionStore,
            userDao = userDao,
            lockoutManager = lockoutManager,
            deviceIdProvider = deviceIdProvider
        )

        val outcome = repository.login("jvdm", "wrong_pin")

        assertEquals(LoginOutcome.BadCredentials, outcome)
        verify(lockoutManager).recordFailedAttempt()
    }

    @Test
    fun `offline login with no cached row returns NeedsFirstOnlineLogin`() = runTest {
        val engine = MockEngine { throw IOException("No network") }

        val repository = AuthRepository(
            apiClient = createApiClient(engine),
            sessionStore = sessionStore,
            userDao = userDao,
            lockoutManager = lockoutManager,
            deviceIdProvider = deviceIdProvider
        )

        val outcome = repository.login("unknown_user", "10001")

        assertEquals(LoginOutcome.NeedsFirstOnlineLogin, outcome)
    }

    @Test
    fun `lockout short circuits before any network call`() = runTest {
        `when`(lockoutManager.isLockedOut()).thenReturn(true)
        `when`(lockoutManager.getRemainingLockoutTimeMs()).thenReturn(180_000L)

        var networkCalled = false
        val engine = MockEngine {
            networkCalled = true
            respond("{}", HttpStatusCode.OK)
        }

        val repository = AuthRepository(
            apiClient = createApiClient(engine),
            sessionStore = sessionStore,
            userDao = userDao,
            lockoutManager = lockoutManager,
            deviceIdProvider = deviceIdProvider
        )

        val outcome = repository.login("jvdm", "30003")

        assertTrue(outcome is LoginOutcome.Locked)
        assertEquals(180_000L, (outcome as LoginOutcome.Locked).untilMillis)
        assertEquals(false, networkCalled)
    }
}
