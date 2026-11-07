package com.mahmoud.clipkid.core.domain.catalog

import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.Video

/**
 * The part of the catalog one child may see. The single place for the visibility rules:
 * - a collection is visible when the parent allowed it AND it matches the child's age band;
 * - a video is visible when its collection is visible, its own age bands include the child's,
 *   it is embeddable and made for kids, and the parent has not hidden it for this child.
 *
 * Every video is free: the subscription never decides what a child can watch (YouTube Developer
 * Policies III.F.3: no charging to watch embedded YouTube content).
 */
class VisibleCatalog private constructor(
    /** Sorted by `sortOrder`. */
    val collections: List<Collection>,
    /** Sorted by collection order, then video `sortOrder`. */
    val videos: List<Video>,
) {
    val videosByCollection: Map<String, List<Video>> = videos.groupBy { it.collectionId }
    private val ids: Set<String> = videos.mapTo(HashSet()) { it.youtubeId }

    fun isVisible(youtubeId: String): Boolean = youtubeId in ids

    companion object {
        fun of(
            child: ChildProfile,
            collections: List<Collection>,
            videos: List<Video>,
            hiddenIds: Set<String>,
        ): VisibleCatalog {
            val byOrder = compareBy<Collection> { it.sortOrder }.thenBy { it.id }
            val visibleCollections = collections
                .filter { child.ageBand in it.ageBands && it.id in child.allowedCollectionIds }
                .sortedWith(byOrder)
            val order = visibleCollections.withIndex().associate { (index, c) -> c.id to index }
            val visibleVideos = videos
                .filter { video ->
                    video.collectionId in order &&
                        child.ageBand in video.ageBands &&
                        video.embeddable &&
                        video.madeForKids &&
                        video.youtubeId !in hiddenIds
                }
                .sortedWith(
                    compareBy<Video> { order.getValue(it.collectionId) }
                        .thenBy { it.sortOrder }
                        .thenBy { it.youtubeId },
                )
            return VisibleCatalog(visibleCollections, visibleVideos)
        }
    }
}
