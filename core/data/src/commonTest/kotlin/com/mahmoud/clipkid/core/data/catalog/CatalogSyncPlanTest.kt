package com.mahmoud.clipkid.core.data.catalog

import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogSyncPlanTest {

    private fun manifest(vararg pairs: Pair<String, String>) = pairs.map { VideoStamp(it.first, it.second) }

    @Test
    fun newChangedAndRemovedVideos() {
        val cached = mapOf("same" to "t1", "changed" to "t1", "removed" to "t1")
        val plan = CatalogSyncPlan.of(
            cached,
            manifest("same" to "t1", "changed" to "t2", "new" to "t1"),
            fullResync = false,
        )
        assertEquals(CatalogSyncPlan(toFetch = listOf("changed", "new"), toDelete = listOf("removed")), plan)
    }

    @Test
    fun nothingChanged_nothingToDo() {
        val plan = CatalogSyncPlan.of(mapOf("a" to "t1"), manifest("a" to "t1"), fullResync = false)
        assertEquals(CatalogSyncPlan(emptyList(), emptyList()), plan)
    }

    @Test
    fun rowsCachedBeforeV3_haveNoStamp_andAreFetchedAgain() {
        val plan = CatalogSyncPlan.of(mapOf("a" to ""), manifest("a" to "t1"), fullResync = false)
        assertEquals(listOf("a"), plan.toFetch)
    }

    @Test
    fun emptyManifest_deletesEverything() {
        // e.g. every video's YouTube data passed 30 days without a refresh
        val plan = CatalogSyncPlan.of(mapOf("a" to "t1", "b" to "t1"), emptyList(), fullResync = false)
        assertEquals(CatalogSyncPlan(emptyList(), listOf("a", "b")), plan)
    }

    @Test
    fun fullResync_fetchesEverything() {
        val plan = CatalogSyncPlan.of(mapOf("a" to "t1", "old" to "t0"), manifest("a" to "t1", "b" to "t1"), fullResync = true)
        assertEquals(CatalogSyncPlan(toFetch = listOf("a", "b"), toDelete = listOf("a", "old")), plan)
    }
}
