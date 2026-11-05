package com.mahmoud.clipkid.core.data.catalog

/**
 * What one incremental sync has to do, from the cached stamps and the backend's manifest:
 * fetch new or changed videos, delete videos the backend no longer lists (unpublished,
 * unavailable, or YouTube data older than 30 days: they drop out of RLS without their
 * `updated_at` changing, so only the manifest shows they are gone).
 */
data class CatalogSyncPlan(
    val toFetch: List<String>,
    val toDelete: List<String>,
) {
    companion object {
        /**
         * [cached] = youtube_id → server updated_at stored with each cached video ("" for rows cached
         * before database v3). [fullResync] ignores the cache and fetches everything.
         */
        fun of(cached: Map<String, String>, manifest: List<VideoStamp>, fullResync: Boolean): CatalogSyncPlan {
            val listed = manifest.associate { it.youtubeId to it.updatedAt }
            if (fullResync) return CatalogSyncPlan(toFetch = listed.keys.sorted(), toDelete = cached.keys.sorted())
            val toFetch = listed.filter { (id, updatedAt) -> cached[id] != updatedAt }.keys.sorted()
            val toDelete = cached.keys.filter { it !in listed }.sorted()
            return CatalogSyncPlan(toFetch, toDelete)
        }
    }
}
