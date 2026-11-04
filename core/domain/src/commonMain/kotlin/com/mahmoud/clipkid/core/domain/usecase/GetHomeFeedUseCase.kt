package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.catalog.VisibleCatalog
import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.HiddenVideosRepository
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.WatchHistoryRepository
import com.mahmoud.clipkid.core.domain.time.currentDayFlow
import com.mahmoud.clipkid.core.domain.time.rangeIn
import com.mahmoud.clipkid.core.domain.time.startOfDaysBefore
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.Video
import com.mahmoud.clipkid.core.model.WatchEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant

data class HomeFeed(
    val child: ChildProfile,
    /** Visible collections, for the covers row. */
    val collections: List<Collection>,
    val continueWatching: ContinueWatching?,
    /** "جديد النهارده": a daily rotating pick, stable for the whole local day. */
    val newToday: List<Video>,
    /** One shelf per visible collection that has visible videos. */
    val shelves: List<Shelf>,
)

data class ContinueWatching(val video: Video, val watchedSec: Int) {
    /** 0..1 for the progress bar. */
    val progress: Float
        get() = if (video.durationSec <= 0) 0f else (watchedSec.toFloat() / video.durationSec).coerceIn(0f, 1f)
}

data class Shelf(val collection: Collection, val videos: List<Video>)

class GetHomeFeedUseCase(
    private val catalog: CatalogRepository,
    private val profiles: ProfileRepository,
    private val history: WatchHistoryRepository,
    private val hidden: HiddenVideosRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(childId: String): Flow<HomeFeed> =
        currentDayFlow(clock, timeZone).flatMapLatest { date ->
            val day = date.rangeIn(timeZone)
            val windowStart = startOfDaysBefore(date, HISTORY_WINDOW_DAYS, timeZone)
            combine(
                profiles.observeProfile(childId).filterNotNull(),
                catalog.observeCollections(),
                catalog.observeVideos(),
                hidden.observeHiddenIds(childId),
                history.observeEvents(childId, windowStart, day.end),
            ) { child, collections, videos, hiddenIds, events ->
                val visible = VisibleCatalog.of(child, collections, videos, hiddenIds)
                HomeFeed(
                    child = child,
                    collections = visible.collections,
                    continueWatching = continueWatching(visible, events),
                    newToday = newToday(child.id, date, visible, events, day.start),
                    shelves = visible.collections.mapNotNull { collection ->
                        visible.videosByCollection[collection.id]?.let { Shelf(collection, it) }
                    },
                )
            }
        }

    companion object {
        /** How far back continue-watching and "already seen" look. */
        const val HISTORY_WINDOW_DAYS = 7
        const val MIN_CONTINUE_SEC = 5
        const val NEW_TODAY_SIZE = 6

        /** The latest playback of each video; the most recent unfinished one (≥ 5 s) wins. */
        internal fun continueWatching(visible: VisibleCatalog, events: List<WatchEvent>): ContinueWatching? {
            val byId = visible.videos.associateBy { it.youtubeId }
            return events
                .groupBy { it.youtubeId }
                .values
                .map { perVideo -> perVideo.maxBy { it.startedAt } }
                .filter { !it.completed && it.watchedSec >= MIN_CONTINUE_SEC && it.youtubeId in byId }
                .maxByOrNull { it.startedAt }
                ?.let { ContinueWatching(byId.getValue(it.youtubeId), it.watchedSec) }
        }

        /**
         * Shuffles visible videos with a seed from (child, date), leaving out videos completed
         * before today (in the history window) so the pick stays the same all day.
         * Falls back to all visible videos when everything was already seen.
         */
        internal fun newToday(
            childId: String,
            date: LocalDate,
            visible: VisibleCatalog,
            events: List<WatchEvent>,
            todayStart: Instant,
        ): List<Video> {
            val seenBefore = events
                .filter { it.completed && it.startedAt < todayStart }
                .mapTo(HashSet()) { it.youtubeId }
            val pool = visible.videos.filter { it.youtubeId !in seenBefore }.ifEmpty { visible.videos }
            return pool.shuffled(Random(dailySeed(childId, date))).take(NEW_TODAY_SIZE)
        }

        /** FNV-1a over "childId|date": the same on every platform, unlike String.hashCode guarantees. */
        internal fun dailySeed(childId: String, date: LocalDate): Int {
            var hash = 0x811C9DC5.toInt()
            for (ch in "$childId|$date") {
                hash = hash xor ch.code
                hash *= 0x01000193
            }
            return hash
        }
    }
}
