package com.mahmoud.clipkid.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sent to the backend when a parent hides a video so the team can review it.
 * Deliberately carries no device, user or child identifiers.
 */
@Serializable
data class HiddenVideoReport(
    @SerialName("youtube_id") val youtubeId: String,
    val reason: String = REASON_PARENT_HIDDEN,
) {
    companion object {
        const val REASON_PARENT_HIDDEN = "parent_hidden"
    }
}
