package com.beeftech.authentication

import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BiometricPromptManagerTest {

    private lateinit var mockActivity: FragmentActivity
    private lateinit var mockBiometricManager: BiometricManager

    @Before
    fun setUp() {
        mockActivity = mock(FragmentActivity::class.java)
        mockBiometricManager = mock(BiometricManager::class.java)
    }

    @Test
    fun testBiometricPromptManagerInitialization() {
        val manager = BiometricPromptManager(mockActivity)
        assertNotNull("BiometricPromptManager should initialize successfully", manager)
    }

    @Test
    fun testCanAuthenticateReturnsFalseWhenHardwareUnavailable() {
        `when`(mockBiometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)

        val manager = BiometricPromptManager(mockActivity)
        val canAuth = manager.canAuthenticate()
        assertFalse("Should return false when biometric hardware is not available", canAuth)
    }
}
