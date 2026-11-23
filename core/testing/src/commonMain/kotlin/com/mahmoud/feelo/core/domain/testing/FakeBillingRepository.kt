package com.mahmoud.feelo.core.domain.testing

import com.mahmoud.feelo.core.domain.repository.BillingRepository
import com.mahmoud.feelo.core.model.BillingStore
import com.mahmoud.feelo.core.model.Entitlement
import com.mahmoud.feelo.core.model.Offering
import com.mahmoud.feelo.core.model.Plan
import com.mahmoud.feelo.core.model.PlanPeriod
import com.mahmoud.feelo.core.model.PurchaseOutcome
import com.mahmoud.feelo.core.model.TrialPeriod
import com.mahmoud.feelo.core.model.TrialUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

val FULL: Entitlement.Full = Entitlement.Full(
    expiresAt = Instant.parse("2027-01-01T00:00:00Z"),
    willRenew = true,
    isTrial = false,
    store = BillingStore.TestStore,
    managementUrl = "https://example.com/manage",
)

fun testPlan(
    period: PlanPeriod,
    priceMicros: Long,
    trial: TrialPeriod? = TrialPeriod(1, TrialUnit.Week),
    currency: String = "KWD",
) = Plan(
    id = if (period == PlanPeriod.Monthly) "\$rc_monthly" else "\$rc_annual",
    period = period,
    price = "${priceMicros / 1_000_000.0} $currency",
    priceMicros = priceMicros,
    currencyCode = currency,
    pricePerMonth = if (period == PlanPeriod.Annual) "${priceMicros / 12 / 1_000_000.0} $currency" else null,
    trial = trial,
)

/** 1.500 KWD a month, 12.000 a year: annual saves 33%. */
val TEST_OFFERING: Offering = Offering(
    monthly = testPlan(PlanPeriod.Monthly, 1_500_000),
    annual = testPlan(PlanPeriod.Annual, 12_000_000),
)

/**
 * Billing for tests. [entitlement] can be changed at any time (renewal, expiry). A purchase
 * returns [purchaseOutcome]; when [purchaseGate] is set, it waits for it first.
 */
class FakeBillingRepository(
    initial: Entitlement = Entitlement.Free,
    var offering: Offering? = TEST_OFFERING,
) : BillingRepository {
    override val entitlement = MutableStateFlow(initial)

    var purchaseOutcome: PurchaseOutcome = PurchaseOutcome.Success(FULL)
    var restoreOutcome: PurchaseOutcome = PurchaseOutcome.NothingToRestore
    var purchaseGate: CompletableDeferred<Unit>? = null
    val purchased = mutableListOf<Plan>()
    var restoreCalls = 0
    var refreshCalls = 0
    var started = false
    var startCalls = 0
    override val isStarted: Boolean get() = started

    override fun start() {
        startCalls++
        started = true
    }

    override suspend fun offering(): Offering? = offering

    override suspend fun purchase(plan: Plan): PurchaseOutcome {
        purchased += plan
        purchaseGate?.await()
        return purchaseOutcome.also(::apply)
    }

    override suspend fun restore(): PurchaseOutcome {
        restoreCalls++
        return restoreOutcome.also(::apply)
    }

    override suspend fun refresh() {
        if (started) refreshCalls++
    }

    private fun apply(outcome: PurchaseOutcome) {
        if (outcome is PurchaseOutcome.Success) entitlement.value = outcome.entitlement
    }
}
