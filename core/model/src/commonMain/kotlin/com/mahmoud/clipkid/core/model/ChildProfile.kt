package com.mahmoud.clipkid.core.model

/** A child on this device. v1 has no accounts: profiles never leave the device. */
data class ChildProfile(
    val id: String,
    val nickname: String,
    /** ARGB, e.g. `0xFFFFB627`. */
    val avatarColor: Long,
    val ageBand: AgeBand,
    val allowedCollectionIds: Set<String>,
    val dailyLimit: DailyLimit,
)

/** What the parent limits per day: a number of videos or a number of minutes. */
sealed interface DailyLimit {
    data class Videos(val count: Int) : DailyLimit {
        init { require(count >= 0) { "count must be >= 0" } }
    }

    data class Minutes(val minutes: Int) : DailyLimit {
        init { require(minutes >= 0) { "minutes must be >= 0" } }
    }
}
