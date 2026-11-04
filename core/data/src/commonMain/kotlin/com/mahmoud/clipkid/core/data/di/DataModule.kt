package com.mahmoud.clipkid.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mahmoud.clipkid.core.data.catalog.CatalogRemoteDataSource
import com.mahmoud.clipkid.core.data.catalog.FakeCatalogDataSource
import com.mahmoud.clipkid.core.data.db.ClipKidDatabase
import com.mahmoud.clipkid.core.data.db.inMemoryClipKidDatabase
import com.mahmoud.clipkid.core.data.prefs.InMemoryPreferencesDataStore
import com.mahmoud.clipkid.core.data.repository.DataStoreSettingsRepository
import com.mahmoud.clipkid.core.data.repository.NoOpHiddenVideoReporter
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
import com.mahmoud.clipkid.core.domain.repository.HiddenVideoReporter
import com.mahmoud.clipkid.core.domain.repository.HiddenVideosRepository
import com.mahmoud.clipkid.core.domain.repository.IdGenerator
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import com.mahmoud.clipkid.core.domain.repository.WatchHistoryRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/** Build-time switches for the data layer. */
data class DataConfig(
    /** true: catalog comes from [FakeCatalogDataSource]. false: Supabase (later phase). */
    val useFakeCatalog: Boolean = true,
)

/**
 * Platform storage: the Room database file, the DataStore file and the [SecureStore]
 * (Android Keystore / iOS Keychain). Android needs `androidContext()`.
 */
expect val platformDataModule: Module

val dataModule: Module = module {
    includes(platformDataModule)

    single { DataConfig() }
    single<CatalogRemoteDataSource> {
        check(get<DataConfig>().useFakeCatalog) { "Supabase catalog source arrives in a later phase" }
        FakeCatalogDataSource()
    }
    single { PinHasher() }

    single<CatalogRepository> { OfflineFirstCatalogRepository(get<ClipKidDatabase>().catalogDao(), get(), get()) }
    single<ProfileRepository> { RoomProfileRepository(get<ClipKidDatabase>().profileDao()) }
    single<WatchHistoryRepository> { RoomWatchHistoryRepository(get<ClipKidDatabase>().watchHistoryDao()) }
    single<FavoritesRepository> { RoomFavoritesRepository(get<ClipKidDatabase>().favoritesDao()) }
    single<HiddenVideosRepository> { RoomHiddenVideosRepository(get<ClipKidDatabase>().hiddenVideosDao()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get(), get(), get()) }
    single<HiddenVideoReporter> { NoOpHiddenVideoReporter }
    single<IdGenerator> { UuidIdGenerator }
}

/**
 * Replaces [platformDataModule] with in-memory storage (Room in memory, in-memory DataStore and
 * SecureStore). Load it after `dataModule` with overrides allowed; for tests and previews.
 */
fun inMemoryPlatformDataModule(): Module = module {
    single<ClipKidDatabase> { inMemoryClipKidDatabase() }
    single<DataStore<Preferences>> { InMemoryPreferencesDataStore() }
    single<SecureStore> { InMemorySecureStore() }
}
