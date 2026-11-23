package com.mahmoud.feelo.core.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** A message from the player page (youtube_player.html). Times are already whole seconds. */
internal sealed interface BridgeMessage {
    data class Ready(val durationSec: Int, val timeSec: Int) : BridgeMessage

    /** [state] is YouTube's `YT.PlayerState` value. */
    data class StateChange(val state: Int, val timeSec: Int, val durationSec: Int) : BridgeMessage
    data class Tick(val timeSec: Int) : BridgeMessage
    data class Error(val code: Int) : BridgeMessage
}

/** YouTube's `YT.PlayerState`. */
internal object YtState {
    const val UNSTARTED = -1
    const val ENDED = 0
    const val PLAYING = 1
    const val PAUSED = 2
    const val BUFFERING = 3
    const val CUED = 5
}

internal object PlayerBridge {
    /** Name of the Android `@JavascriptInterface` object and the iOS message handler. */
    const val ANDROID_INTERFACE = "FeeloAndroid"
    const val IOS_HANDLER = "feelo"

    /** Compose resource path of the page. */
    const val HTML_PATH = "files/youtube_player.html"
    const val CONFIG_PLACEHOLDER = "__FEELO_CONFIG__"

    private val json = Json { ignoreUnknownKeys = true }

    /** Parses one JSON message; anything malformed or unknown gives null and is ignored. */
    fun parse(raw: String): BridgeMessage? = try {
        val o = json.parseToJsonElement(raw).jsonObject
        when (o.string("type")) {
            "ready" -> BridgeMessage.Ready(durationSec = o.seconds("duration"), timeSec = o.seconds("time"))
            "state" -> o.int("state")?.let {
                BridgeMessage.StateChange(it, timeSec = o.seconds("time"), durationSec = o.seconds("duration"))
            }
            "tick" -> BridgeMessage.Tick(o.seconds("time"))
            "error" -> o.int("code")?.let { BridgeMessage.Error(it) }
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    /**
     * The page with its config filled in. The id must already have passed
     * [PlayerConfig.isValidVideoId]; the config is JSON, so nothing else is pasted into the script.
     */
    fun buildHtml(template: String, videoId: String, startSec: Int): String {
        require(PlayerConfig.isValidVideoId(videoId)) { "Invalid YouTube id" }
        require(CONFIG_PLACEHOLDER in template) { "Player template has no config placeholder" }
        val config = buildJsonObject {
            put("videoId", videoId)
            put("origin", PlayerConfig.BASE_URL)
            put("startSec", startSec.coerceAtLeast(0))
        }
        return template.replace(CONFIG_PLACEHOLDER, config.toString())
    }

    /** Script that runs one command on the page. */
    fun play() = "window.feelo && window.feelo.play();"
    fun pause() = "window.feelo && window.feelo.pause();"
    fun seekTo(sec: Int) = "window.feelo && window.feelo.seekTo(${sec.coerceAtLeast(0)});"

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.content
    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.let { it.intOrNull ?: it.doubleOrNull?.toInt() }
    private fun JsonObject.seconds(key: String): Int =
        (this[key]?.jsonPrimitive?.doubleOrNull ?: 0.0).let { if (it.isNaN() || it < 0) 0 else it.toInt() }
}
