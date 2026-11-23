package com.mahmoud.feelo.core.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProfileDao {
    @Query("SELECT * FROM child_profiles ORDER BY rowid")
    abstract fun observeAll(): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM child_profiles WHERE id = :id")
    abstract fun observe(id: String): Flow<ChildProfileEntity?>

    @Query("SELECT * FROM child_profiles WHERE id = :id")
    abstract suspend fun get(id: String): ChildProfileEntity?

    @Upsert
    abstract suspend fun upsert(profile: ChildProfileEntity)

    @Query("DELETE FROM child_profiles WHERE id = :id")
    protected abstract suspend fun deleteProfile(id: String)

    @Query("DELETE FROM watch_events WHERE child_id = :childId")
    protected abstract suspend fun deleteEvents(childId: String)

    @Query("DELETE FROM favorites WHERE child_id = :childId")
    protected abstract suspend fun deleteFavorites(childId: String)

    @Query("DELETE FROM hidden_videos WHERE child_id = :childId")
    protected abstract suspend fun deleteHidden(childId: String)

    @Query("DELETE FROM extra_grants WHERE child_id = :childId")
    protected abstract suspend fun deleteGrants(childId: String)

    /** Removes the profile and everything recorded for that child. */
    @Transaction
    open suspend fun deleteWithData(id: String) {
        deleteEvents(id)
        deleteFavorites(id)
        deleteHidden(id)
        deleteGrants(id)
        deleteProfile(id)
    }
}

@Dao
abstract class WatchHistoryDao {
    @Query(
        "SELECT * FROM watch_events WHERE child_id = :childId AND started_at >= :from AND started_at < :until " +
            "ORDER BY started_at",
    )
    abstract fun observeRange(childId: String, from: Long, until: Long): Flow<List<WatchEventEntity>>

    @Query(
        "SELECT * FROM watch_events WHERE child_id = :childId AND started_at >= :from AND started_at < :until " +
            "ORDER BY started_at",
    )
    abstract suspend fun getRange(childId: String, from: Long, until: Long): List<WatchEventEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(event: WatchEventEntity)

    @Query(
        "UPDATE watch_events SET watched_sec = :watchedSec, position_sec = :positionSec, completed = :completed " +
            "WHERE id = :id",
    )
    abstract suspend fun updateProgress(id: String, watchedSec: Int, positionSec: Int, completed: Boolean)

    @Query("SELECT videos FROM extra_grants WHERE child_id = :childId AND date = :date")
    abstract fun observeExtra(childId: String, date: String): Flow<Int?>

    @Query("SELECT videos FROM extra_grants WHERE child_id = :childId AND date = :date")
    abstract suspend fun getExtra(childId: String, date: String): Int?

    @Upsert
    protected abstract suspend fun upsertGrant(grant: ExtraGrantEntity)

    @Transaction
    open suspend fun addExtra(childId: String, date: String, videos: Int) {
        val current = getExtra(childId, date) ?: 0
        upsertGrant(ExtraGrantEntity(childId, date, current + videos))
    }
}

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites WHERE child_id = :childId ORDER BY added_at DESC")
    fun observe(childId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE child_id = :childId AND youtube_id = :youtubeId)")
    suspend fun exists(childId: String, youtubeId: String): Boolean

    @Upsert
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE child_id = :childId AND youtube_id = :youtubeId")
    suspend fun delete(childId: String, youtubeId: String)
}

@Dao
interface HiddenVideosDao {
    @Query("SELECT youtube_id FROM hidden_videos WHERE child_id = :childId")
    fun observeIds(childId: String): Flow<List<String>>

    @Query("SELECT youtube_id FROM hidden_videos WHERE child_id = :childId")
    suspend fun getIds(childId: String): List<String>

    /** Hiding twice keeps the first time. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(hidden: HiddenVideoEntity)
}

@Dao
abstract class CatalogDao {
    @Query("SELECT * FROM cached_collections ORDER BY sort_order, id")
    abstract fun observeCollections(): Flow<List<CachedCollectionEntity>>

    @Query("SELECT * FROM cached_videos ORDER BY collection_id, sort_order, youtube_id")
    abstract fun observeVideos(): Flow<List<CachedVideoEntity>>

    @Query("DELETE FROM cached_collections")
    protected abstract suspend fun clearCollections()

    @Query("DELETE FROM cached_videos")
    protected abstract suspend fun clearVideos()

    @Insert
    protected abstract suspend fun insertCollections(collections: List<CachedCollectionEntity>)

    @Query("SELECT youtube_id, server_updated_at FROM cached_videos")
    abstract suspend fun videoStamps(): List<CachedVideoStamp>

    @Upsert
    protected abstract suspend fun upsertVideos(videos: List<CachedVideoEntity>)

    @Query("DELETE FROM cached_videos WHERE youtube_id IN (:youtubeIds)")
    protected abstract suspend fun deleteVideos(youtubeIds: List<String>)

    /** YouTube data must not be kept longer than 30 days (rule 5). Returns the rows removed. */
    @Query("DELETE FROM cached_videos WHERE refreshed_at < :before")
    abstract suspend fun deleteVideosRefreshedBefore(before: Long): Int

    /**
     * Applies one sync in a single transaction: collections replaced whole (a small table), changed
     * videos upserted, videos the backend no longer lists deleted. [fullResync] clears the videos first.
     */
    @Transaction
    open suspend fun applySync(
        collections: List<CachedCollectionEntity>,
        changedVideos: List<CachedVideoEntity>,
        deletedVideoIds: List<String>,
        fullResync: Boolean,
    ) {
        clearCollections()
        insertCollections(collections)
        if (fullResync) {
            clearVideos()
        } else {
            // SQLite limits bound parameters per statement; stay well under it.
            deletedVideoIds.chunked(500).forEach { deleteVideos(it) }
        }
        upsertVideos(changedVideos)
    }
}
