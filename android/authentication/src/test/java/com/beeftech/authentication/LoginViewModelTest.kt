package com.beeftech.authentication

import com.beeftech.authentication.data.AuthRepository
import com.beeftech.authentication.data.LoginOutcome
import com.beeftech.authentication.domain.LoggedInUser
import com.beeftech.authentication.viewmodel.LoginUiState
import com.beeftech.authentication.viewmodel.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mock(AuthRepository::class.java)
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState is Idle`() {
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `blank username or pin sets Error state`() {
        viewModel.login("", "12345")
        assertTrue(viewModel.uiState.value is LoginUiState.Error)
        assertEquals(
            "Username and PIN are required",
            (viewModel.uiState.value as LoginUiState.Error).message
        )

        viewModel.login("jvdm", "")
        assertTrue(viewModel.uiState.value is LoginUiState.Error)
    }

    @Test
    fun `successful login updates state to Done`() = runTest {
        val user = LoggedInUser("u1", "jvdm", 3, "DEV_1")
        `when`(authRepository.login("jvdm", "30003")).thenReturn(LoginOutcome.Success(user))

        viewModel.login("jvdm", "30003")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LoginUiState.Done)
        assertEquals(user, (viewModel.uiState.value as LoginUiState.Done).user)
    }

    @Test
    fun `bad credentials maps to user error message`() = runTest {
        `when`(authRepository.login("jvdm", "wrong")).thenReturn(LoginOutcome.BadCredentials)

        viewModel.login("jvdm", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LoginUiState.Error)
        assertEquals(
            "Incorrect username or PIN",
            (viewModel.uiState.value as LoginUiState.Error).message
        )
    }

    @Test
    fun `locked outcome maps to lockout message`() = runTest {
        `when`(authRepository.login("jvdm", "wrong")).thenReturn(LoginOutcome.Locked(300_000L))

        viewModel.login("jvdm", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LoginUiState.Error)
        assertEquals(
            "Too many attempts. Try again in 5 minutes.",
            (viewModel.uiState.value as LoginUiState.Error).message
        )
    }

    @Test
    fun `wrong device outcome maps to registered worker message`() = runTest {
        `when`(authRepository.login("admin", "10001")).thenReturn(LoginOutcome.WrongDevice)

        viewModel.login("admin", "10001")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LoginUiState.Error)
        assertEquals(
            "This phone is registered to another worker",
            (viewModel.uiState.value as LoginUiState.Error).message
        )
    }

    @Test
    fun `needs first online login outcome maps to setup message`() = runTest {
        `when`(authRepository.login("new_user", "10001")).thenReturn(LoginOutcome.NeedsFirstOnlineLogin)

        viewModel.login("new_user", "10001")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LoginUiState.Error)
        assertEquals(
            "Connect once to set up this account",
            (viewModel.uiState.value as LoginUiState.Error).message
        )
    }

    @Test
    fun `clearError resets Error state to Idle`() {
        viewModel.login("", "")
        assertTrue(viewModel.uiState.value is LoginUiState.Error)

        viewModel.clearError()
        assertEquals(LoginUiState.Idle, viewModel.uiState.value)
    }
}
