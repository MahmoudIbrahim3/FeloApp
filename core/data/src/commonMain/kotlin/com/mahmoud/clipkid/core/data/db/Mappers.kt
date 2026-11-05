package com.mahmoud.clipkid.core.data.db

import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.CoverStyle
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.core.model.Favorite
import com.mahmoud.clipkid.core.model.HiddenVideo
import com.mahmoud.clipkid.core.model.Video
import com.mahmoud.clipkid.core.model.WatchEvent
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Instant

internal val DbJson = Json { ignoreUnknownKeys = true }

private val StringList = ListSerializer(String.serializer())
private val StringSet = SetSerializer(String.serializer())
private val AgeBandSet = SetSerializer(AgeBand.serializer())

private const val LIMIT_VIDEOS = "videos"
private const val LIMIT_MINUTES = "minutes"

internal fun ChildProfile.toEntity() = ChildProfileEntity(
    id = id,
    nickname = nickname,
    avatarColor = avatarColor,
    ageBand = ageBand.name,
    allowedCollectionIds = DbJson.encodeToString(StringSet, allowedCollectionIds),
    limitType = when (dailyLimit) {
        is DailyLimit.Videos -> LIMIT_VIDEOS
        is DailyLimit.Minutes -> LIMIT_MINUTES
    },
    limitValue = when (val limit = dailyLimit) {
        is DailyLimit.Videos -> limit.count
        is DailyLimit.Minutes -> limit.minutes
    },
)

internal fun ChildProfileEntity.toModel() = ChildProfile(
    id = id,
    nickname = nickname,
    avatarColor = avatarColor,
    ageBand = AgeBand.valueOf(ageBand),
    allowedCollectionIds = DbJson.decodeFromString(StringSet, allowedCollectionIds),
    dailyLimit = when (limitType) {
        LIMIT_MINUTES -> DailyLimit.Minutes(limitValue)
        else -> DailyLimit.Videos(limitValue)
    },
)

internal fun WatchEvent.toEntity() = WatchEventEntity(
    id = id,
    childId = childId,
    youtubeId = youtubeId,
    startedAt = startedAt.toEpochMilliseconds(),
    watchedSec = watchedSec,
    completed = completed,
    countsTowardLimit = countsTowardLimit,
    positionSec = positionSec,
)

internal fun WatchEventEntity.toModel() = WatchEvent(
    id = id,
    childId = childId,
    youtubeId = youtubeId,
    startedAt = Instant.fromEpochMilliseconds(startedAt),
    watchedSec = watchedSec,
    completed = completed,
    countsTowardLimit = countsTowardLimit,
    positionSec = positionSec,
)

internal fun Favorite.toEntity() = FavoriteEntity(childId, youtubeId, addedAt.toEpochMilliseconds())

internal fun FavoriteEntity.toModel() = Favorite(childId, youtubeId, Instant.fromEpochMilliseconds(addedAt))

internal fun HiddenVideo.toEntity() = HiddenVideoEntity(childId, youtubeId, hiddenAt.toEpochMilliseconds())

internal fun Collection.toEntity() = CachedCollectionEntity(
    id = id,
    slug = slug,
    titleAr = titleAr,
    cover = DbJson.encodeToString(CoverStyle.serializer(), cover),
    ageBands = DbJson.encodeToString(AgeBandSet, ageBands),
    sortOrder = sortOrder,
    isPremium = isPremium,
)

internal fun CachedCollectionEntity.toModel() = Collection(
    id = id,
    slug = slug,
    titleAr = titleAr,
    cover = DbJson.decodeFromString(CoverStyle.serializer(), cover),
    ageBands = DbJson.decodeFromString(AgeBandSet, ageBands),
    sortOrder = sortOrder,
    isPremium = isPremium,
)

internal fun Video.toEntity(serverUpdatedAt: String = "") = CachedVideoEntity(
    youtubeId = youtubeId,
    collectionId = collectionId,
    titleAr = titleAr,
    durationSec = durationSec,
    thumbnailUrl = thumbnailUrl,
    tags = DbJson.encodeToString(StringList, tags),
    ageBands = DbJson.encodeToString(AgeBandSet, ageBands),
    sortOrder = sortOrder,
    madeForKids = madeForKids,
    embeddable = embeddable,
    refreshedAt = refreshedAt.toEpochMilliseconds(),
    serverUpdatedAt = serverUpdatedAt,
)

internal fun CachedVideoEntity.toModel() = Video(
    youtubeId = youtubeId,
    collectionId = collectionId,
    titleAr = titleAr,
    durationSec = durationSec,
    thumbnailUrl = thumbnailUrl,
    tags = DbJson.decodeFromString(StringList, tags),
    ageBands = DbJson.decodeFromString(AgeBandSet, ageBands),
    sortOrder = sortOrder,
    madeForKids = madeForKids,
    embeddable = embeddable,
    refreshedAt = Instant.fromEpochMilliseconds(refreshedAt),
)
