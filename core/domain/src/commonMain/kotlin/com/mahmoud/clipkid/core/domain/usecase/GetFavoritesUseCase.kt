package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.catalog.VisibleCatalog
import com.mahmoud.clipkid.core.domain.repository.BillingRepository
import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.FavoritesRepository
import com.mahmoud.clipkid.core.domain.repository.HiddenVideosRepository
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.observeIsFull
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

/** A favorite with the collection it belongs to (for its chip). */
data class FavoriteItem(
    val video: Video,
    val collection: Collection,
)

/**
 * The child's favorites, newest first. A favorite only appears while its video is still visible
 * to the child (hidden or no longer allowed videos drop out).
 */
class GetFavoritesUseCase(
    private val catalog: CatalogRepository,
    private val profiles: ProfileRepository,
    private val favorites: FavoritesRepository,
    private val hidden: HiddenVideosRepository,
    private val billing: BillingRepository,
) {
    operator fun invoke(childId: String): Flow<List<FavoriteItem>> = combine(
        combine(profiles.observeProfile(childId).filterNotNull(), billing.observeIsFull(), ::Pair),
        catalog.observeCollections(),
        catalog.observeVideos(),
        hidden.observeHiddenIds(childId),
        favorites.observeFavorites(childId),
    ) { (child, isFull), collections, videos, hiddenIds, favs ->
        visibleFavorites(child, collections, videos, hiddenIds, favs.map { it.youtubeId }, isFull)
    }

    companion object {
        internal fun visibleFavorites(
            child: ChildProfile,
            collections: List<Collection>,
            videos: List<Video>,
            hiddenIds: Set<String>,
            favoriteIds: List<String>,
            isFull: Boolean,
        ): List<FavoriteItem> {
            val visible = VisibleCatalog.of(child, collections, videos, hiddenIds, isFull)
            val byId = visible.videos.associateBy { it.youtubeId }
            val collectionById = visible.collections.associateBy { it.id }
            return favoriteIds.mapNotNull { id ->
                val video = byId[id] ?: return@mapNotNull null
                FavoriteItem(video, collectionById.getValue(video.collectionId))
            }
        }
    }
}
