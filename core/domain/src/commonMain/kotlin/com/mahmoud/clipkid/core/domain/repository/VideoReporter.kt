package com.mahmoud.clipkid.core.domain.repository

import com.mahmoud.clipkid.core.model.VideoReport

/**
 * Tells the backend about a video that needs review (hidden by a parent, removed, or no longer
 * embeddable), with no device, user or child identifiers.
 */
fun interface VideoReporter {
    suspend fun report(report: VideoReport)
}
