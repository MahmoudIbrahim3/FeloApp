package com.mahmoud.feelo.core.domain.repository

import com.mahmoud.feelo.core.model.HiddenVideo
import kotlinx.coroutines.flow.Flow

interface HiddenVideosRepository {
    fun observeHiddenIds(childId: String): Flow<Set<String>>

    suspend fun getHiddenIds(childId: String): Set<String>

    suspend fun hide(video: HiddenVideo)
}
