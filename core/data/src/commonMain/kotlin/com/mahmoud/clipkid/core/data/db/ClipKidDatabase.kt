package com.mahmoud.clipkid.core.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
import androidx.sqlite.execSQL
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
    // v2 (Phase 4): watch_events.position_sec for resume and progress bars.
    // v3 (Phase 5): cached_videos.server_updated_at for the incremental Supabase sync.
    // v4 (Phase 8): cached_collections.is_premium dropped (every video is free).
    // v5 (Phase 8): three age bands (3-5, 6-8, 9-12); dev data wiped, same tables.
    // Migrations are hand-written (MIGRATION_1_2 … MIGRATION_4_5) so they don't depend on a 2.json
    // schema file being exported first.
    version = 5,
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

/** v1 → v2: last playback position, for resume and progress bars. Old rows read as 0. */
internal val MIGRATION_1_2 = Migration(1, 2) { connection ->
    connection.execSQL("ALTER TABLE watch_events ADD COLUMN position_sec INTEGER NOT NULL DEFAULT 0")
}

/** v2 → v3: the server's updated_at per cached video. Old rows read as "" and are fetched again. */
internal val MIGRATION_2_3 = Migration(2, 3) { connection ->
    connection.execSQL("ALTER TABLE cached_videos ADD COLUMN server_updated_at TEXT NOT NULL DEFAULT ''")
}

/**
 * v3 → v4: no premium collections any more. SQLite 3.35+ (the bundled driver) drops the column in
 * place; the rest of the cached catalog stays.
 */
internal val MIGRATION_3_4 = Migration(3, 4) { connection ->
    connection.execSQL("ALTER TABLE cached_collections DROP COLUMN is_premium")
}

/**
 * v4 → v5: the four age bands became three (3-5, 6-8, 9-12). Saved children and the cached
 * catalog still hold the old values, which no longer parse. The app was not released yet, so
 * everything on the device goes: the next launch syncs the catalog again and, with no child
 * left, starts onboarding (prepareApp). Children's data first, then the children, then the cache.
 */
internal val MIGRATION_4_5 = Migration(4, 5) { connection ->
    listOf(
        "watch_events",
        "favorites",
        "hidden_videos",
        "extra_grants",
        "child_profiles",
        "cached_videos",
        "cached_collections",
    ).forEach { table -> connection.execSQL("DELETE FROM $table") }
}

/** Same driver, dispatcher and migrations on every platform: bundled SQLite, queries off the main thread. */
fun RoomDatabase.Builder<ClipKidDatabase>.buildClipKid(): ClipKidDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .build()

/**
 * A fresh in-memory database (lost when closed). For tests and Compose previews; the app uses
 * the file database.
 */
fun inMemoryClipKidDatabase(): ClipKidDatabase = inMemoryDatabaseBuilder().buildClipKid()

internal expect fun inMemoryDatabaseBuilder(): RoomDatabase.Builder<ClipKidDatabase>
