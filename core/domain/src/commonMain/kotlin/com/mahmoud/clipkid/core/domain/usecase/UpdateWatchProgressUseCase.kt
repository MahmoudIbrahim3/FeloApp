package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.repository.WatchHistoryRepository

/**
 * Saves how far the child got in a playback: [watchedSec] (time actually played, for the minutes
 * quota and activity) and [positionSec] (where playback was, for resume and progress bars).
 */
class UpdateWatchProgressUseCase(private val history: WatchHistoryRepository) {
    suspend operator fun invoke(eventId: String, watchedSec: Int, positionSec: Int, completed: Boolean) {
        history.updateProgress(eventId, watchedSec.coerceAtLeast(0), positionSec.coerceAtLeast(0), completed)
    }
}
