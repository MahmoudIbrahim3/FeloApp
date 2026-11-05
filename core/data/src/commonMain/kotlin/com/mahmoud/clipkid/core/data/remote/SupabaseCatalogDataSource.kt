package com.mahmoud.clipkid.core.data.remote

import com.mahmoud.clipkid.core.data.catalog.CatalogRemoteDataSource
import com.mahmoud.clipkid.core.data.catalog.RemoteVideo
import com.mahmoud.clipkid.core.data.catalog.VideoStamp
import com.mahmoud.clipkid.core.model.Collection
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/**
 * Reads the catalog through PostgREST. RLS decides what is visible (published collections;
 * approved, embeddable videos refreshed within 30 days, in published collections), so the
 * queries themselves don't filter.
 */
internal class SupabaseCatalogDataSource(private val client: SupabaseClient) : CatalogRemoteDataSource {

    override suspend fun fetchCollections(): List<Collection> =
        client.from(TABLE_COLLECTIONS)
            .select(Columns.raw(CollectionRow.COLUMNS)) {
                order("sort_order", Order.ASCENDING)
                range(0L, PAGE_SIZE - 1L)
            }
            .decodeList<CollectionRow>()
            .mapNotNull { it.toModel() }

    override suspend fun fetchVideoManifest(): List<VideoStamp> {
        val stamps = mutableListOf<VideoStamp>()
        var from = 0L
        while (true) {
            val page = client.from(TABLE_VIDEOS)
                .select(Columns.raw(VideoStampRow.COLUMNS)) {
                    order("youtube_id", Order.ASCENDING)
                    range(from, from + PAGE_SIZE - 1)
                }
                .decodeList<VideoStampRow>()
            page.mapTo(stamps) { it.toStamp() }
            if (page.size < PAGE_SIZE) return stamps
            from += PAGE_SIZE
        }
    }

    override suspend fun fetchVideos(youtubeIds: List<String>): List<RemoteVideo> {
        require(youtubeIds.size <= CatalogRemoteDataSource.MAX_IDS_PER_REQUEST) { "Too many ids in one request" }
        if (youtubeIds.isEmpty()) return emptyList()
        return client.from(TABLE_VIDEOS)
            .select(Columns.raw(VideoRow.COLUMNS)) {
                filter { isIn("youtube_id", youtubeIds) }
            }
            .decodeList<VideoRow>()
            .mapNotNull { it.toRemoteVideo() }
    }

    private companion object {
        const val TABLE_COLLECTIONS = "collections"
        const val TABLE_VIDEOS = "videos"

        /** PostgREST's max_rows (supabase/config.toml). */
        const val PAGE_SIZE = 1000L
    }
}
