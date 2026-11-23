package com.mahmoud.feelo.core.domain.usecase

import com.mahmoud.feelo.core.domain.testing.FakeImageDiskCache
import com.mahmoud.feelo.core.domain.testing.FakeSettingsRepository
import com.mahmoud.feelo.core.domain.testing.FixedClock
import com.mahmoud.feelo.core.domain.testing.kuwait
import com.mahmoud.feelo.core.domain.usecase.PruneImageCacheUseCase.Companion.MAX_IMAGE_CACHE_AGE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.runTest

class PruneImageCacheUseCaseTest {
    private val start = kuwait(2026, 11, 22, 10)
    private val clock = FixedClock(start)
    private val settings = FakeSettingsRepository()
    private val cache = FakeImageDiskCache()
    private val prune = PruneImageCacheUseCase(settings, clock)

    @Test
    fun firstRunClearsAndRecordsTheTime() = runTest {
        assertTrue(prune(cache))
        assertEquals(1, cache.clears)
        assertEquals(start, settings.imageCacheClearedAt.value)
    }

    @Test
    fun doesNotClearBeforeSevenDays() = runTest {
        settings.imageCacheClearedAt.value = start
        clock.instant = start + 6.days + 23.hours

        assertFalse(prune(cache))
        assertEquals(0, cache.clears)
        assertEquals(start, settings.imageCacheClearedAt.value)
    }

    @Test
    fun clearsAtExactlySevenDays() = runTest {
        settings.imageCacheClearedAt.value = start
        clock.instant = start + MAX_IMAGE_CACHE_AGE

        assertTrue(prune(cache))
        assertEquals(1, cache.clears)
        assertEquals(clock.instant, settings.imageCacheClearedAt.value)
    }

    @Test
    fun clearsLongAfterSevenDays() = runTest {
        settings.imageCacheClearedAt.value = start
        clock.instant = start + 90.days

        assertTrue(prune(cache))
        assertEquals(1, cache.clears)
    }

    @Test
    fun clearsWhenTheSavedTimeIsInTheFuture() = runTest {
        // The device clock was moved back after the last clear.
        settings.imageCacheClearedAt.value = start + 30.days

        assertTrue(prune(cache))
        assertEquals(1, cache.clears)
        assertEquals(start, settings.imageCacheClearedAt.value)
    }

    @Test
    fun failedClearRecordsNothingAndDoesNotThrow() = runTest {
        cache.error = IllegalStateException("disk")

        assertFalse(prune(cache))
        assertNull(settings.imageCacheClearedAt.value)

        // The next launch retries.
        cache.error = null
        assertTrue(prune(cache))
        assertEquals(1, cache.clears)
    }

    @Test
    fun clearsOncePerWeekAcrossLaunches() = runTest {
        assertTrue(prune(cache))
        clock.instant = start + 3.days
        assertFalse(prune(cache))
        clock.instant = start + 7.days
        assertTrue(prune(cache))
        clock.instant = start + 10.days
        assertFalse(prune(cache))

        assertEquals(2, cache.clears)
    }
}
