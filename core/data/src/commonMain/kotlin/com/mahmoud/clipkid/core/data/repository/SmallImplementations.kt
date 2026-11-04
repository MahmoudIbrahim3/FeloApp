package com.mahmoud.clipkid.core.data.repository

import com.mahmoud.clipkid.core.domain.repository.HiddenVideoReporter
import com.mahmoud.clipkid.core.domain.repository.IdGenerator
import com.mahmoud.clipkid.core.model.HiddenVideoReport
import kotlin.uuid.Uuid

/** Until the Supabase backend exists, hide reports go nowhere (the hide still applies locally). */
internal object NoOpHiddenVideoReporter : HiddenVideoReporter {
    override suspend fun report(report: HiddenVideoReport) = Unit
}

internal object UuidIdGenerator : IdGenerator {
    override fun newId(): String = Uuid.random().toString()
}
