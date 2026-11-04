package com.mahmoud.clipkid.core.domain.repository

import com.mahmoud.clipkid.core.model.Favorite
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    /** Newest first. */
    fun observeFavorites(childId: String): Flow<List<Favorite>>

    suspend fun isFavorite(childId: String, youtubeId: String): Boolean

    suspend fun add(favorite: Favorite)

    suspend fun remove(childId: String, youtubeId: String)
}
