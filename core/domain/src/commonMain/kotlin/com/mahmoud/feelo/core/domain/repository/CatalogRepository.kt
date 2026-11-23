package com.mahmoud.feelo.core.domain.repository

import com.mahmoud.feelo.core.model.Collection
import com.mahmoud.feelo.core.model.Video
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** The curated catalog, served from the local cache. [refresh] syncs the cache with the backend. */
interface CatalogRepository {
    fun observeCollections(): Flow<List<Collection>>

    fun observeVideos(): Flow<List<Video>>

    /** When the cache was last synced, or null if it never was. */
    suspend fun lastRefreshedAt(): Instant?

    /**
     * Syncs the cache with the backend (only changed rows are downloaded) and drops YouTube data
     * older than 30 days. Throws when offline; the cache keeps working.
     */
    suspend fun refresh(now: Instant)
}
