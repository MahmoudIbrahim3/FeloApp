package com.mahmoud.clipkid.core.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerBridgeTest {

    @Test
    fun parsesEveryMessageType() {
        assertEquals(
            BridgeMessage.Ready(durationSec = 185, timeSec = 0),
            PlayerBridge.parse("""{"type":"ready","duration":185.43,"time":0}"""),
        )
        assertEquals(
            BridgeMessage.StateChange(state = 1, timeSec = 12, durationSec = 185),
            PlayerBridge.parse("""{"type":"state","state":1,"time":12.9,"duration":185}"""),
        )
        assertEquals(BridgeMessage.Tick(timeSec = 17), PlayerBridge.parse("""{"type":"tick","time":17.2}"""))
        assertEquals(BridgeMessage.Error(code = 150), PlayerBridge.parse("""{"type":"error","code":150}"""))
        assertEquals(BridgeMessage.Error(code = -1), PlayerBridge.parse("""{"type":"error","code":-1}"""))
    }

    @Test
    fun missingOrBadTimesBecomeZero() {
        assertEquals(BridgeMessage.Tick(0), PlayerBridge.parse("""{"type":"tick"}"""))
        assertEquals(BridgeMessage.Tick(0), PlayerBridge.parse("""{"type":"tick","time":-3}"""))
        assertEquals(BridgeMessage.Tick(0), PlayerBridge.parse("""{"type":"tick","time":"soon"}"""))
    }

    @Test
    fun malformedOrUnknownMessagesAreIgnored() {
        assertNull(PlayerBridge.parse("not json"))
        assertNull(PlayerBridge.parse("[]"))
        assertNull(PlayerBridge.parse("""{"type":"party"}"""))
        assertNull(PlayerBridge.parse("""{"type":"state"}"""))
        assertNull(PlayerBridge.parse("""{"type":"error"}"""))
    }

    @Test
    fun buildHtml_fillsTheConfigWithIdOriginAndStart() {
        val html = PlayerBridge.buildHtml("var CONFIG = __CLIPKID_CONFIG__;", "dQw4w9WgXcQ", startSec = 42)
        assertEquals(
            """var CONFIG = {"videoId":"dQw4w9WgXcQ","origin":"https://com.mahmoud.clipkid","startSec":42};""",
            html,
        )
    }

    @Test
    fun buildHtml_rejectsAnInvalidId() {
        assertFailsWith<IllegalArgumentException> {
            PlayerBridge.buildHtml("__CLIPKID_CONFIG__", "</script><script>alert(1)", 0)
        }
    }

    @Test
    fun videoIdFormat() {
        assertTrue(PlayerConfig.isValidVideoId("dQw4w9WgXcQ"))
        assertTrue(PlayerConfig.isValidVideoId("a-b_c-d_e-f"))
        assertFalse(PlayerConfig.isValidVideoId("REPLACE_L01"))
        assertFalse(PlayerConfig.isValidVideoId("short"))
        assertFalse(PlayerConfig.isValidVideoId("dQw4w9WgXcQ1"))
        assertFalse(PlayerConfig.isValidVideoId("dQw4w9WgX\"Q"))
    }

    @Test
    fun onlyWebLinksCanBeOpened() {
        assertTrue(PlayerConfig.isOpenableLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertFalse(PlayerConfig.isOpenableLink("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(PlayerConfig.isOpenableLink("javascript:alert(1)"))
        assertFalse(PlayerConfig.isOpenableLink("about:blank"))
    }
}
