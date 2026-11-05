package com.mahmoud.clipkid.core.data.repository

import com.mahmoud.clipkid.core.domain.repository.VideoReporter
import com.mahmoud.clipkid.core.domain.repository.IdGenerator
import com.mahmoud.clipkid.core.model.VideoReport
import kotlin.uuid.Uuid

/** Until the Supabase backend exists, hide reports go nowhere (the hide still applies locally). */
internal object NoOpVideoReporter : VideoReporter {
    override suspend fun report(report: VideoReport) = Unit
}

internal object UuidIdGenerator : IdGenerator {
    override fun newId(): String = Uuid.random().toString()
}
