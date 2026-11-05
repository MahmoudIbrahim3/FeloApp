package com.mahmoud.clipkid.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sent to the backend so the team can review a video: a parent hid it, or the player found it
 * removed or no longer embeddable. Deliberately carries no device, user or child identifiers.
 */
@Serializable
data class VideoReport(
    @SerialName("youtube_id") val youtubeId: String,
    val reason: String = REASON_PARENT_HIDDEN,
) {
    companion object {
        const val REASON_PARENT_HIDDEN = "parent_hidden"

        /** Player error 100: the video was removed or made private. */
        const val REASON_VIDEO_REMOVED = "video_removed"

        /** Player errors 101 / 150: the owner no longer allows embedding. */
        const val REASON_EMBED_DISABLED = "embed_disabled"
    }
}
