package com.mahmoud.clipkid.core.data.catalog

import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.Video

/**
 * A video as the backend currently lists it: its id and the server's `updated_at`, compared as an
 * opaque string (no clock or precision games).
 */
data class VideoStamp(val youtubeId: String, val updatedAt: String)

/** A video row plus its server `updated_at`, kept in the cache for the next diff. */
data class RemoteVideo(val video: Video, val updatedAt: String)

/**
 * Where the catalog comes from: Supabase ([com.mahmoud.clipkid.core.data.remote.SupabaseCatalogDataSource])
 * or [FakeCatalogDataSource] (tests, previews, a build without Supabase config).
 *
 * Everything returned is already filtered by the backend (RLS): published collections and
 * approved, embeddable videos refreshed within 30 days.
 */
interface CatalogRemoteDataSource {
    /** All visible collections (a small table, fetched whole every sync). */
    suspend fun fetchCollections(): List<Collection>

    /** id + updated_at of every visible video: what the cache must end up containing. */
    suspend fun fetchVideoManifest(): List<VideoStamp>

    /** Full rows for these ids (at most [MAX_IDS_PER_REQUEST]). Ids no longer visible are left out. */
    suspend fun fetchVideos(youtubeIds: List<String>): List<RemoteVideo>

    companion object {
        /** Keeps `youtube_id=in.(…)` URLs short. */
        const val MAX_IDS_PER_REQUEST = 100
    }
}
