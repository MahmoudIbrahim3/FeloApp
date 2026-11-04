package com.mahmoud.clipkid.core.data.db

import com.mahmoud.clipkid.core.data.catalog.FakeCatalogDataSource
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.core.model.Favorite
import com.mahmoud.clipkid.core.model.WatchEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MappersTest {

    private val instant = Instant.parse("2026-11-04T14:00:00.123Z")

    @Test
    fun profileRoundTripsBothLimitKinds() {
        val videos = ChildProfile("c1", "يوسف", 0xFFFFB627, AgeBand.A4_6, setOf("letters", "numbers"), DailyLimit.Videos(3))
        val minutes = videos.copy(id = "c2", ageBand = AgeBand.A10_12, allowedCollectionIds = emptySet(), dailyLimit = DailyLimit.Minutes(30))

        assertEquals(videos, videos.toEntity().toModel())
        assertEquals(minutes, minutes.toEntity().toModel())
        assertEquals("minutes", minutes.toEntity().limitType)
    }

    @Test
    fun watchEventRoundTripsWithMillisecondPrecision() {
        val event = WatchEvent("e1", "c1", "REPLACE_L01", instant, 95, completed = false, countsTowardLimit = false)
        assertEquals(event, event.toEntity().toModel())
    }

    @Test
    fun favoriteRoundTrips() {
        val favorite = Favorite("c1", "REPLACE_L01", instant)
        assertEquals(favorite, favorite.toEntity().toModel())
    }

    @Test
    fun wholeFakeCatalogRoundTrips() {
        FakeCatalogDataSource.COLLECTIONS.forEach { assertEquals(it, it.toEntity().toModel()) }
        FakeCatalogDataSource.VIDEOS.forEach { assertEquals(it, it.toEntity().toModel()) }
    }

    @Test
    fun coverIsStoredAsTypedJson() {
        val letters = FakeCatalogDataSource.COLLECTIONS.first().toEntity()
        assertEquals("""{"bg":4294960816,"fg":4287257088,"mark":{"type":"glyph","text":"أ ب"}}""", letters.cover)
        assertEquals("""["2-3","4-6"]""", letters.ageBands)
    }
}
