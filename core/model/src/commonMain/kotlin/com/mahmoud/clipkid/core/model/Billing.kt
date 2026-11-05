package com.mahmoud.clipkid.core.model

import kotlin.time.Instant

/**
 * What the parent has paid for. Content rules only look at [isFull]; the details are for the
 * paywall and the dashboard ("مشترك لحد ...").
 */
sealed interface Entitlement {
    /** The store has not answered yet since the app started. Never shown as locked or open. */
    data object Unknown : Entitlement

    data object Free : Entitlement

    /** The `full_access` entitlement is active (paid, in a free trial, or in a grace period). */
    data class Full(
        /** Null for a lifetime grant (e.g. a promotional entitlement from the RevenueCat dashboard). */
        val expiresAt: Instant?,
        val willRenew: Boolean,
        val isTrial: Boolean,
        val store: BillingStore,
        /** Where the parent manages or cancels the subscription; null when the store gives none. */
        val managementUrl: String?,
    ) : Entitlement
}

val Entitlement.isFull: Boolean get() = this is Entitlement.Full

enum class BillingStore { AppStore, PlayStore, TestStore, Other }

/** Free vs full limits. Which collections are free is decided by the backend (`is_premium`). */
object SubscriptionLimits {
    const val MAX_FREE_CHILDREN: Int = 1
    const val MAX_FULL_CHILDREN: Int = 4

    fun maxChildren(isFull: Boolean): Int = if (isFull) MAX_FULL_CHILDREN else MAX_FREE_CHILDREN
}

enum class PlanPeriod { Monthly, Annual }

enum class TrialUnit { Day, Week, Month, Year }

/** A free trial as the store describes it, e.g. 1 week or 7 days. */
data class TrialPeriod(val count: Int, val unit: TrialUnit) {
    /** Length in days, for "جرّب ٧ أيام بلاش". */
    val days: Int
        get() = count * when (unit) {
            TrialUnit.Day -> 1
            TrialUnit.Week -> 7
            TrialUnit.Month -> 30
            TrialUnit.Year -> 365
        }
}

/**
 * One purchasable plan from the live offering. Every price is formatted by the store in the
 * parent's currency and locale; the app never builds a price itself.
 */
data class Plan(
    /** The RevenueCat package identifier (`$rc_monthly` / `$rc_annual`). */
    val id: String,
    val period: PlanPeriod,
    val price: String,
    val priceMicros: Long,
    val currencyCode: String,
    /** Annual only: the store's per-month equivalent, e.g. "0.83 KWD". */
    val pricePerMonth: String?,
    /** Null when the plan has no trial, or this parent is not eligible for it. */
    val trial: TrialPeriod?,
)

data class Offering(
    val monthly: Plan?,
    val annual: Plan?,
) {
    /** Annual first: it is the default choice on the paywall. */
    val plans: List<Plan> get() = listOfNotNull(annual, monthly)

    /**
     * Whole percent saved by annual against 12 × monthly, rounded down. Null when a plan is
     * missing, the currencies differ, or there is no saving.
     */
    val annualSavingsPercent: Int?
        get() {
            val m = monthly ?: return null
            val a = annual ?: return null
            if (m.currencyCode != a.currencyCode || m.priceMicros <= 0) return null
            val yearOfMonthly = m.priceMicros * 12
            val saved = yearOfMonthly - a.priceMicros
            if (saved <= 0) return null
            return (saved * 100 / yearOfMonthly).toInt().takeIf { it > 0 }
        }
}

/** How a purchase or a restore ended. */
sealed interface PurchaseOutcome {
    /** The store accepted it; [entitlement] is the access right after. */
    data class Success(val entitlement: Entitlement) : PurchaseOutcome

    /** The parent closed the store sheet. Nothing to show. */
    data object Cancelled : PurchaseOutcome

    /** Accepted but waiting for payment (e.g. cash or bank approval on Play). Unlocks when paid. */
    data object Pending : PurchaseOutcome

    /** Restore only: the store account has no active ClipKid subscription. */
    data object NothingToRestore : PurchaseOutcome

    data class Failed(val error: BillingError) : PurchaseOutcome
}

enum class BillingError {
    Network,
    StoreProblem,

    /** Purchases are disabled on this device or account (e.g. Screen Time / family rules). */
    NotAllowed,

    /** The plan is not on sale here, or the store is not set up. */
    NotAvailable,
    AlreadyOwned,
    Unknown,
}
