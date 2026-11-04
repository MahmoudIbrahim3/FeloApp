package com.mahmoud.clipkid.core.domain.repository

import com.mahmoud.clipkid.core.model.HiddenVideoReport

/** Tells the backend a parent hid a video, with no device, user or child identifiers. */
fun interface HiddenVideoReporter {
    suspend fun report(report: HiddenVideoReport)
}
