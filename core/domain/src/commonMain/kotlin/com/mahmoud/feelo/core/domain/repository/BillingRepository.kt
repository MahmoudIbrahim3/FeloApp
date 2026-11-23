package com.mahmoud.feelo.core.domain.repository

import com.mahmoud.feelo.core.model.Entitlement
import com.mahmoud.feelo.core.model.Offering
import com.mahmoud.feelo.core.model.Plan
import com.mahmoud.feelo.core.model.PurchaseOutcome
import com.mahmoud.feelo.core.model.isFull
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
     * Current access. Starts as [Entitlement.Unknown] only until the on-device cache is read (Free
     * when nothing is cached), so it never waits for the store. Once [start]ed, it follows the
     * store: purchase, renewal, cancellation, expiry and refund.
     */
    val entitlement: StateFlow<Entitlement>

    /** True once [start] has set up the store connection in this process. */
    val isStarted: Boolean

    /**
     * Sets up the store connection. Idempotent; call on the main thread. Only through
     * [com.mahmoud.feelo.core.domain.usecase.ConnectBillingUseCase], which checks the parent's
     * consent first.
     */
    fun start()

    /**
     * The `default` offering with live store prices, or null when it can't be loaded (offline,
     * store problem, or not [start]ed).
     */
    suspend fun offering(): Offering?

    /** Opens the store's purchase sheet. Call only from the paywall, which is behind the PIN. */
    suspend fun purchase(plan: Plan): PurchaseOutcome

    /** Looks for an earlier subscription on this store account. */
    suspend fun restore(): PurchaseOutcome

    /** Asks the store again. Does nothing before [start]. Never throws. */
    suspend fun refresh()
}

/**
 * Whether the subscription is on. Waits for the first known entitlement (the on-device cache), so
 * a subscriber never sees locks flash at launch.
 */
fun BillingRepository.observeIsFull(): Flow<Boolean> =
    entitlement.filter { it != Entitlement.Unknown }.map { it.isFull }.distinctUntilChanged()

suspend fun BillingRepository.isFullNow(): Boolean = observeIsFull().first()
