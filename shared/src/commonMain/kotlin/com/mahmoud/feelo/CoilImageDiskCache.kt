package com.mahmoud.feelo

import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.mahmoud.feelo.core.domain.repository.ImageDiskCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The thumbnail disk cache of the app's Coil singleton (set up in [App]). Created in composition
 * from `LocalPlatformContext`, because Coil needs the platform context and the DI graph has none.
 */
internal class CoilImageDiskCache(private val context: PlatformContext) : ImageDiskCache {
    override suspend fun clear() {
        // Deleting files: off the main thread.
        withContext(Dispatchers.Default) {
            SingletonImageLoader.get(context).diskCache?.clear()
        }
    }
}
