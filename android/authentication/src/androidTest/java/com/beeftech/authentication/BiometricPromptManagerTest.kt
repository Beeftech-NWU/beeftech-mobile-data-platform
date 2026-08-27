package com.beeftech.authentication

import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

class BiometricTestActivity : FragmentActivity()

@RunWith(AndroidJUnit4::class)
class BiometricPromptManagerTest {

    @Test
    fun biometricPromptManager_isCreatedSuccessfully() {

        ActivityScenario.launch(BiometricTestActivity::class.java).use { scenario ->

            scenario.onActivity { activity ->

                val biometricPromptManager =
                    BiometricPromptManager(activity)

                assertNotNull(biometricPromptManager)
            }
        }
    }

    @Test
    fun canAuthenticate_returnsCorrectResult() {

        ActivityScenario.launch(BiometricTestActivity::class.java).use { scenario ->

            scenario.onActivity { activity ->

                val biometricPromptManager =
                    BiometricPromptManager(activity)

                val biometricManager =
                    BiometricManager.from(activity)

                val expected =
                    biometricManager.canAuthenticate() ==
                            BiometricManager.BIOMETRIC_SUCCESS

                val actual =
                    biometricPromptManager.canAuthenticate()

                assertEquals(expected, actual)
            }
        }
    }
}