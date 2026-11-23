package com.mahmoud.feelo.core.domain.usecase

import com.mahmoud.feelo.core.domain.repository.VideoReporter
import com.mahmoud.feelo.core.domain.repository.WatchHistoryRepository
import com.mahmoud.feelo.core.domain.time.startOfDaysBefore
import com.mahmoud.feelo.core.domain.time.todayRange
import com.mahmoud.feelo.core.model.VideoReport
import com.mahmoud.feelo.core.model.WatchSource
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

/** Why a video is reported to the team for review. */
enum class VideoReportReason {
    /** A parent hid it for their child. */
    ParentHidden,

    /** Player error 100: removed or made private. */
    Removed,

    /** Player errors 101 / 150: the owner no longer allows embedding. */
    EmbedDisabled,
}

/**
 * Tells the team about a video as `{youtubeId, reason}` only: no device, user or child
 * identifiers (backend table `video_reports`). Best-effort: a failure (e.g. offline) is swallowed,
 * the caller's own action (hide, "not available" screen) already applied.
 */
class ReportVideoUseCase(private val reporter: VideoReporter) {
    suspend operator fun invoke(youtubeId: String, reason: VideoReportReason) {
        val code = when (reason) {
            VideoReportReason.ParentHidden -> VideoReport.REASON_PARENT_HIDDEN
            VideoReportReason.Removed -> VideoReport.REASON_VIDEO_REMOVED
            VideoReportReason.EmbedDisabled -> VideoReport.REASON_EMBED_DISABLED
        }
        try {
            reporter.report(VideoReport(youtubeId = youtubeId, reason = code))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort: nothing on the device depends on the report arriving.
        }
    }
}
