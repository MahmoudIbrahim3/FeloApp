package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.catalog.VisibleCatalog
import com.mahmoud.clipkid.core.domain.repository.BillingRepository
import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.HiddenVideosRepository
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.WatchHistoryRepository
import com.mahmoud.clipkid.core.domain.repository.observeIsFull
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.Video
import com.mahmoud.clipkid.core.model.WatchEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** What the child has done with one video. */
sealed interface VideoStatus {
    data object New : VideoStatus

    /** Started but not finished; [watchedSec] is the furthest point reached (playback position). */
    data class InProgress(val watchedSec: Int) : VideoStatus

    data object Watched : VideoStatus

    companion object {
        /** Derives the status of one video from its own events. Any completed playback means Watched. */
        fun of(events: List<WatchEvent>): VideoStatus = when {
            events.isEmpty() -> New
            events.any { it.completed } -> Watched
            else -> events.maxOf { it.progressSec }.let { if (it > 0) InProgress(it) else New }
        }
    }
}

data class CollectionItem(
    val video: Video,
    val status: VideoStatus,
) {
    /** 0..1 for the progress bar. */
    val progress: Float
        get() = when (status) {
            VideoStatus.New -> 0f
            VideoStatus.Watched -> 1f
            is VideoStatus.InProgress ->
                if (video.durationSec <= 0) 0f else (status.watchedSec.toFloat() / video.durationSec).coerceIn(0f, 1f)
        }
}

data class CollectionPage(
    val collection: Collection,
    /** Visible videos of the collection, in sortOrder. */
    val items: List<CollectionItem>,
) {
    val watchedCount: Int get() = items.count { it.status == VideoStatus.Watched }
}

/**
 * One collection as one child sees it: only visible videos, each with its status.
 * Emits null when the collection is not visible to the child (not allowed, or another age band).
 */
class GetCollectionUseCase(
    private val catalog: CatalogRepository,
    private val profiles: ProfileRepository,
    private val history: WatchHistoryRepository,
    private val hidden: HiddenVideosRepository,
    private val clock: Clock,
    private val billing: BillingRepository,
) {
    operator fun invoke(childId: String, collectionId: String): Flow<CollectionPage?> {
        val until = clock.now() + HISTORY_HORIZON
        return combine(
            combine(profiles.observeProfile(childId).filterNotNull(), billing.observeIsFull(), ::Pair),
            catalog.observeCollections(),
            catalog.observeVideos(),
            hidden.observeHiddenIds(childId),
            history.observeEvents(childId, EPOCH, until),
        ) { (child, isFull), collections, videos, hiddenIds, events ->
            page(child, collections, videos, hiddenIds, events, collectionId, isFull)
        }
    }

    companion object {
        private val EPOCH: Instant = Instant.fromEpochSeconds(0)
        private val HISTORY_HORIZON = 366.days

        internal fun page(
            child: ChildProfile,
            collections: List<Collection>,
            videos: List<Video>,
            hiddenIds: Set<String>,
            events: List<WatchEvent>,
            collectionId: String,
            isFull: Boolean,
        ): CollectionPage? {
            val visible = VisibleCatalog.of(child, collections, videos, hiddenIds, isFull)
            val collection = visible.collections.firstOrNull { it.id == collectionId } ?: return null
            val eventsByVideo = events.groupBy { it.youtubeId }
            val items = visible.videosByCollection[collection.id].orEmpty().map { video ->
                CollectionItem(video, VideoStatus.of(eventsByVideo[video.youtubeId].orEmpty()))
            }
            return CollectionPage(collection, items)
        }
    }
}
