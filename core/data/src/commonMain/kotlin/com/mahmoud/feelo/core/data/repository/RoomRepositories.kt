package com.mahmoud.feelo.core.data.repository

import com.mahmoud.feelo.core.data.db.FavoritesDao
import com.mahmoud.feelo.core.data.db.HiddenVideosDao
import com.mahmoud.feelo.core.data.db.ProfileDao
import com.mahmoud.feelo.core.data.db.WatchHistoryDao
import com.mahmoud.feelo.core.data.db.toEntity
import com.mahmoud.feelo.core.data.db.toModel
import com.mahmoud.feelo.core.domain.repository.FavoritesRepository
import com.mahmoud.feelo.core.domain.repository.HiddenVideosRepository
import com.mahmoud.feelo.core.domain.repository.ProfileRepository
import com.mahmoud.feelo.core.domain.repository.WatchHistoryRepository
import com.mahmoud.feelo.core.model.ChildProfile
import com.mahmoud.feelo.core.model.Favorite
import com.mahmoud.feelo.core.model.HiddenVideo
import com.mahmoud.feelo.core.model.WatchEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal class RoomProfileRepository(private val dao: ProfileDao) : ProfileRepository {
    override fun observeProfiles(): Flow<List<ChildProfile>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    override fun observeProfile(id: String): Flow<ChildProfile?> =
        dao.observe(id).map { it?.toModel() }.distinctUntilChanged()

    override suspend fun getProfile(id: String): ChildProfile? = dao.get(id)?.toModel()

    override suspend fun upsert(profile: ChildProfile) = dao.upsert(profile.toEntity())

    override suspend fun delete(id: String) = dao.deleteWithData(id)
}

internal class RoomWatchHistoryRepository(private val dao: WatchHistoryDao) : WatchHistoryRepository {
    override fun observeEvents(childId: String, from: Instant, until: Instant): Flow<List<WatchEvent>> =
        dao.observeRange(childId, from.toEpochMilliseconds(), until.toEpochMilliseconds())
            .map { rows -> rows.map { it.toModel() } }
            .distinctUntilChanged()

    override suspend fun getEvents(childId: String, from: Instant, until: Instant): List<WatchEvent> =
        dao.getRange(childId, from.toEpochMilliseconds(), until.toEpochMilliseconds()).map { it.toModel() }

    override suspend fun record(event: WatchEvent) = dao.insert(event.toEntity())

    override suspend fun updateProgress(eventId: String, watchedSec: Int, positionSec: Int, completed: Boolean) =
        dao.updateProgress(eventId, watchedSec, positionSec, completed)

    override fun observeExtraVideos(childId: String, date: LocalDate): Flow<Int> =
        dao.observeExtra(childId, date.toString()).map { it ?: 0 }.distinctUntilChanged()

    override suspend fun getExtraVideos(childId: String, date: LocalDate): Int = dao.getExtra(childId, date.toString()) ?: 0

    override suspend fun addExtraVideos(childId: String, date: LocalDate, videos: Int) {
        require(videos > 0) { "videos must be > 0" }
        dao.addExtra(childId, date.toString(), videos)
    }
}

internal class RoomFavoritesRepository(private val dao: FavoritesDao) : FavoritesRepository {
    override fun observeFavorites(childId: String): Flow<List<Favorite>> =
        dao.observe(childId).map { rows -> rows.map { it.toModel() } }

    override suspend fun isFavorite(childId: String, youtubeId: String): Boolean = dao.exists(childId, youtubeId)

    override suspend fun add(favorite: Favorite) = dao.upsert(favorite.toEntity())

    override suspend fun remove(childId: String, youtubeId: String) = dao.delete(childId, youtubeId)
}

internal class RoomHiddenVideosRepository(private val dao: HiddenVideosDao) : HiddenVideosRepository {
    override fun observeHiddenIds(childId: String): Flow<Set<String>> =
        dao.observeIds(childId).map { it.toSet() }.distinctUntilChanged()

    override suspend fun getHiddenIds(childId: String): Set<String> = dao.getIds(childId).toSet()

    override suspend fun hide(video: HiddenVideo) = dao.insert(video.toEntity())
}
