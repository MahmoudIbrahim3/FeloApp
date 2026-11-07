package com.mahmoud.clipkid.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mahmoud.clipkid.core.data.catalog.CatalogRemoteDataSource
import com.mahmoud.clipkid.core.data.catalog.FakeCatalogDataSource
import com.mahmoud.clipkid.core.data.config.SupabaseBuildConfig
import com.mahmoud.clipkid.core.data.db.ClipKidDatabase
import com.mahmoud.clipkid.core.data.db.inMemoryClipKidDatabase
import com.mahmoud.clipkid.core.data.network.AlwaysOnlineNetworkMonitor
import com.mahmoud.clipkid.core.data.prefs.InMemoryPreferencesDataStore
import com.mahmoud.clipkid.core.data.remote.SupabaseCatalogDataSource
import com.mahmoud.clipkid.core.data.remote.SupabaseVideoReporter
import com.mahmoud.clipkid.core.data.remote.createClipKidSupabase
import com.mahmoud.clipkid.core.data.repository.DataStoreEntitlementCache
import com.mahmoud.clipkid.core.data.repository.DataStoreSettingsRepository
import com.mahmoud.clipkid.core.data.repository.NoOpVideoReporter
import com.mahmoud.clipkid.core.data.repository.OfflineFirstCatalogRepository
import com.mahmoud.clipkid.core.data.repository.RoomFavoritesRepository
import com.mahmoud.clipkid.core.data.repository.RoomHiddenVideosRepository
import com.mahmoud.clipkid.core.data.repository.RoomProfileRepository
import com.mahmoud.clipkid.core.data.repository.RoomWatchHistoryRepository
import com.mahmoud.clipkid.core.data.repository.UuidIdGenerator
import com.mahmoud.clipkid.core.data.secure.InMemorySecureStore
import com.mahmoud.clipkid.core.data.secure.PinHasher
import com.mahmoud.clipkid.core.data.secure.SecureStore
import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.FavoritesRepository
import com.mahmoud.clipkid.core.domain.repository.VideoReporter
import com.mahmoud.clipkid.core.domain.repository.HiddenVideosRepository
import com.mahmoud.clipkid.core.domain.repository.IdGenerator
import com.mahmoud.clipkid.core.domain.repository.NetworkMonitor
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.EntitlementCache
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import com.mahmoud.clipkid.core.domain.repository.WatchHistoryRepository
import io.github.jan.supabase.SupabaseClient
import org.koin.core.module.Module
import org.koin.dsl.module

/** Build-time switches for the data layer. */
data class DataConfig(
    /** true: catalog comes from [FakeCatalogDataSource]. false: Supabase. */
    val useFakeCatalog: Boolean = false,
    /** From local.properties / env at build time (BuildKonfig). Never committed for production. */
    val supabaseUrl: String = SupabaseBuildConfig.SUPABASE_URL,
    val supabasePublishableKey: String = SupabaseBuildConfig.SUPABASE_PUBLISHABLE_KEY,
) {
    val hasSupabase: Boolean get() = supabaseUrl.isNotBlank() && supabasePublishableKey.isNotBlank()

    /** Supabase unless fakes were asked for or the build has no Supabase config. */
    val usesSupabase: Boolean get() = !useFakeCatalog && hasSupabase
}

/**
 * Platform storage and services: the Room database file, the DataStore file, the [SecureStore]
 * (Android Keystore / iOS Keychain) and the [NetworkMonitor]. Android needs `androidContext()`.
 */
expect val platformDataModule: Module

val dataModule: Module = module {
    includes(platformDataModule)

    single { DataConfig() }
    // Created only when the config has Supabase values (see usesSupabase).
    single<SupabaseClient> {
        val config = get<DataConfig>()
        createClipKidSupabase(config.supabaseUrl, config.supabasePublishableKey)
    }
    single<CatalogRemoteDataSource> {
        val config = get<DataConfig>()
        if (config.usesSupabase) {
            SupabaseCatalogDataSource(get())
        } else {
            if (!config.useFakeCatalog) {
                println(
                    "ClipKid WARNING: no Supabase config (supabase.url / supabase.publishableKey in " +
                        "local.properties, or CLIPKID_SUPABASE_URL / CLIPKID_SUPABASE_PUBLISHABLE_KEY). " +
                        "Using the FAKE catalog.",
                )
            }
            FakeCatalogDataSource(clock = get())
        }
    }
    single { PinHasher() }

    single<CatalogRepository> { OfflineFirstCatalogRepository(get<ClipKidDatabase>().catalogDao(), get(), get(), get()) }
    single<ProfileRepository> { RoomProfileRepository(get<ClipKidDatabase>().profileDao()) }
    single<WatchHistoryRepository> { RoomWatchHistoryRepository(get<ClipKidDatabase>().watchHistoryDao()) }
    single<FavoritesRepository> { RoomFavoritesRepository(get<ClipKidDatabase>().favoritesDao()) }
    single<HiddenVideosRepository> { RoomHiddenVideosRepository(get<ClipKidDatabase>().hiddenVideosDao()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get(), get(), get()) }
    single<EntitlementCache> { DataStoreEntitlementCache(get()) }
    single<VideoReporter> { if (get<DataConfig>().usesSupabase) SupabaseVideoReporter(get()) else NoOpVideoReporter }
    single<IdGenerator> { UuidIdGenerator }
}

/**
 * Replaces [platformDataModule] with in-memory storage (Room in memory, in-memory DataStore and
 * SecureStore), an always-online network and the fake catalog, so tests and previews never touch
 * the network. Load it after `dataModule` with overrides allowed.
 */
fun inMemoryPlatformDataModule(): Module = module {
    single { DataConfig(useFakeCatalog = true) }
    single<ClipKidDatabase> { inMemoryClipKidDatabase() }
    single<DataStore<Preferences>> { InMemoryPreferencesDataStore() }
    single<SecureStore> { InMemorySecureStore() }
    single<NetworkMonitor> { AlwaysOnlineNetworkMonitor }
}
