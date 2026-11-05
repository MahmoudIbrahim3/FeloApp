package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.repository.FavoritesRepository
import com.mahmoud.clipkid.core.domain.repository.HiddenVideosRepository
import com.mahmoud.clipkid.core.domain.repository.WatchHistoryRepository
import com.mahmoud.clipkid.core.domain.time.today
import com.mahmoud.clipkid.core.model.Favorite
import com.mahmoud.clipkid.core.model.HiddenVideo
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/** Parent (behind the gate) grants extra videos for today only, e.g. +1 or +3. */
class GrantExtraVideosUseCase(
    private val history: WatchHistoryRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    suspend operator fun invoke(childId: String, videos: Int) {
        require(videos > 0) { "videos must be > 0" }
        history.addExtraVideos(childId, clock.today(timeZone), videos)
    }
}

/**
 * Parent hides a video for one child. It is saved locally first (and removed from that child's
 * favorites), then reported to the backend as `{youtubeId, reason}` only. Reporting is
 * best-effort: a failure never undoes the hide.
 */
class HideVideoUseCase(
    private val hidden: HiddenVideosRepository,
    private val favorites: FavoritesRepository,
    private val report: ReportVideoUseCase,
    private val clock: Clock,
) {
    suspend operator fun invoke(childId: String, youtubeId: String) {
        hidden.hide(HiddenVideo(childId, youtubeId, clock.now()))
        favorites.remove(childId, youtubeId)
        // Best-effort: the hide already applies on this device even if the report fails.
        report(youtubeId, VideoReportReason.ParentHidden)
    }
}

/** Adds or removes a favorite. Returns the new state (true = now a favorite). */
class ToggleFavoriteUseCase(
    private val favorites: FavoritesRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(childId: String, youtubeId: String): Boolean =
        if (favorites.isFavorite(childId, youtubeId)) {
            favorites.remove(childId, youtubeId)
            false
        } else {
            favorites.add(Favorite(childId, youtubeId, clock.now()))
            true
        }
}
