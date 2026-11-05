package com.mahmoud.clipkid.core.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

// Plain columns only; JSON and enum mapping happens in Mappers.kt so the schema stays obvious.
// Instants are epoch milliseconds, dates are ISO strings (2026-11-04).

@Entity(tableName = "child_profiles")
data class ChildProfileEntity(
    @PrimaryKey val id: String,
    val nickname: String,
    @ColumnInfo(name = "avatar_color") val avatarColor: Long,
    @ColumnInfo(name = "age_band") val ageBand: String,
    /** JSON array of collection ids. */
    @ColumnInfo(name = "allowed_collection_ids") val allowedCollectionIds: String,
    /** "videos" or "minutes". */
    @ColumnInfo(name = "limit_type") val limitType: String,
    @ColumnInfo(name = "limit_value") val limitValue: Int,
)

@Entity(
    tableName = "watch_events",
    indices = [Index(value = ["child_id", "started_at"])],
)
data class WatchEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "youtube_id") val youtubeId: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "watched_sec") val watchedSec: Int,
    val completed: Boolean,
    @ColumnInfo(name = "counts_toward_limit") val countsTowardLimit: Boolean,
    /** Added in v2 (real player): last playback position. Old rows read as 0. */
    @ColumnInfo(name = "position_sec", defaultValue = "0") val positionSec: Int = 0,
)

@Entity(tableName = "favorites", primaryKeys = ["child_id", "youtube_id"])
data class FavoriteEntity(
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "youtube_id") val youtubeId: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

@Entity(tableName = "hidden_videos", primaryKeys = ["child_id", "youtube_id"])
data class HiddenVideoEntity(
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "youtube_id") val youtubeId: String,
    @ColumnInfo(name = "hidden_at") val hiddenAt: Long,
)

@Entity(tableName = "extra_grants", primaryKeys = ["child_id", "date"])
data class ExtraGrantEntity(
    @ColumnInfo(name = "child_id") val childId: String,
    val date: String,
    val videos: Int,
)

@Entity(tableName = "cached_collections")
data class CachedCollectionEntity(
    @PrimaryKey val id: String,
    val slug: String,
    @ColumnInfo(name = "title_ar") val titleAr: String,
    /** JSON CoverStyle. */
    val cover: String,
    /** JSON array of AgeBand. */
    @ColumnInfo(name = "age_bands") val ageBands: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "is_premium") val isPremium: Boolean,
)

@Entity(
    tableName = "cached_videos",
    indices = [Index(value = ["collection_id"])],
)
data class CachedVideoEntity(
    @PrimaryKey @ColumnInfo(name = "youtube_id") val youtubeId: String,
    @ColumnInfo(name = "collection_id") val collectionId: String,
    @ColumnInfo(name = "title_ar") val titleAr: String,
    @ColumnInfo(name = "duration_sec") val durationSec: Int,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String,
    /** JSON array of Arabic tag labels. */
    val tags: String,
    /** JSON array of AgeBand. */
    @ColumnInfo(name = "age_bands") val ageBands: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "made_for_kids") val madeForKids: Boolean,
    val embeddable: Boolean,
    @ColumnInfo(name = "refreshed_at") val refreshedAt: Long,
)
