package com.mahmoud.clipkid.core.data.repository

import com.mahmoud.clipkid.core.data.prefs.InMemoryPreferencesDataStore
import com.mahmoud.clipkid.core.model.BillingStore
import com.mahmoud.clipkid.core.model.Entitlement
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class DataStoreEntitlementCacheTest {

    private val cache = DataStoreEntitlementCache(InMemoryPreferencesDataStore())

    private val full = Entitlement.Full(
        expiresAt = Instant.parse("2027-01-15T00:00:00Z"),
        willRenew = true,
        isTrial = true,
        store = BillingStore.AppStore,
        managementUrl = "https://apps.apple.com/account/subscriptions",
    )

    @Test
    fun nothingCachedReadsAsNull() = runTest {
        assertNull(cache.read())
    }

    @Test
    fun fullRoundTrips() = runTest {
        cache.write(full)
        assertEquals(full, cache.read())
    }

    @Test
    fun freeReplacesFullAndClearsItsDetails() = runTest {
        cache.write(full)
        cache.write(Entitlement.Free)
        assertEquals(Entitlement.Free, cache.read())

        val lifetime = full.copy(expiresAt = null, managementUrl = null)
        cache.write(lifetime)
        assertEquals(lifetime, cache.read(), "no leftover date or URL from an earlier write")
    }

    @Test
    fun unknownIsNeverStored() = runTest {
        cache.write(full)
        cache.write(Entitlement.Unknown)
        assertEquals(full, cache.read())
    }
}
