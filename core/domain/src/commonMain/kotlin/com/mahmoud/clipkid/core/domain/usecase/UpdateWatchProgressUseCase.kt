package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.repository.WatchHistoryRepository

/** Saves how far the child got in a playback (minutes quota, continue watching, activity). */
class UpdateWatchProgressUseCase(private val history: WatchHistoryRepository) {
    suspend operator fun invoke(eventId: String, watchedSec: Int, completed: Boolean) {
        history.updateProgress(eventId, watchedSec.coerceAtLeast(0), completed)
    }
}
