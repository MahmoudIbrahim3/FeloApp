package com.mahmoud.clipkid.core.data.remote

import com.mahmoud.clipkid.core.domain.repository.VideoReporter
import com.mahmoud.clipkid.core.model.VideoReport
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * Inserts `{youtube_id, reason}` into `video_reports`, nothing else. Anon may insert but not read
 * the table, so nothing is selected back (return=minimal).
 */
internal class SupabaseVideoReporter(private val client: SupabaseClient) : VideoReporter {
    override suspend fun report(report: VideoReport) {
        client.from("video_reports").insert(VideoReportRow(youtubeId = report.youtubeId, reason = report.reason))
    }
}
