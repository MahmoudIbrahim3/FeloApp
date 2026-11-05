package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.repository.VideoReporter
import com.mahmoud.clipkid.core.domain.repository.WatchHistoryRepository
import com.mahmoud.clipkid.core.domain.time.startOfDaysBefore
import com.mahmoud.clipkid.core.domain.time.todayRange
import com.mahmoud.clipkid.core.model.VideoReport
import com.mahmoud.clipkid.core.model.WatchSource
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * Where the player should start. Only "Continue watching" and a collection's in-progress video
 * resume; every other entry point (Home, New today, Favorites, Up Next, replay) starts at 0.
 *
 * Resume point = the latest unfinished playback of this video in the same 7-day window as
 * Continue watching, when more than [MIN_REMAINING_SEC] are left.
 */
class GetResumePositionUseCase(
    private val history: WatchHistoryRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    suspend operator fun invoke(childId: String, youtubeId: String, durationSec: Int, source: WatchSource): Int {
        if (source != WatchSource.ContinueWatching && source != WatchSource.Collection) return 0
        val today = clock.todayRange(timeZone)
        val from = startOfDaysBefore(today.date, GetHomeFeedUseCase.HISTORY_WINDOW_DAYS, timeZone)
        val latest = history.getEvents(childId, from, today.end)
            .filter { it.youtubeId == youtubeId }
            .maxByOrNull { it.startedAt }
            ?: return 0
        if (latest.completed) return 0
        val position = latest.progressSec
        return if (position > 0 && durationSec - position > MIN_REMAINING_SEC) position else 0
    }

    companion object {
        const val MIN_REMAINING_SEC = 10
    }
}

/** Why the player could not show a video. */
enum class UnavailableReason { Removed, EmbedDisabled }

/**
 * The player found a video removed (error 100) or no longer embeddable (101 / 150): tell the team
 * as `{youtubeId, reason}` only. Best-effort, like hiding a video.
 */
class ReportUnavailableVideoUseCase(private val reporter: VideoReporter) {
    suspend operator fun invoke(youtubeId: String, reason: UnavailableReason) {
        val code = when (reason) {
            UnavailableReason.Removed -> VideoReport.REASON_VIDEO_REMOVED
            UnavailableReason.EmbedDisabled -> VideoReport.REASON_EMBED_DISABLED
        }
        try {
            reporter.report(VideoReport(youtubeId = youtubeId, reason = code))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort: the child already sees the friendly "not available" state.
        }
    }
}
