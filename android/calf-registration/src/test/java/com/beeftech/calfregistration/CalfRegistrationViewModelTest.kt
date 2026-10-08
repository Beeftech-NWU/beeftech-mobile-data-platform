package com.beeftech.calfregistration

import android.content.Context
import com.beeftech.calfregistration.data.CalfCaptureContext
import com.beeftech.calfregistration.data.CalfRegistrationApiClient
import com.beeftech.calfregistration.data.CalfRegistrationRepository
import com.beeftech.calfregistration.fakes.FakeCalfRegistrationDao
import com.beeftech.calfregistration.fakes.FakePendingSyncDao
import com.beeftech.calfregistration.fakes.rejectingApiClient
import com.beeftech.calfregistration.fakes.successfulApiClient
import com.beeftech.calfregistration.ui.CalfRegistrationData
import com.beeftech.calfregistration.viewmodel.CalfRegistrationViewModel
import com.beeftech.database.repository.PendingSyncRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class CalfRegistrationViewModelTest {

    private val mockContext: Context = object : android.content.ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(apiClient: CalfRegistrationApiClient): CalfRegistrationViewModel {
        val repository = CalfRegistrationRepository(
            calfRegistrationDao = FakeCalfRegistrationDao(),
            pendingSyncRepository = PendingSyncRepository(FakePendingSyncDao()),
            apiClient = apiClient,
            captureContextProvider = { CalfCaptureContext(deviceId = "TEST-DEVICE") }
        )
        return CalfRegistrationViewModel(repository, mockContext)
    }

    @Test
    fun `saveCalf updates registeredCalves and reports success`() = runBlocking {
        val viewModel = buildViewModel(successfulApiClient())
        val resultDeferred = CompletableDeferred<Pair<Boolean, String>>()

        viewModel.saveCalf(
            CalfRegistrationData(tagNumber = "Blu1234567", animalType = "Bonsmara")
        ) { success, message ->
            resultDeferred.complete(success to message)
        }

        val (success, message) = withTimeout(5_000) { resultDeferred.await() }

        assertEquals(true, success)
        assertTrue(message.isNotBlank())
        assertEquals(1, viewModel.registeredCalves.value.size)
        assertEquals("Blu1234567", viewModel.registeredCalves.value.first().tagNumber)
        assertTrue(viewModel.registeredCalves.value.first().synced)
    }

    @Test
    fun `loadCalves populates registeredCalves from the repository on init`() = runBlocking {
        val calfDao = FakeCalfRegistrationDao()
        val repository = CalfRegistrationRepository(
            calfRegistrationDao = calfDao,
            pendingSyncRepository = PendingSyncRepository(FakePendingSyncDao()),
            apiClient = successfulApiClient(),
            captureContextProvider = { CalfCaptureContext(deviceId = "TEST-DEVICE") }
        )

        repository.saveCalf(CalfRegistrationData(tagNumber = "Blu0000001"))

        val viewModel = CalfRegistrationViewModel(repository, mockContext)

        assertEquals(1, viewModel.registeredCalves.value.size)
        assertEquals("Blu0000001", viewModel.registeredCalves.value.first().tagNumber)
    }

    @Test
    fun `parentOptions refreshes after a female calf is saved`() = runBlocking {
        val viewModel = buildViewModel(successfulApiClient())
        assertTrue(viewModel.parentOptions.value.dams.isEmpty())

        val resultDeferred = CompletableDeferred<Pair<Boolean, String>>()
        viewModel.saveCalf(
            CalfRegistrationData(tagNumber = "Blu0000011", animalType = "BNM — Bonsmara", gender = "Female")
        ) { success, message -> resultDeferred.complete(success to message) }
        withTimeout(5_000) { resultDeferred.await() }

        assertEquals(listOf("Blu0000011 (Bonsmara)"), viewModel.parentOptions.value.dams)
        assertTrue(viewModel.parentOptions.value.sires.isEmpty())
    }

    @Test
    fun `retrySync reports the server reason once a calf is rejected for good`() = runBlocking {
        val viewModel = buildViewModel(rejectingApiClient("Tag already registered"))

        val saved = CompletableDeferred<Pair<Boolean, String>>()
        viewModel.saveCalf(CalfRegistrationData(tagNumber = "Blu1234567")) { ok, msg -> saved.complete(ok to msg) }
        withTimeout(5_000) { saved.await() }

        suspend fun retry(): Pair<Boolean, String> {
            val result = CompletableDeferred<Pair<Boolean, String>>()
            viewModel.retrySync { ok, msg -> result.complete(ok to msg) }
            return withTimeout(5_000) { result.await() }
        }

        val second = retry()
        assertEquals(false, second.first)
        assertTrue(second.second.contains("Tag already registered"))

        val third = retry()
        assertEquals(false, third.first)
        assertTrue(third.second.contains("rejected calf Blu1234567"))
        assertTrue(viewModel.registeredCalves.value.single().needsAttention)
    }

    @Test
    fun `isTagRegistered returns true for existing tag and false for new tag`() = runBlocking {
        val viewModel = buildViewModel(successfulApiClient())

        assertEquals(false, viewModel.isTagRegistered("Blu0000064"))

        val resultDeferred = CompletableDeferred<Pair<Boolean, String>>()
        viewModel.saveCalf(
            CalfRegistrationData(tagNumber = "Blu0000064", animalType = "Brangus")
        ) { success, message ->
            resultDeferred.complete(success to message)
        }
        withTimeout(5_000) { resultDeferred.await() }

        assertEquals(true, viewModel.isTagRegistered("Blu0000064"))
        assertEquals(false, viewModel.isTagRegistered("Red0000123"))
    }

    @Test
    fun `saveCalf reports a duplicate tag as a failure`() = runBlocking {
        val viewModel = buildViewModel(successfulApiClient())

        suspend fun save(): Pair<Boolean, String> {
            val result = CompletableDeferred<Pair<Boolean, String>>()
            viewModel.saveCalf(CalfRegistrationData(tagNumber = "Blu1234567")) { ok, msg ->
                result.complete(ok to msg)
            }
            return withTimeout(5_000) { result.await() }
        }

        assertEquals(true, save().first)

        val (success, message) = save()

        assertEquals(false, success)
        assertTrue(message.contains("Blu1234567"))
        assertEquals(1, viewModel.registeredCalves.value.size)
    }

    @Test
    fun `saveCalf appends the unregistered dam warning to the success message`() = runBlocking {
        val viewModel = buildViewModel(successfulApiClient())
        val result = CompletableDeferred<Pair<Boolean, String>>()

        viewModel.saveCalf(
            CalfRegistrationData(tagNumber = "Blu1234567", dameTagNumber = "Blu0000011 (Bonsmara)")
        ) { ok, msg -> result.complete(ok to msg) }

        val (success, message) = withTimeout(5_000) { result.await() }

        assertEquals(true, success)
        assertTrue(message.contains("Dam Blu0000011 is not registered"))
    }
}
