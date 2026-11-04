package com.mahmoud.clipkid.core.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        ChildProfileEntity::class,
        WatchEventEntity::class,
        FavoriteEntity::class,
        HiddenVideoEntity::class,
        ExtraGrantEntity::class,
        CachedCollectionEntity::class,
        CachedVideoEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(ClipKidDatabaseConstructor::class)
abstract class ClipKidDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun hiddenVideosDao(): HiddenVideosDao
    abstract fun catalogDao(): CatalogDao

    companion object {
        const val FILE_NAME = "clipkid.db"
    }
}

/** Implemented by the Room compiler for each platform. */
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object ClipKidDatabaseConstructor : RoomDatabaseConstructor<ClipKidDatabase> {
    override fun initialize(): ClipKidDatabase
}

/** Same driver and dispatcher on every platform: bundled SQLite, queries off the main thread. */
fun RoomDatabase.Builder<ClipKidDatabase>.buildClipKid(): ClipKidDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

/**
 * A fresh in-memory database (lost when closed). For tests and Compose previews; the app uses
 * the file database.
 */
fun inMemoryClipKidDatabase(): ClipKidDatabase = inMemoryDatabaseBuilder().buildClipKid()

internal expect fun inMemoryDatabaseBuilder(): RoomDatabase.Builder<ClipKidDatabase>
