package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

sealed interface CatalogRefresh {
    /** The cache is younger than the max age; nothing was fetched. */
    data object Fresh : CatalogRefresh
    data object Refreshed : CatalogRefresh

    /** Fetch failed (e.g. offline). The app keeps working from the cache. */
    data class Failed(val cause: Throwable) : CatalogRefresh
}

/** On app start: refresh the cached catalog when it is older than 24 h (or was never fetched). */
class RefreshCatalogIfStaleUseCase(
    private val catalog: CatalogRepository,
    private val clock: Clock,
    private val maxAge: Duration = 24.hours,
) {
    suspend operator fun invoke(force: Boolean = false): CatalogRefresh {
        val now = clock.now()
        val last = catalog.lastRefreshedAt()
        if (!force && last != null && now - last < maxAge) return CatalogRefresh.Fresh
        return try {
            catalog.refresh(now)
            CatalogRefresh.Refreshed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CatalogRefresh.Failed(e)
        }
    }
}
