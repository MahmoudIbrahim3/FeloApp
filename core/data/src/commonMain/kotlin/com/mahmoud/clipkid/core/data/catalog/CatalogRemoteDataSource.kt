package com.mahmoud.clipkid.core.data.catalog

import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.Video

data class RemoteCatalog(val collections: List<Collection>, val videos: List<Video>)

/** Where the catalog comes from: [FakeCatalogDataSource] for now, Supabase in a later phase. */
fun interface CatalogRemoteDataSource {
    suspend fun fetchCatalog(): RemoteCatalog
}
