package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock

sealed interface CatalogSync {
    data object Synced : CatalogSync

    /** Fetch failed (e.g. offline). The app keeps working from the cache. */
    data class Failed(val cause: Throwable) : CatalogSync
}

/**
 * Syncs the cached catalog with the backend. Runs on every app start: only changed rows are
 * downloaded, so it is cheap. The app start waits for it only when nothing is cached yet.
 */
class SyncCatalogUseCase(
    private val catalog: CatalogRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): CatalogSync = try {
        catalog.refresh(clock.now())
        CatalogSync.Synced
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        CatalogSync.Failed(e)
    }
}
