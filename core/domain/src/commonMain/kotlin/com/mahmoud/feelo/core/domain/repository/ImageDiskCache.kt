package com.mahmoud.feelo.core.domain.repository

/**
 * The on-disk cache of YouTube thumbnail images (Coil's disk cache in the app). Only the files on
 * disk: the in-memory cache empties with the process anyway. See PruneImageCacheUseCase.
 */
interface ImageDiskCache {
    /** Deletes every cached image file. */
    suspend fun clear()
}
