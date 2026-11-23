package com.mahmoud.feelo.core.domain.usecase

import com.mahmoud.feelo.core.domain.catalog.VisibleCatalog
import com.mahmoud.feelo.core.domain.repository.CatalogRepository
import com.mahmoud.feelo.core.domain.repository.FavoritesRepository
import com.mahmoud.feelo.core.domain.repository.HiddenVideosRepository
import com.mahmoud.feelo.core.domain.repository.ProfileRepository
import com.mahmoud.feelo.core.model.ChildProfile
import com.mahmoud.feelo.core.model.Collection
import com.mahmoud.feelo.core.model.Video
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
) {
    operator fun invoke(childId: String): Flow<List<FavoriteItem>> = combine(
        profiles.observeProfile(childId).filterNotNull(),
        catalog.observeCollections(),
        catalog.observeVideos(),
        hidden.observeHiddenIds(childId),
        favorites.observeFavorites(childId),
    ) { child, collections, videos, hiddenIds, favs ->
        visibleFavorites(child, collections, videos, hiddenIds, favs.map { it.youtubeId })
    }

    companion object {
        internal fun visibleFavorites(
            child: ChildProfile,
            collections: List<Collection>,
            videos: List<Video>,
            hiddenIds: Set<String>,
            favoriteIds: List<String>,
        ): List<FavoriteItem> {
            val visible = VisibleCatalog.of(child, collections, videos, hiddenIds)
            val byId = visible.videos.associateBy { it.youtubeId }
            val collectionById = visible.collections.associateBy { it.id }
            return favoriteIds.mapNotNull { id ->
                val video = byId[id] ?: return@mapNotNull null
                FavoriteItem(video, collectionById.getValue(video.collectionId))
            }
        }
    }
}
