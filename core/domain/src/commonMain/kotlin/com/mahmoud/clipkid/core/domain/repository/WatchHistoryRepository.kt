package com.mahmoud.clipkid.core.domain.repository

import com.mahmoud.clipkid.core.model.WatchEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** Watch events and the parent's per-day extra-video grants. Everything stays on the device. */
interface WatchHistoryRepository {
    /** Events of [childId] with `from <= startedAt < until`, oldest first. */
    fun observeEvents(childId: String, from: Instant, until: Instant): Flow<List<WatchEvent>>

    suspend fun getEvents(childId: String, from: Instant, until: Instant): List<WatchEvent>

    suspend fun record(event: WatchEvent)

    suspend fun updateProgress(eventId: String, watchedSec: Int, completed: Boolean)

    /** Sum of extra videos granted to [childId] for [date]; 0 when none. */
    fun observeExtraVideos(childId: String, date: LocalDate): Flow<Int>

    suspend fun getExtraVideos(childId: String, date: LocalDate): Int

    /** Adds [videos] to the extras already granted for [date]. */
    suspend fun addExtraVideos(childId: String, date: LocalDate, videos: Int)
}
