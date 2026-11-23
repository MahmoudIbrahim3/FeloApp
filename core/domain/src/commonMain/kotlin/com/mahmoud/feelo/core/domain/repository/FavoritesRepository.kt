package com.mahmoud.feelo.core.domain.repository

import com.mahmoud.feelo.core.model.Favorite
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    /** Newest first. */
    fun observeFavorites(childId: String): Flow<List<Favorite>>

    suspend fun isFavorite(childId: String, youtubeId: String): Boolean

    suspend fun add(favorite: Favorite)

    suspend fun remove(childId: String, youtubeId: String)
}
