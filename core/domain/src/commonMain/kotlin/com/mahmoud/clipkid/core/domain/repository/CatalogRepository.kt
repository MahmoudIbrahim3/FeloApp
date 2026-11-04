package com.mahmoud.clipkid.core.domain.repository

import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.Video
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** The curated catalog, served from the local cache. [refresh] replaces the cache from the backend. */
interface CatalogRepository {
    fun observeCollections(): Flow<List<Collection>>

    fun observeVideos(): Flow<List<Video>>

    /** When the cache was last refreshed, or null if it never was. */
    suspend fun lastRefreshedAt(): Instant?

    /** Fetches the catalog from the backend and replaces the cache. Throws when offline. */
    suspend fun refresh(now: Instant)
}
