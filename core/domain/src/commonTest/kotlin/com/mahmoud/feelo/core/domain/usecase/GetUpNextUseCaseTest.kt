package com.mahmoud.feelo.core.domain.usecase

import com.mahmoud.feelo.core.domain.testing.ANIMALS
import com.mahmoud.feelo.core.domain.testing.CHILD
import com.mahmoud.feelo.core.domain.testing.COLORS
import com.mahmoud.feelo.core.domain.testing.Catalog
import com.mahmoud.feelo.core.domain.testing.FakeCatalogRepository
import com.mahmoud.feelo.core.domain.testing.FakeHiddenVideosRepository
import com.mahmoud.feelo.core.domain.testing.FakeProfileRepository
import com.mahmoud.feelo.core.domain.testing.FakeWatchHistoryRepository
import com.mahmoud.feelo.core.domain.testing.FixedClock
import com.mahmoud.feelo.core.domain.testing.KUWAIT
import com.mahmoud.feelo.core.domain.testing.OTHER_CHILD
import com.mahmoud.feelo.core.domain.testing.child
import com.mahmoud.feelo.core.domain.testing.event
import com.mahmoud.feelo.core.domain.testing.kuwait
import com.mahmoud.feelo.core.model.ChildProfile
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.core.model.HiddenVideo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class GetUpNextUseCaseTest {

    private val now = kuwait(2026, 11, 4, 17, 0)
    private val clock = FixedClock(now)
    private val history = FakeWatchHistoryRepository()
    private val hidden = FakeHiddenVideosRepository()

    private fun upNext(
        profile: ChildProfile = child(DailyLimit.Videos(10)),
        catalog: FakeCatalogRepository = Catalog.repository(),
    ): GetUpNextUseCase {
        val profiles = FakeProfileRepository(profile)
        return GetUpNextUseCase(
            catalog = catalog,
            profiles = profiles,
            history = history,
            hidden = hidden,
            quota = GetDailyQuotaUseCase(profiles, history, clock, KUWAIT),
            clock = clock,
            timeZone = KUWAIT,
        )
    }

    private fun watchedToday(vararg ids: String) {
        history.events.value += ids.mapIndexed { i, id -> event(id, kuwait(2026, 11, 4, 10, i)) }
    }

    private suspend fun choices(finished: String, useCase: GetUpNextUseCase = upNext()) =
        assertIs<UpNextResult.Choices>(useCase(CHILD, finished))

    private val UpNextResult.Choices.ids get() = all.map { it.youtubeId }

    @Test
    fun primaryIsNextInSameCollection_thenTwoSharingTheFirstTag() = runTest {
        watchedToday("L2")

        val result = choices("L2")

        assertEquals("L2", result.finished?.youtubeId)
        assertEquals("L3", result.primary?.youtubeId)
        assertEquals(ANIMALS, result.tagPicks?.tag)
        assertEquals(listOf("L3", "N1", "S1"), result.ids)
    }

    @Test
    fun primaryWrapsToTheStartOfTheCollectionSkippingWatchedToday() = runTest {
        watchedToday("L1", "L5")

        val result = choices("L5")

        assertEquals("L2", result.primary?.youtubeId)
        // L5 has no tags.
        assertNull(result.tagPicks)
    }

    @Test
    fun excludesVideosWatchedToday() = runTest {
        watchedToday("L2", "L3", "N1")

        val result = choices("L2")

        assertEquals("L4", result.primary?.youtubeId)
        assertEquals(listOf("S1", "S2"), result.tagPicks?.videos?.map { it.youtubeId })
    }

    @Test
    fun videosWatchedYesterdayAreOfferedAgain() = runTest {
        history.events.value = listOf(event("L3", kuwait(2026, 11, 3, 20, 0)))

        assertEquals("L3", choices("L2").primary?.youtubeId)
    }

    @Test
    fun excludesHiddenVideosOfThisChildOnly() = runTest {
        hidden.hidden.value = listOf(
            HiddenVideo(CHILD, "L3", now),
            HiddenVideo(CHILD, "N1", now),
            HiddenVideo(OTHER_CHILD, "S1", now),
        )

        val result = choices("L2")

        assertEquals("L4", result.primary?.youtubeId)
        assertEquals(listOf("S1", "S2"), result.tagPicks?.videos?.map { it.youtubeId })
    }

    @Test
    fun neverOffersVideosOutsideTheChildsAgeBand() = runTest {
        // Everything with the "animals" tag that a 6-8 child may see is watched; C1 (science, 9-12)
        // also has the tag but must not appear.
        watchedToday("L2", "L3", "N1", "S1", "S2")

        val result = choices("L3")

        assertEquals("L4", result.primary?.youtubeId)
        assertNull(result.tagPicks)
        assertTrue(result.all.none { it.youtubeId == "C1" || it.youtubeId == "N3" })
    }

    @Test
    fun onlyAllowedCollections_fallsThroughToTheNextTag() = runTest {
        val lettersOnly = child(DailyLimit.Videos(10), allowed = setOf("letters"))

        val result = choices("L2", upNext(lettersOnly))

        assertEquals("L3", result.primary?.youtubeId)
        // No other "animals" video in letters, so the second tag ("colors") is used.
        assertEquals(COLORS, result.tagPicks?.tag)
        assertEquals(listOf("L4"), result.tagPicks?.videos?.map { it.youtubeId })
    }

    @Test
    fun skipsVideosThatAreNotEmbeddableOrNotMadeForKids() = runTest {
        val catalog = FakeCatalogRepository(
            Catalog.collections,
            Catalog.videos.map {
                when (it.youtubeId) {
                    "L3" -> it.copy(embeddable = false)
                    "N1" -> it.copy(madeForKids = false)
                    else -> it
                }
            },
        )

        val result = choices("L2", upNext(catalog = catalog))

        assertEquals("L4", result.primary?.youtubeId)
        assertEquals(listOf("S1", "S2"), result.tagPicks?.videos?.map { it.youtubeId })
    }

    @Test
    fun neverMoreThanThreeChoices_noDuplicates_neverTheFinishedVideo() = runTest {
        val useCase = upNext()
        for (finished in Catalog.videos.map { it.youtubeId }) {
            val result = choices(finished, useCase)
            assertTrue(result.all.size <= UpNextResult.MAX_CHOICES, "$finished -> ${result.ids}")
            assertEquals(result.ids.distinct(), result.ids, "duplicates for $finished")
            assertTrue(finished !in result.ids)
        }
    }

    @Test
    fun limitReachedWhenTodaysQuotaIsUsed() = runTest {
        watchedToday("L1", "L2")

        assertEquals(UpNextResult.LimitReached, upNext(child(DailyLimit.Videos(2)))(CHILD, "L2"))
    }

    @Test
    fun minutesMode_limitReachedWhenTimeIsUp() = runTest {
        history.events.value = listOf(event("L1", kuwait(2026, 11, 4, 16, 0), watchedSec = 15 * 60))

        assertEquals(UpNextResult.LimitReached, upNext(child(DailyLimit.Minutes(15)))(CHILD, "L1"))
    }

    @Test
    fun unknownFinishedVideoGivesNoChoices() = runTest {
        val result = choices("gone")

        assertNull(result.finished)
        assertEquals(emptyList(), result.all)
    }
}
