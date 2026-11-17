package com.mahmoud.clipkid.core.data.remote

import com.mahmoud.clipkid.core.data.catalog.CatalogRemoteDataSource
import com.mahmoud.clipkid.core.data.catalog.RemoteVideo
import com.mahmoud.clipkid.core.data.catalog.VideoStamp
import com.mahmoud.clipkid.core.model.Collection
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder

/**
 * Reads the catalog through PostgREST. RLS decides what is visible (published collections;
 * approved, embeddable videos refreshed within 30 days, in published collections). The queries
 * repeat the same filters, so a wrongly configured key never brings unpublished content into
 * the app. The 30-day limit is applied by OfflineFirstCatalogRepository with the device clock.
 */
internal class SupabaseCatalogDataSource(private val client: SupabaseClient) : CatalogRemoteDataSource {

    override suspend fun fetchCollections(): List<Collection> =
        client.from(TABLE_COLLECTIONS)
            .select(Columns.raw(CollectionRow.COLUMNS)) {
                filter { eq("is_published", true) }
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
                .select(Columns.raw(VideoStampRow.COLUMNS + PUBLISHED_COLLECTION)) {
                    filter { visibleVideosOnly() }
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
            .select(Columns.raw(VideoRow.COLUMNS + PUBLISHED_COLLECTION)) {
                filter {
                    isIn("youtube_id", youtubeIds)
                    visibleVideosOnly()
                }
            }
            .decodeList<VideoRow>()
            .mapNotNull { it.toRemoteVideo() }
    }

    /**
     * Same rule as the videos RLS policy: approved, embeddable, in a published collection.
     * The manifest and [fetchVideos] must use the same filter, or the sync would keep or delete
     * the wrong videos.
     */
    private fun PostgrestFilterBuilder.visibleVideosOnly() {
        eq("status", "approved")
        eq("yt_embeddable", true)
        eq("$TABLE_COLLECTIONS.is_published", true)
    }

    private companion object {
        const val TABLE_COLLECTIONS = "collections"
        const val TABLE_VIDEOS = "videos"

        /**
         * Embeds the video's collection with an inner join, so the `collections.is_published`
         * filter drops videos whose collection is unpublished. The embedded object in the
         * response is ignored when decoding (ignoreUnknownKeys).
         */
        const val PUBLISHED_COLLECTION = ",$TABLE_COLLECTIONS!inner(is_published)"

        /** PostgREST's max_rows (supabase/config.toml). */
        const val PAGE_SIZE = 1000L
    }
}
