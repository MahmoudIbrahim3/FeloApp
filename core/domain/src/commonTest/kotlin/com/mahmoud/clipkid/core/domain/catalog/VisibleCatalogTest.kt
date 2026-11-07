package com.mahmoud.clipkid.core.domain.catalog

import com.mahmoud.clipkid.core.domain.testing.Catalog
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.model.AgeBand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Every video is free: only the parent's choices, the age band and hidden videos decide. */
class VisibleCatalogTest {

    @Test
    fun everyAllowedCollectionOfTheAgeBandIsVisible() {
        val visible = VisibleCatalog.of(child(allowed = setOf("letters", "numbers", "stories")), Catalog.collections, Catalog.videos, emptySet())

        assertEquals(listOf("letters", "numbers", "stories"), visible.collections.map { it.id })
        assertTrue(visible.isVisible("S1"))
    }

    @Test
    fun respectsWhatTheParentAllowed() {
        val visible = VisibleCatalog.of(child(allowed = setOf("letters")), Catalog.collections, Catalog.videos, emptySet())

        assertEquals(listOf("letters"), visible.collections.map { it.id })
        assertFalse(visible.isVisible("S1"))
    }

    @Test
    fun followsTheAgeBand() {
        val toddler = child(ageBand = AgeBand.A2_3, allowed = setOf("letters", "stories"))
        val visible = VisibleCatalog.of(toddler, Catalog.collections, Catalog.videos, emptySet())

        assertEquals(listOf("letters"), visible.collections.map { it.id }, "stories is 4-12: not shown to a 2-3 child")
    }

    @Test
    fun hiddenVideosNeverAppear() {
        val visible = VisibleCatalog.of(child(), Catalog.collections, Catalog.videos, setOf("L2"))

        assertFalse(visible.isVisible("L2"))
        assertTrue(visible.isVisible("L1"))
    }
}
