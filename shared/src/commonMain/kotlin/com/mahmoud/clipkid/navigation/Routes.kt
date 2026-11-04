package com.mahmoud.clipkid.navigation

import com.mahmoud.clipkid.core.model.WatchSource
import kotlinx.serialization.Serializable

// Type-safe routes. Names end in "Dest" so they don't clash with the feature screens (HomeRoute, ...).

@Serializable
data class HomeDest(val childId: String)

@Serializable
data class FavoritesDest(val childId: String)

@Serializable
data class CollectionDest(val childId: String, val collectionId: String)

@Serializable
data class PlayerDest(val childId: String, val youtubeId: String, val source: WatchSource)

@Serializable
data class UpNextDest(val childId: String, val finishedYoutubeId: String)

@Serializable
data class DoneTodayDest(val childId: String)

/** Parental gate. Stub until the parent feature provides the real check. */
@Serializable
data object GateDest
