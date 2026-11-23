package com.mahmoud.feelo.core.billing.revenuecat

import com.mahmoud.feelo.core.domain.repository.BillingRepository
import com.mahmoud.feelo.core.domain.repository.EntitlementCache
import com.mahmoud.feelo.core.model.BillingError
import com.mahmoud.feelo.core.model.Entitlement
import com.mahmoud.feelo.core.model.Offering
import com.mahmoud.feelo.core.model.Plan
import com.mahmoud.feelo.core.model.PlanPeriod
import com.mahmoud.feelo.core.model.PurchaseOutcome
import com.mahmoud.feelo.core.model.isFull
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.configure
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.ktx.awaitTrialOrIntroPriceEligibility
import com.revenuecat.purchases.kmp.models.CacheFetchPolicy
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.IntroEligibilityStatus
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * [BillingRepository] on RevenueCat.
 *
 * Privacy (child-directed app):
 * - RevenueCat is configured only after the parent's consent, from parent mode (or to re-check a
 *   subscription whose period ended): see ConnectBillingUseCase. Until then [entitlement] comes
 *   from the on-device [EntitlementCache] and nothing is sent. Once configured in a process, the
 *   SDK may refresh by itself when the app returns to the foreground, until the process ends.
 * - The RevenueCat user is anonymous (`appUserId = null`, never `logIn`): a random id kept on the
 *   device. No attributes, email, device identifiers, ad ids or integrations are ever set.
 * - Store in-app messages (e.g. "payment declined") are off: they would pop a store sheet over
 *   whatever the child is watching. The parent sees billing problems in the store instead.
 * - App Store promoted purchases are ignored: every purchase must start from the paywall,
 *   which is behind the PIN.
 */
internal class RevenueCatBillingRepository(
    private val apiKey: String,
    private val debugLogs: Boolean,
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val cache: EntitlementCache,
) : BillingRepository {

    private val _entitlement = MutableStateFlow<Entitlement>(Entitlement.Unknown)
    override val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    @Volatile private var started = false
    override val isStarted: Boolean get() = started

    init {
        // The last known status, without contacting the store. Nothing cached = free.
        scope.launch {
            val cached = cache.read() ?: Entitlement.Free
            _entitlement.compareAndSet(Entitlement.Unknown, cached)
        }
    }
    private var expiryCheck: Job? = null

    /** Packages of the last loaded offering, by id, so [purchase] buys exactly what was shown. */
    private var packages: Map<String, Package> = emptyMap()

    override fun start() {
        if (started) return
        started = true
        Purchases.logLevel = if (debugLogs) LogLevel.DEBUG else LogLevel.WARN
        if (!Purchases.isConfigured) {
            Purchases.configure(apiKey) {
                appUserId = null
                showInAppMessagesAutomatically = false
            }
        }
        Purchases.sharedInstance.delegate = object : PurchasesDelegate {
            override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) = publish(customerInfo)

            override fun onPurchasePromoProduct(
                product: StoreProduct,
                startPurchase: (
                    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
                    onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
                ) -> Unit,
            ) = Unit
        }
        scope.launch { load(CacheFetchPolicy.CACHED_OR_FETCHED) }
        // Offline on a fresh install, the store may never answer: treat as free until it does.
        scope.launch {
            delay(UNKNOWN_TIMEOUT)
            _entitlement.compareAndSet(Entitlement.Unknown, Entitlement.Free)
        }
    }

    override suspend fun refresh() {
        if (started) load(CacheFetchPolicy.CACHED_OR_FETCHED)
    }

    private suspend fun load(policy: CacheFetchPolicy) {
        try {
            publish(Purchases.sharedInstance.awaitCustomerInfo(policy))
        } catch (e: PurchasesException) {
            // Keep the last known access (the SDK caches it); only an unknown start becomes free.
            _entitlement.compareAndSet(Entitlement.Unknown, Entitlement.Free)
        }
    }

    private fun publish(info: CustomerInfo) {
        val value = info.toEntitlement()
        _entitlement.value = value
        scope.launch { cache.write(value) }
        scheduleExpiryCheck(value)
    }

    /**
     * Asks the store again right after the current period ends, so a cancelled subscription locks
     * even if the app stays open. Renewals and grace periods move the date forward.
     */
    private fun scheduleExpiryCheck(value: Entitlement) {
        expiryCheck?.cancel()
        val expiresAt = (value as? Entitlement.Full)?.expiresAt ?: return
        expiryCheck = scope.launch {
            val wait = expiresAt - clock.now()
            delay(wait.coerceAtLeast(0.seconds) + EXPIRY_MARGIN)
            load(CacheFetchPolicy.FETCH_CURRENT)
        }
    }

    override suspend fun offering(): Offering? {
        if (!started) return null
        val offerings = try {
            Purchases.sharedInstance.awaitOfferings()
        } catch (e: PurchasesException) {
            return null
        }
        val current = offerings[RevenueCatIds.OFFERING] ?: offerings.current ?: return null
        val monthly = current[RevenueCatIds.MONTHLY] ?: current.monthly
        val annual = current[RevenueCatIds.ANNUAL] ?: current.annual
        if (monthly == null && annual == null) return null

        val eligible = appStoreTrialEligibility(listOfNotNull(monthly, annual).map { it.storeProduct })
        packages = listOfNotNull(monthly, annual).associateBy { it.identifier }
        return Offering(
            monthly = monthly?.let { it.storeProduct.toPlan(it.identifier, PlanPeriod.Monthly, it.storeProduct.id in eligible) },
            annual = annual?.let { it.storeProduct.toPlan(it.identifier, PlanPeriod.Annual, it.storeProduct.id in eligible) },
        )
    }

    /**
     * Product ids eligible for their App Store free trial. Unknown counts as not eligible, so the
     * paywall never promises a trial the store won't give (RevenueCat's advice).
     */
    private suspend fun appStoreTrialEligibility(products: List<StoreProduct>): Set<String> {
        val withTrial = products.filter { it.appStoreFreeTrial() != null }
        if (withTrial.isEmpty()) return emptySet()
        return Purchases.sharedInstance.awaitTrialOrIntroPriceEligibility(withTrial)
            .filterValues { it == IntroEligibilityStatus.ELIGIBLE }
            .keys.mapTo(HashSet()) { it.id }
    }

    override suspend fun purchase(plan: Plan): PurchaseOutcome {
        if (!started) return PurchaseOutcome.Failed(BillingError.NotAvailable)
        val pkg = packages[plan.id] ?: return PurchaseOutcome.Failed(BillingError.NotAvailable)
        return try {
            val result = Purchases.sharedInstance.awaitPurchase(pkg)
            publish(result.customerInfo)
            PurchaseOutcome.Success(entitlement.value)
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) PurchaseOutcome.Cancelled else e.code.toOutcome()
        } catch (e: PurchasesException) {
            e.code.toOutcome()
        }
    }

    override suspend fun restore(): PurchaseOutcome = if (!started) {
        PurchaseOutcome.Failed(BillingError.NotAvailable)
    } else try {
        val info = Purchases.sharedInstance.awaitRestore()
        publish(info)
        val value = entitlement.value
        if (value.isFull) PurchaseOutcome.Success(value) else PurchaseOutcome.NothingToRestore
    } catch (e: PurchasesException) {
        e.code.toOutcome()
    }

    private companion object {
        val UNKNOWN_TIMEOUT = 5.seconds
        val EXPIRY_MARGIN = 5.seconds
    }
}
