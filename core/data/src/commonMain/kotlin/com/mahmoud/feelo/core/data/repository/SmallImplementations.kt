package com.mahmoud.feelo.core.data.repository

import com.mahmoud.feelo.core.domain.repository.VideoReporter
import com.mahmoud.feelo.core.domain.repository.IdGenerator
import com.mahmoud.feelo.core.model.VideoReport
import kotlin.uuid.Uuid

/** Until the Supabase backend exists, hide reports go nowhere (the hide still applies locally). */
internal object NoOpVideoReporter : VideoReporter {
    override suspend fun report(report: VideoReport) = Unit
}

internal object UuidIdGenerator : IdGenerator {
    override fun newId(): String = Uuid.random().toString()
}
