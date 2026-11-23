package com.mahmoud.feelo.core.domain.usecase

import com.mahmoud.feelo.core.domain.catalog.VisibleCatalog
import com.mahmoud.feelo.core.domain.repository.CatalogRepository
import com.mahmoud.feelo.core.domain.repository.HiddenVideosRepository
import com.mahmoud.feelo.core.domain.repository.ProfileRepository
import com.mahmoud.feelo.core.domain.repository.WatchHistoryRepository
import com.mahmoud.feelo.core.domain.time.HISTORY_START
import com.mahmoud.feelo.core.domain.time.currentDayFlow
import com.mahmoud.feelo.core.domain.time.rangeIn
import com.mahmoud.feelo.core.domain.time.startOfDaysBefore
import com.mahmoud.feelo.core.model.ChildProfile
import com.mahmoud.feelo.core.model.Collection
import com.mahmoud.feelo.core.model.Video
import com.mahmoud.feelo.core.model.WatchEvent
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
    /**
     * One shelf per visible collection, in catalog order: up to [GetHomeFeedUseCase.SHELF_SIZE]
     * videos next in line. Videos already in [continueWatching] or [newToday] are left out, so
     * nothing appears twice on Home; a collection with nothing left has no shelf.
     */
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
                // All history: a shelf needs "finished ever", like the Collection screen.
                history.observeEvents(childId, HISTORY_START, day.end),
            ) { child, collections, videos, hiddenIds, allEvents ->
                val visible = VisibleCatalog.of(child, collections, videos, hiddenIds)
                // Continue watching and "already seen" only look back HISTORY_WINDOW_DAYS.
                val recent = allEvents.filter { it.startedAt >= windowStart }
                val continueWatching = continueWatching(visible, recent)
                val newToday = newToday(child.id, date, visible, recent, day.start)
                val shownIds = buildSet {
                    continueWatching?.let { add(it.video.youtubeId) }
                    newToday.mapTo(this) { it.youtubeId }
                }
                val completedIds = allEvents.filter { it.completed }.mapTo(HashSet()) { it.youtubeId }
                HomeFeed(
                    child = child,
                    collections = visible.collections,
                    continueWatching = continueWatching,
                    newToday = newToday,
                    shelves = visible.collections.mapNotNull { collection ->
                        shelf(collection, visible.videosByCollection[collection.id].orEmpty(), completedIds, shownIds)
                    },
                )
            }
        }

    companion object {
        /** How far back continue-watching and "already seen" look. */
        const val HISTORY_WINDOW_DAYS = 7
        const val MIN_CONTINUE_SEC = 5
        const val NEW_TODAY_SIZE = 3
        const val SHELF_SIZE = 3

        /**
         * The next [SHELF_SIZE] videos of one collection, like episodes of a series: the first
         * ones not finished yet (in collection order), topped up with finished ones from the
         * start once fewer are left, so a fully watched collection starts over. Videos in
         * [shownIds] (already elsewhere on Home) are skipped. Null when nothing is left.
         *
         * @param videos the collection's visible videos, in sortOrder.
         */
        internal fun shelf(
            collection: Collection,
            videos: List<Video>,
            completedIds: Set<String>,
            shownIds: Set<String>,
        ): Shelf? {
            val candidates = videos.filter { it.youtubeId !in shownIds }
            val (finished, notFinished) = candidates.partition { it.youtubeId in completedIds }
            val picks = (notFinished + finished).take(SHELF_SIZE)
            return if (picks.isEmpty()) null else Shelf(collection, picks)
        }

        /** The latest playback of each video; the most recent unfinished one (≥ 5 s) wins. */
        internal fun continueWatching(visible: VisibleCatalog, events: List<WatchEvent>): ContinueWatching? {
            val byId = visible.videos.associateBy { it.youtubeId }
            return events
                .groupBy { it.youtubeId }
                .values
                .map { perVideo -> perVideo.maxBy { it.startedAt } }
                .filter { !it.completed && it.watchedSec >= MIN_CONTINUE_SEC && it.youtubeId in byId }
                .maxByOrNull { it.startedAt }
                ?.let { ContinueWatching(byId.getValue(it.youtubeId), it.progressSec) }
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
