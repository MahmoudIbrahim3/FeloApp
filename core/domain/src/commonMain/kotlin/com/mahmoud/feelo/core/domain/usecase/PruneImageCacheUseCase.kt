package com.mahmoud.feelo.core.domain.usecase

import com.mahmoud.feelo.core.domain.repository.ImageDiskCache
import com.mahmoud.feelo.core.domain.repository.SettingsRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CancellationException

/**
 * Clears the thumbnail disk cache once it is [MAX_IMAGE_CACHE_AGE] old (compliance A10).
 *
 * YouTube API Services Developer Policies III.E.4: YouTube data, thumbnails included, may not be
 * stored longer than 30 days without a refresh. The catalog rows already follow that
 * (OfflineFirstCatalogRepository); Coil's disk cache has no age limit of its own, so the app wipes
 * it every 7 days, which keeps every thumbnail file well inside 30 days.
 *
 * Runs at app start, before the first screen, so a stale file is never drawn. The cache is
 * passed in because it needs the platform context (Coil), which the DI graph doesn't have.
 */
class PruneImageCacheUseCase(
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    /** @return true when the cache was cleared. A failed clear is not recorded, so the next launch retries. */
    suspend operator fun invoke(cache: ImageDiskCache): Boolean {
        val now = clock.now()
        val last = settings.getImageCacheClearedAt()
        // A saved time in the future means the clock moved back: treat the cache as stale.
        val due = last == null || last > now || now - last >= MAX_IMAGE_CACHE_AGE
        if (!due) return false
        return try {
            cache.clear()
            settings.setImageCacheClearedAt(now)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        val MAX_IMAGE_CACHE_AGE = 7.days
    }
}
