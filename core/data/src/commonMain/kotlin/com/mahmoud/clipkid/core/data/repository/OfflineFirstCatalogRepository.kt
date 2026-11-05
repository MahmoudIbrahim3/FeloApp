package com.mahmoud.clipkid.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.mahmoud.clipkid.core.data.catalog.CatalogRemoteDataSource
import com.mahmoud.clipkid.core.data.catalog.CatalogSyncPlan
import com.mahmoud.clipkid.core.data.db.CatalogDao
import com.mahmoud.clipkid.core.data.db.toEntity
import com.mahmoud.clipkid.core.data.db.toModel
import com.mahmoud.clipkid.core.data.prefs.PreferenceKeys
import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Screens read the Room cache; [refresh] syncs it with the backend incrementally:
 * collections whole, videos by comparing each row's server `updated_at` with a manifest
 * (see [CatalogSyncPlan]). The cache never serves YouTube data older than 30 days (rule 5),
 * even when the device stays offline.
 */
internal class OfflineFirstCatalogRepository(
    private val dao: CatalogDao,
    private val remote: CatalogRemoteDataSource,
    private val prefs: DataStore<Preferences>,
    private val clock: Clock,
) : CatalogRepository {
    override fun observeCollections(): Flow<List<Collection>> =
        dao.observeCollections().map { rows -> rows.map { it.toModel() } }

    override fun observeVideos(): Flow<List<Video>> = dao.observeVideos().map { rows ->
        val oldest = (clock.now() - MAX_YOUTUBE_DATA_AGE).toEpochMilliseconds()
        rows.filter { it.refreshedAt >= oldest }.map { it.toModel() }
    }

    override suspend fun lastRefreshedAt(): Instant? =
        prefs.data.first()[PreferenceKeys.CatalogRefreshedAt]?.let(Instant::fromEpochMilliseconds)

    /** Two syncs (e.g. the UI was recreated at launch) never interleave. */
    private val syncLock = Mutex()

    override suspend fun refresh(now: Instant) = syncLock.withLock { sync(now) }

    private suspend fun sync(now: Instant) {
        // First, even if the network fails below: drop YouTube data past 30 days.
        dao.deleteVideosRefreshedBefore((now - MAX_YOUTUBE_DATA_AGE).toEpochMilliseconds())

        val fullResync = prefs.data.first()[PreferenceKeys.CatalogSyncVersion] != CATALOG_SYNC_VERSION
        val collections = remote.fetchCollections()
        val manifest = remote.fetchVideoManifest()
        val cached = if (fullResync) emptyMap() else dao.videoStamps().associate { it.youtubeId to it.serverUpdatedAt }
        val plan = CatalogSyncPlan.of(cached, manifest, fullResync)

        val changed = plan.toFetch
            .chunked(CatalogRemoteDataSource.MAX_IDS_PER_REQUEST)
            .flatMap { remote.fetchVideos(it) }
            .filter { now - it.video.refreshedAt < MAX_YOUTUBE_DATA_AGE }

        dao.applySync(
            collections = collections.map { it.toEntity() },
            changedVideos = changed.map { it.video.toEntity(serverUpdatedAt = it.updatedAt) },
            deletedVideoIds = plan.toDelete,
            fullResync = fullResync,
        )
        prefs.edit {
            it[PreferenceKeys.CatalogRefreshedAt] = now.toEpochMilliseconds()
            it[PreferenceKeys.CatalogSyncVersion] = CATALOG_SYNC_VERSION
        }
    }

    companion object {
        /**
         * Bump when the cached shape or the mapping from the backend changes: the next sync then
         * clears the cache and fetches everything. 1 = Supabase catalog (Phase 5).
         */
        const val CATALOG_SYNC_VERSION = 1

        /** YouTube API Services policy: API data is never kept longer than 30 days. */
        val MAX_YOUTUBE_DATA_AGE = 30.days
    }
}
