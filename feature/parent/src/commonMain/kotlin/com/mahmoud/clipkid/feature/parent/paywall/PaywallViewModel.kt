package com.mahmoud.clipkid.feature.parent.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.clipkid.core.domain.parent.ParentSession
import com.mahmoud.clipkid.core.domain.repository.BillingRepository
import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.usecase.EnablePremiumCollectionsUseCase
import com.mahmoud.clipkid.core.model.BillingError
import com.mahmoud.clipkid.core.model.Entitlement
import com.mahmoud.clipkid.core.model.Offering
import com.mahmoud.clipkid.core.model.Plan
import com.mahmoud.clipkid.core.model.PlanPeriod
import com.mahmoud.clipkid.core.model.PurchaseOutcome
import com.mahmoud.clipkid.core.model.SubscriptionLimits
import com.mahmoud.clipkid.core.model.isFull
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PaywallBusy { Purchasing, Restoring }

/** A one-line note above the button. Errors in red, the rest in grey. */
enum class PaywallMessage(val isError: Boolean) {
    Pending(false),
    NothingToRestore(false),
    Network(true),
    NotAllowed(true),
    NotAvailable(true),
    AlreadyOwned(true),
    Failed(true),
}

data class PaywallUiState(
    val loading: Boolean = true,
    /** Null while loading, or when the store can't be reached. */
    val offering: Offering? = null,
    val selected: PlanPeriod = PlanPeriod.Annual,
    val busy: PaywallBusy? = null,
    val message: PaywallMessage? = null,
    /** Set when the subscription is active: the "إنت مشترك" card replaces the plans. */
    val subscription: Entitlement.Full? = null,
    /** Bought or restored on this screen just now: a welcome title instead of the status one. */
    val justSubscribed: Boolean = false,
    /** Free collections in the catalog, for "المجاني: ٣ كوليكشنز وطفل واحد". */
    val freeCollections: Int = 0,
    val maxChildren: Int = SubscriptionLimits.MAX_FULL_CHILDREN,
) {
    /** The plan the button buys: the selected one, or the other if the store has only one. */
    val selectedPlan: Plan?
        get() = offering?.let { o ->
            if (selected == PlanPeriod.Annual) o.annual ?: o.monthly else o.monthly ?: o.annual
        }

    val unavailable: Boolean get() = !loading && subscription == null && offering == null

    val savingsPercent: Int? get() = offering?.annualSavingsPercent
}

sealed interface PaywallEvent {
    data class SelectPlan(val period: PlanPeriod) : PaywallEvent
    data object Subscribe : PaywallEvent
    data object Restore : PaywallEvent
    data object Retry : PaywallEvent
    data object Close : PaywallEvent
    data object Terms : PaywallEvent
    data object Privacy : PaywallEvent
    data object ManageSubscription : PaywallEvent
}

sealed interface PaywallEffect {
    data object Close : PaywallEffect

    /** Leaves the app (store, terms, privacy). The paywall is already behind the PIN. */
    data class OpenUrl(val url: String) : PaywallEffect
}

/**
 * Paywall (Paywall.png). Reachable only from parent mode, so only after the PIN.
 *
 * - Plans and prices come from the live store offering; nothing is hard-coded.
 * - Annual is selected by default; the button offers the store's free trial when the parent is
 *   eligible for it.
 * - A purchase or restore keeps parent mode open while the store sheet is up
 *   ([ParentSession.holdForStoreFlow]).
 * - When it turns the subscription on, premium collections are switched on for every child.
 * - The entitlement is observed: a renewal, a cancellation or a purchase from another screen
 *   shows up here at once.
 */
class PaywallViewModel(
    private val billing: BillingRepository,
    private val catalog: CatalogRepository,
    private val enablePremium: EnablePremiumCollectionsUseCase,
    private val session: ParentSession,
    private val links: LegalLinks = LegalLinks.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallUiState())
    val state: StateFlow<PaywallUiState> = _state.asStateFlow()

    private val effectChannel = Channel<PaywallEffect>(Channel.BUFFERED)
    val effects: Flow<PaywallEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            billing.entitlement.collect { value ->
                _state.update { it.copy(subscription = value as? Entitlement.Full) }
            }
        }
        viewModelScope.launch {
            val free = catalog.observeCollections().first().count { !it.isPremium }
            _state.update { it.copy(freeCollections = free) }
        }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val offering = billing.offering()
            _state.update { it.copy(loading = false, offering = offering) }
        }
    }

    fun onEvent(event: PaywallEvent) {
        when (event) {
            is PaywallEvent.SelectPlan -> _state.update { if (it.busy == null) it.copy(selected = event.period, message = null) else it }
            PaywallEvent.Subscribe -> subscribe()
            PaywallEvent.Restore -> restore()
            PaywallEvent.Retry -> if (_state.value.busy == null) load()
            PaywallEvent.Close -> effectChannel.trySend(PaywallEffect.Close)
            PaywallEvent.Terms -> effectChannel.trySend(PaywallEffect.OpenUrl(links.terms))
            PaywallEvent.Privacy -> effectChannel.trySend(PaywallEffect.OpenUrl(links.privacy))
            PaywallEvent.ManageSubscription ->
                _state.value.subscription?.managementUrl?.let { effectChannel.trySend(PaywallEffect.OpenUrl(it)) }
        }
    }

    private fun subscribe() {
        val current = _state.value
        val plan = current.selectedPlan ?: return
        if (current.busy != null) return
        runStoreFlow(PaywallBusy.Purchasing) { billing.purchase(plan) }
    }

    private fun restore() {
        if (_state.value.busy != null) return
        runStoreFlow(PaywallBusy.Restoring) { billing.restore() }
    }

    private fun runStoreFlow(busy: PaywallBusy, call: suspend () -> PurchaseOutcome) {
        val wasFull = billing.entitlement.value.isFull
        _state.update { it.copy(busy = busy, message = null) }
        viewModelScope.launch {
            val outcome = try {
                session.holdForStoreFlow { call() }
            } finally {
                _state.update { it.copy(busy = null) }
            }
            when (outcome) {
                is PurchaseOutcome.Success ->
                    if (outcome.entitlement.isFull) {
                        if (!wasFull) enablePremium()
                        _state.update { it.copy(justSubscribed = true) }
                    } else {
                        _state.update { it.copy(message = PaywallMessage.Failed) }
                    }
                PurchaseOutcome.Cancelled -> Unit
                PurchaseOutcome.Pending -> _state.update { it.copy(message = PaywallMessage.Pending) }
                PurchaseOutcome.NothingToRestore -> _state.update { it.copy(message = PaywallMessage.NothingToRestore) }
                is PurchaseOutcome.Failed -> _state.update { it.copy(message = outcome.error.toMessage()) }
            }
        }
    }

    private fun BillingError.toMessage(): PaywallMessage = when (this) {
        BillingError.Network -> PaywallMessage.Network
        BillingError.NotAllowed -> PaywallMessage.NotAllowed
        BillingError.NotAvailable -> PaywallMessage.NotAvailable
        BillingError.AlreadyOwned -> PaywallMessage.AlreadyOwned
        BillingError.StoreProblem, BillingError.Unknown -> PaywallMessage.Failed
    }
}
