package com.mahmoud.clipkid.core.billing

import com.mahmoud.clipkid.core.billing.di.BillingConfig
import com.mahmoud.clipkid.core.billing.revenuecat.toBillingError
import com.mahmoud.clipkid.core.billing.revenuecat.toOutcome
import com.mahmoud.clipkid.core.model.BillingError
import com.mahmoud.clipkid.core.model.PurchaseOutcome
import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BillingConfigTest {

    @Test
    fun theTestStoreKeyIsUsedOnlyInDebugBuilds() {
        assertEquals("test_x", BillingConfig("goog_x", "test_x", isDebugBuild = true).apiKey)
        assertEquals("goog_x", BillingConfig("goog_x", "test_x", isDebugBuild = false).apiKey)
        assertEquals("goog_x", BillingConfig("goog_x", "", isDebugBuild = true).apiKey)
    }

    @Test
    fun noKeyMeansNoRevenueCat() {
        assertFalse(BillingConfig("", "", isDebugBuild = true).usesRevenueCat)
        assertFalse(BillingConfig("", "test_x", isDebugBuild = false).usesRevenueCat, "a release never falls back to the Test Store")
        assertTrue(BillingConfig("appl_x", "", isDebugBuild = false).usesRevenueCat)
    }

    @Test
    fun storeErrorsMapToWhatTheParentSees() {
        assertEquals(PurchaseOutcome.Cancelled, PurchasesErrorCode.PurchaseCancelledError.toOutcome())
        assertEquals(PurchaseOutcome.Pending, PurchasesErrorCode.PaymentPendingError.toOutcome())
        assertEquals(BillingError.Network, PurchasesErrorCode.OfflineConnectionError.toBillingError())
        assertEquals(BillingError.NotAllowed, PurchasesErrorCode.PurchaseNotAllowedError.toBillingError())
        assertEquals(BillingError.AlreadyOwned, PurchasesErrorCode.ReceiptAlreadyInUseError.toBillingError())
        assertEquals(BillingError.NotAvailable, PurchasesErrorCode.ProductNotAvailableForPurchaseError.toBillingError())
        assertEquals(BillingError.Unknown, PurchasesErrorCode.UnknownError.toBillingError())
    }
}
