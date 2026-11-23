package com.mahmoud.feelo.core.data.repository

import app.cash.turbine.test
import com.mahmoud.feelo.core.data.prefs.InMemoryPreferencesDataStore
import com.mahmoud.feelo.core.data.secure.InMemorySecureStore
import com.mahmoud.feelo.core.data.secure.PinHasher
import com.mahmoud.feelo.core.domain.parent.PinLockout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class DataStoreSettingsRepositoryTest {

    private val secure = InMemorySecureStore()
    private val settings = DataStoreSettingsRepository(InMemoryPreferencesDataStore(), secure, PinHasher(iterations = 1_000))

    @Test
    fun pinIsStoredHashedInSecureStoreAndVerifies() = runTest {
        assertFalse(settings.hasPin.first())
        assertFalse(settings.verifyPin("1234"))

        settings.setPin("1234")

        assertTrue(settings.hasPin.first())
        assertTrue(settings.verifyPin("1234"))
        assertFalse(settings.verifyPin("4321"))
        val stored = assertNotNull(secure.get(DataStoreSettingsRepository.PIN_HASH_KEY))
        assertFalse("1234" in stored)
    }

    @Test
    fun changingThePinReplacesTheOldOne() = runTest {
        settings.setPin("1234")
        settings.setPin("9876")

        assertFalse(settings.verifyPin("1234"))
        assertTrue(settings.verifyPin("9876"))
    }

    @Test
    fun missingSecureEntryMeansNoPin_soTheParentIsNotLockedOut() = runTest {
        settings.setPin("1234")
        secure.remove(DataStoreSettingsRepository.PIN_HASH_KEY)

        assertFalse(settings.hasPin.first())
    }

    @Test
    fun pinMustBeDigits() = runTest {
        assertFailsWith<IllegalArgumentException> { settings.setPin("12a4") }
        assertFailsWith<IllegalArgumentException> { settings.setPin("") }
    }

    @Test
    fun pinLockoutIsSavedAndCleared() = runTest {
        assertEquals(PinLockout.None, settings.getPinLockout())

        val until = Instant.fromEpochMilliseconds(1_793_800_000_000)
        settings.setPinLockout(PinLockout(failedAttempts = 0, lockedUntil = until))
        assertEquals(PinLockout(0, until), settings.getPinLockout())

        settings.setPinLockout(PinLockout(failedAttempts = 3))
        assertEquals(PinLockout(3, null), settings.getPinLockout())
    }

    @Test
    fun onboardingFlag() = runTest {
        settings.onboardingDone.test {
            assertFalse(awaitItem())
            settings.setOnboardingDone(true)
            assertTrue(awaitItem())
        }
    }

    @Test
    fun activeChildCanBeSetAndCleared() = runTest {
        assertNull(settings.activeChildId.first())
        settings.setActiveChildId("c1")
        assertEquals("c1", settings.activeChildId.first())
        settings.setActiveChildId(null)
        assertNull(settings.activeChildId.first())
    }

    @Test
    fun consentIsOffUntilGivenAndSurvives() = runTest {
        assertFalse(settings.consentGiven.first())
        settings.setConsentGiven(true)
        assertTrue(settings.consentGiven.first())
    }
}
