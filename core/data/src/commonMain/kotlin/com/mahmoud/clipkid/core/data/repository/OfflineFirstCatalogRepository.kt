package com.mahmoud.clipkid.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.mahmoud.clipkid.core.data.catalog.CatalogRemoteDataSource
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
import kotlin.time.Instant

/** Screens read the Room cache; [refresh] replaces it from the remote source in one transaction. */
internal class OfflineFirstCatalogRepository(
    private val dao: CatalogDao,
    private val remote: CatalogRemoteDataSource,
    private val prefs: DataStore<Preferences>,
) : CatalogRepository {
    override fun observeCollections(): Flow<List<Collection>> =
        dao.observeCollections().map { rows -> rows.map { it.toModel() } }

    override fun observeVideos(): Flow<List<Video>> = dao.observeVideos().map { rows -> rows.map { it.toModel() } }

    override suspend fun lastRefreshedAt(): Instant? =
        prefs.data.first()[PreferenceKeys.CatalogRefreshedAt]?.let(Instant::fromEpochMilliseconds)

    override suspend fun refresh(now: Instant) {
        val catalog = remote.fetchCatalog()
        dao.replaceAll(catalog.collections.map { it.toEntity() }, catalog.videos.map { it.toEntity() })
        prefs.edit { it[PreferenceKeys.CatalogRefreshedAt] = now.toEpochMilliseconds() }
    }
}
