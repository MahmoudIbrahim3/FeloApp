package com.mahmoud.feelo.core.player

import kotlin.time.Duration.Companion.seconds

/** Fixed values of the embedded YouTube player. */
object PlayerConfig {
    /**
     * Base URL of the player page, sent as the HTTP Referer and used as the player's `origin`.
     * YouTube's Required Minimum Functionality asks native apps to identify as `https://<app ID>`,
     * stable across versions. Without a Referer the player fails with error 153.
     */
    const val BASE_URL = "https://com.mahmoud.feelo"

    /** How often the page reports the current time while playing. */
    val TICK_INTERVAL = 5.seconds

    /** No `onReady` within this time is treated as offline (error [ERROR_TIMEOUT]). */
    val READY_TIMEOUT = 15.seconds

    /** Our own codes, next to YouTube's (2, 5, 100, 101, 150, 153). */
    const val ERROR_SCRIPT_FAILED = -1
    const val ERROR_TIMEOUT = -2

    /** YouTube's "invalid parameter" code, also used when the id fails our format check. */
    const val ERROR_INVALID_PARAM = 2

    /** YouTube: the request has no HTTP Referer (or equivalent client identification). */
    const val ERROR_MISSING_REFERER = 153

    private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

    fun isValidVideoId(videoId: String): Boolean = VIDEO_ID.matches(videoId)

    /** Only these links can be offered to the parental gate; anything else is dropped. */
    fun isOpenableLink(url: String): Boolean =
        url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)
}
