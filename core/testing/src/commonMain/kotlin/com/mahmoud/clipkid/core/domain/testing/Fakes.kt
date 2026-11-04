package com.mahmoud.clipkid.core.domain.testing

import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.FavoritesRepository
import com.mahmoud.clipkid.core.domain.repository.HiddenVideoReporter
import com.mahmoud.clipkid.core.domain.repository.HiddenVideosRepository
import com.mahmoud.clipkid.core.domain.repository.IdGenerator
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.WatchHistoryRepository
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.Favorite
import com.mahmoud.clipkid.core.model.HiddenVideo
import com.mahmoud.clipkid.core.model.HiddenVideoReport
import com.mahmoud.clipkid.core.model.Video
import com.mahmoud.clipkid.core.model.WatchEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

class FakeCatalogRepository(
    collections: List<Collection> = emptyList(),
    videos: List<Video> = emptyList(),
) : CatalogRepository {
    val collections = MutableStateFlow(collections)
    val videos = MutableStateFlow(videos)
    var lastRefreshed: Instant? = null
    var refreshCalls = 0
    var refreshError: Exception? = null

    override fun observeCollections(): Flow<List<Collection>> = collections
    override fun observeVideos(): Flow<List<Video>> = videos
    override suspend fun lastRefreshedAt(): Instant? = lastRefreshed
    override suspend fun refresh(now: Instant) {
        refreshCalls++
        refreshError?.let { throw it }
        lastRefreshed = now
    }
}

class FakeProfileRepository(vararg profiles: ChildProfile) : ProfileRepository {
    val profiles = MutableStateFlow(profiles.associateBy { it.id })

    override fun observeProfiles(): Flow<List<ChildProfile>> = profiles.map { it.values.toList() }
    override fun observeProfile(id: String): Flow<ChildProfile?> = profiles.map { it[id] }.distinctUntilChanged()
    override suspend fun getProfile(id: String): ChildProfile? = profiles.value[id]
    override suspend fun upsert(profile: ChildProfile) = profiles.update { it + (profile.id to profile) }
    override suspend fun delete(id: String) = profiles.update { it - id }
}

class FakeWatchHistoryRepository(events: List<WatchEvent> = emptyList()) : WatchHistoryRepository {
    val events = MutableStateFlow(events)
    val extras = MutableStateFlow<Map<Pair<String, LocalDate>, Int>>(emptyMap())

    private fun select(all: List<WatchEvent>, childId: String, from: Instant, until: Instant) =
        all.filter { it.childId == childId && it.startedAt >= from && it.startedAt < until }.sortedBy { it.startedAt }

    override fun observeEvents(childId: String, from: Instant, until: Instant): Flow<List<WatchEvent>> =
        events.map { select(it, childId, from, until) }.distinctUntilChanged()

    override suspend fun getEvents(childId: String, from: Instant, until: Instant): List<WatchEvent> =
        select(events.value, childId, from, until)

    override suspend fun record(event: WatchEvent) = events.update { it + event }

    override suspend fun updateProgress(eventId: String, watchedSec: Int, completed: Boolean) =
        events.update { all ->
            all.map { if (it.id == eventId) it.copy(watchedSec = watchedSec, completed = completed) else it }
        }

    override fun observeExtraVideos(childId: String, date: LocalDate): Flow<Int> =
        extras.map { it[childId to date] ?: 0 }.distinctUntilChanged()

    override suspend fun getExtraVideos(childId: String, date: LocalDate): Int = extras.value[childId to date] ?: 0

    override suspend fun addExtraVideos(childId: String, date: LocalDate, videos: Int) =
        extras.update { it + ((childId to date) to ((it[childId to date] ?: 0) + videos)) }
}

class FakeFavoritesRepository : FavoritesRepository {
    val favorites = MutableStateFlow<List<Favorite>>(emptyList())

    override fun observeFavorites(childId: String): Flow<List<Favorite>> =
        favorites.map { all -> all.filter { it.childId == childId }.sortedByDescending { it.addedAt } }

    override suspend fun isFavorite(childId: String, youtubeId: String): Boolean =
        favorites.value.any { it.childId == childId && it.youtubeId == youtubeId }

    override suspend fun add(favorite: Favorite) = favorites.update { all ->
        all.filterNot { it.childId == favorite.childId && it.youtubeId == favorite.youtubeId } + favorite
    }

    override suspend fun remove(childId: String, youtubeId: String) = favorites.update { all ->
        all.filterNot { it.childId == childId && it.youtubeId == youtubeId }
    }
}

class FakeHiddenVideosRepository(vararg hidden: HiddenVideo) : HiddenVideosRepository {
    val hidden = MutableStateFlow(hidden.toList())

    override fun observeHiddenIds(childId: String): Flow<Set<String>> =
        hidden.map { all -> all.filter { it.childId == childId }.mapTo(HashSet()) { it.youtubeId } }.distinctUntilChanged()

    override suspend fun getHiddenIds(childId: String): Set<String> =
        hidden.value.filter { it.childId == childId }.mapTo(HashSet()) { it.youtubeId }

    override suspend fun hide(video: HiddenVideo) = hidden.update { all ->
        if (all.any { it.childId == video.childId && it.youtubeId == video.youtubeId }) all else all + video
    }
}

class FakeHiddenVideoReporter : HiddenVideoReporter {
    val reports = mutableListOf<HiddenVideoReport>()
    var error: Exception? = null

    override suspend fun report(report: HiddenVideoReport) {
        error?.let { throw it }
        reports += report
    }
}

class SequentialIdGenerator(private val prefix: String = "event-") : IdGenerator {
    private var next = 1
    override fun newId(): String = prefix + next++
}
