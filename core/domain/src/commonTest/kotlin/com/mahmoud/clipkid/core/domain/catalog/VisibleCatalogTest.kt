package com.mahmoud.clipkid.core.domain.catalog

import com.mahmoud.clipkid.core.domain.testing.Catalog
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.model.AgeBand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisibleCatalogTest {

    /** Catalog with "stories" (4-12) as the premium collection. */
    private val collections = Catalog.collections.map { if (it.id == "stories") it.copy(isPremium = true) else it }
    private val childWithStories = child(allowed = setOf("letters", "numbers", "stories"))

    @Test
    fun freeHidesPremiumVideosButListsTheCollectionAsLocked() {
        val visible = VisibleCatalog.of(childWithStories, collections, Catalog.videos, emptySet(), isFull = false)

        assertEquals(listOf("letters", "numbers"), visible.collections.map { it.id })
        assertEquals(listOf("stories"), visible.lockedCollections.map { it.id })
        assertTrue(visible.videos.none { it.collectionId == "stories" })
        assertFalse(visible.isVisible("S1"))
    }

    @Test
    fun freeListsLockedPremiumOfTheAgeBandEvenIfTheParentNeverAllowedIt() {
        val visible = VisibleCatalog.of(child(allowed = setOf("letters")), collections, Catalog.videos, emptySet(), isFull = false)
        assertEquals(listOf("stories"), visible.lockedCollections.map { it.id })
    }

    @Test
    fun lockedPremiumFollowsTheAgeBand() {
        val toddler = child(ageBand = AgeBand.A2_3, allowed = setOf("letters", "stories"))
        val visible = VisibleCatalog.of(toddler, collections, Catalog.videos, emptySet(), isFull = false)
        assertTrue(visible.lockedCollections.isEmpty(), "stories is 4-12: not shown to a 2-3 child at all")
    }

    @Test
    fun fullShowsAllowedPremiumAndLocksNothing() {
        val visible = VisibleCatalog.of(childWithStories, collections, Catalog.videos, emptySet(), isFull = true)

        assertEquals(listOf("letters", "numbers", "stories"), visible.collections.map { it.id })
        assertTrue(visible.lockedCollections.isEmpty())
        assertTrue(visible.isVisible("S1"))
    }

    @Test
    fun fullStillRespectsWhatTheParentAllowed() {
        val visible = VisibleCatalog.of(child(allowed = setOf("letters")), collections, Catalog.videos, emptySet(), isFull = true)
        assertEquals(listOf("letters"), visible.collections.map { it.id })
        assertTrue(visible.lockedCollections.isEmpty())
    }

    @Test
    fun expiryRelocksWithoutChangingTheProfile() {
        val profile = childWithStories
        VisibleCatalog.of(profile, collections, Catalog.videos, emptySet(), isFull = true)
        val afterExpiry = VisibleCatalog.of(profile, collections, Catalog.videos, emptySet(), isFull = false)

        assertEquals(listOf("stories"), afterExpiry.lockedCollections.map { it.id })
        assertTrue("stories" in profile.allowedCollectionIds, "settings are kept for when the subscription is back")
    }
}
