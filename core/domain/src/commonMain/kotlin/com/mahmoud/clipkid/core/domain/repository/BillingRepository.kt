package com.mahmoud.clipkid.core.domain.repository

import com.mahmoud.clipkid.core.model.Entitlement
import com.mahmoud.clipkid.core.model.Offering
import com.mahmoud.clipkid.core.model.Plan
import com.mahmoud.clipkid.core.model.PurchaseOutcome
import com.mahmoud.clipkid.core.model.isFull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Subscriptions. The store user is anonymous: nothing about the parent or the children is ever
 * sent, only what the store needs to sell and verify the subscription.
 */
interface BillingRepository {
    /**
     * Current access. Starts as [Entitlement.Unknown] and becomes Free or Full as soon as the store
     * answers (or fails). Updates by itself on purchase, renewal, cancellation, expiry and refund.
     */
    val entitlement: StateFlow<Entitlement>

    /** Sets up the store connection. Called once at app start, on the main thread. */
    fun start()

    /** The `default` offering with live store prices, or null when it can't be loaded (offline, store problem). */
    suspend fun offering(): Offering?

    /** Opens the store's purchase sheet. Call only from the paywall, which is behind the PIN. */
    suspend fun purchase(plan: Plan): PurchaseOutcome

    /** Looks for an earlier subscription on this store account. */
    suspend fun restore(): PurchaseOutcome

    /** Asks the store again (app back in the foreground). Never throws. */
    suspend fun refresh()
}

/**
 * Whether premium content is open. Waits for the first known entitlement, so a subscriber never
 * sees locks flash while the store answers.
 */
fun BillingRepository.observeIsFull(): Flow<Boolean> =
    entitlement.filter { it != Entitlement.Unknown }.map { it.isFull }.distinctUntilChanged()

suspend fun BillingRepository.isFullNow(): Boolean = observeIsFull().first()
