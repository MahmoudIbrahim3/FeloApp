package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.catalog.VisibleCatalog
import com.mahmoud.clipkid.core.domain.repository.BillingRepository
import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.HiddenVideosRepository
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.WatchHistoryRepository
import com.mahmoud.clipkid.core.domain.repository.isFullNow
import com.mahmoud.clipkid.core.domain.time.todayRange
import com.mahmoud.clipkid.core.model.Video
import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone

/** Up to 2 videos that share [tag] with the finished one ("ولا حاجة عن {tag}؟"). */
data class TagPicks(val tag: String, val videos: List<Video>)

sealed interface UpNextResult {
    /** The limit is used up: show DoneToday instead of Up Next. */
    data object LimitReached : UpNextResult

    data class Choices(
        /** The video that just finished (for the "خلصته!" row and its replay), null if no longer in the catalog. */
        val finished: Video?,
        /** Next video in the same collection. */
        val primary: Video?,
        val tagPicks: TagPicks?,
    ) : UpNextResult {
        /** Every choice in display order. Never more than [MAX_CHOICES]. */
        val all: List<Video> get() = listOfNotNull(primary) + tagPicks?.videos.orEmpty()
    }

    companion object {
        const val MAX_CHOICES = 3
        const val MAX_TAG_PICKS = 2
    }
}

/**
 * Up Next = 1 primary (the next visible video in the same collection by sortOrder, wrapping to the
 * start) + up to 2 sharing the finished video's first tag that has candidates. Videos watched today,
 * hidden or not visible to the child are never offered.
 */
class GetUpNextUseCase(
    private val catalog: CatalogRepository,
    private val profiles: ProfileRepository,
    private val history: WatchHistoryRepository,
    private val hidden: HiddenVideosRepository,
    private val quota: GetDailyQuotaUseCase,
    private val clock: Clock,
    private val timeZone: TimeZone,
    private val billing: BillingRepository,
) {
    suspend operator fun invoke(childId: String, finishedYoutubeId: String): UpNextResult {
        val child = requireNotNull(profiles.getProfile(childId)) { "Unknown child $childId" }
        val today = quota.current(childId)
        if (today == null || today.isReached) return UpNextResult.LimitReached

        val allVideos = catalog.observeVideos().first()
        val visible = VisibleCatalog.of(
            child = child,
            collections = catalog.observeCollections().first(),
            videos = allVideos,
            hiddenIds = hidden.getHiddenIds(childId),
            isFull = billing.isFullNow(),
        )
        val day = clock.todayRange(timeZone)
        val watchedToday = history.getEvents(childId, day.start, day.end).mapTo(HashSet()) { it.youtubeId }
        val finished = allVideos.firstOrNull { it.youtubeId == finishedYoutubeId }

        fun eligible(video: Video) =
            video.youtubeId != finishedYoutubeId && video.youtubeId !in watchedToday

        val primary = finished?.let { done ->
            val sameCollection = visible.videosByCollection[done.collectionId].orEmpty()
            val (after, before) = sameCollection.partition { it.sortOrder > done.sortOrder }
            (after + before).firstOrNull(::eligible)
        }

        val tagPicks = finished?.tags?.firstNotNullOfOrNull { tag ->
            val picks = visible.videos
                .filter { eligible(it) && it.youtubeId != primary?.youtubeId && tag in it.tags }
                .take(UpNextResult.MAX_TAG_PICKS)
            if (picks.isEmpty()) null else TagPicks(tag, picks)
        }

        return UpNextResult.Choices(finished, primary, tagPicks)
    }
}
