package com.mahmoud.feelo.core.billing.fake

import com.mahmoud.feelo.core.domain.repository.BillingRepository
import com.mahmoud.feelo.core.model.BillingError
import com.mahmoud.feelo.core.model.Entitlement
import com.mahmoud.feelo.core.model.Offering
import com.mahmoud.feelo.core.model.Plan
import com.mahmoud.feelo.core.model.PurchaseOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Used when the build has no RevenueCat key (fresh clone, tests, previews): always free, the
 * paywall shows "not available" and nothing can be bought. No made-up prices.
 */
class NoStoreBillingRepository : BillingRepository {
    private val _entitlement = MutableStateFlow<Entitlement>(Entitlement.Free)
    override val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    override val isStarted: Boolean = false

    override fun start() = Unit

    override suspend fun offering(): Offering? = null

    override suspend fun purchase(plan: Plan): PurchaseOutcome = PurchaseOutcome.Failed(BillingError.NotAvailable)

    override suspend fun restore(): PurchaseOutcome = PurchaseOutcome.Failed(BillingError.NotAvailable)

    override suspend fun refresh() = Unit
}
