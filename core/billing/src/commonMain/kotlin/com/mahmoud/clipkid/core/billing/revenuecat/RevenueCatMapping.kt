package com.mahmoud.clipkid.core.billing.revenuecat

import com.mahmoud.clipkid.core.model.BillingError
import com.mahmoud.clipkid.core.model.BillingStore
import com.mahmoud.clipkid.core.model.Entitlement
import com.mahmoud.clipkid.core.model.Plan
import com.mahmoud.clipkid.core.model.PlanPeriod
import com.mahmoud.clipkid.core.model.PurchaseOutcome
import com.mahmoud.clipkid.core.model.TrialPeriod
import com.mahmoud.clipkid.core.model.TrialUnit
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.DiscountPaymentMode
import com.revenuecat.purchases.kmp.models.PeriodType
import com.revenuecat.purchases.kmp.models.Period
import com.revenuecat.purchases.kmp.models.PeriodUnit
import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import com.revenuecat.purchases.kmp.models.Store
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.freePhase
import kotlin.time.Instant

/** The names set up in the RevenueCat dashboard (see /supabase/README.md → RevenueCat). */
internal object RevenueCatIds {
    const val ENTITLEMENT = "full_access"
    const val OFFERING = "default"
    const val MONTHLY = "\$rc_monthly"
    const val ANNUAL = "\$rc_annual"
}

internal fun CustomerInfo.toEntitlement(): Entitlement {
    val info = entitlements[RevenueCatIds.ENTITLEMENT]?.takeIf { it.isActive } ?: return Entitlement.Free
    return Entitlement.Full(
        expiresAt = info.expirationDateMillis?.let(Instant::fromEpochMilliseconds),
        willRenew = info.willRenew,
        isTrial = info.periodType == PeriodType.TRIAL,
        store = when (info.store) {
            Store.APP_STORE, Store.MAC_APP_STORE -> BillingStore.AppStore
            Store.PLAY_STORE -> BillingStore.PlayStore
            Store.TEST_STORE -> BillingStore.TestStore
            else -> BillingStore.Other
        },
        managementUrl = managementUrlString,
    )
}

/**
 * Maps a store product to a [Plan]. [appStoreTrialEligible] is the App Store eligibility check
 * for introductory offers; Google Play only returns offers the user is eligible for, so the
 * product's default option already answers it there.
 */
internal fun StoreProduct.toPlan(packageId: String, period: PlanPeriod, appStoreTrialEligible: Boolean): Plan =
    Plan(
        id = packageId,
        period = period,
        price = price.formatted,
        priceMicros = price.amountMicros,
        currencyCode = price.currencyCode,
        pricePerMonth = if (period == PlanPeriod.Annual) pricePerMonth?.formatted else null,
        trial = freeTrial(appStoreTrialEligible),
    )

/** Google Play: the free phase of the default offer. App Store: a free-trial introductory offer. */
internal fun StoreProduct.freeTrial(appStoreTrialEligible: Boolean): TrialPeriod? {
    defaultOption?.freePhase?.let { return it.billingPeriod.toTrial(1) }
    val intro = appStoreFreeTrial() ?: return null
    if (!appStoreTrialEligible) return null
    return intro.subscriptionPeriod.toTrial(intro.numberOfPeriods.toInt().coerceAtLeast(1))
}

internal fun StoreProduct.appStoreFreeTrial() =
    introductoryDiscount?.takeIf { it.paymentMode == DiscountPaymentMode.FREE_TRIAL }

private fun Period.toTrial(times: Int): TrialPeriod? {
    val unit = when (unit) {
        PeriodUnit.DAY -> TrialUnit.Day
        PeriodUnit.WEEK -> TrialUnit.Week
        PeriodUnit.MONTH -> TrialUnit.Month
        PeriodUnit.YEAR -> TrialUnit.Year
        PeriodUnit.UNKNOWN -> return null
    }
    return TrialPeriod(value * times, unit).takeIf { it.count > 0 }
}

internal fun PurchasesErrorCode.toOutcome(): PurchaseOutcome = when (this) {
    PurchasesErrorCode.PurchaseCancelledError -> PurchaseOutcome.Cancelled
    PurchasesErrorCode.PaymentPendingError -> PurchaseOutcome.Pending
    else -> PurchaseOutcome.Failed(toBillingError())
}

internal fun PurchasesErrorCode.toBillingError(): BillingError = when (this) {
    PurchasesErrorCode.NetworkError,
    PurchasesErrorCode.OfflineConnectionError,
    -> BillingError.Network

    PurchasesErrorCode.StoreProblemError,
    PurchasesErrorCode.UnexpectedBackendResponseError,
    PurchasesErrorCode.UnknownBackendError,
    -> BillingError.StoreProblem

    PurchasesErrorCode.PurchaseNotAllowedError,
    PurchasesErrorCode.InsufficientPermissionsError,
    -> BillingError.NotAllowed

    PurchasesErrorCode.ProductNotAvailableForPurchaseError,
    PurchasesErrorCode.ConfigurationError,
    PurchasesErrorCode.InvalidCredentialsError,
    -> BillingError.NotAvailable

    PurchasesErrorCode.ProductAlreadyPurchasedError,
    PurchasesErrorCode.ReceiptAlreadyInUseError,
    PurchasesErrorCode.ReceiptInUseByOtherSubscriberError,
    -> BillingError.AlreadyOwned

    else -> BillingError.Unknown
}
