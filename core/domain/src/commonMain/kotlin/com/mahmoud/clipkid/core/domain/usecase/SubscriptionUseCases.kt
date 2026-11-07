package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.repository.BillingRepository
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import com.mahmoud.clipkid.core.domain.repository.observeIsFull
import com.mahmoud.clipkid.core.model.Entitlement
import com.mahmoud.clipkid.core.model.SubscriptionLimits
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Free vs full for this device. The subscription sells parent features only; every video is free
 * for every child.
 *
 * - Full: every child profile is usable, up to [SubscriptionLimits.MAX_FULL_CHILDREN], and the
 *   Activity screen shows the whole week.
 * - Free: only the first profile (the oldest) is usable, and Activity shows today only. Extra
 *   profiles made while subscribed are kept with their settings and history, and come back when
 *   the subscription does. History is always recorded, so the week shows again on resubscribing.
 */
data class Access(
    val isFull: Boolean,
    /** Profile ids, oldest first. */
    val childIds: List<String>,
) {
    val unlockedChildIds: Set<String> =
        (if (isFull) childIds else childIds.take(SubscriptionLimits.MAX_FREE_CHILDREN)).toSet()

    val maxChildren: Int get() = SubscriptionLimits.maxChildren(isFull)

    val canAddChild: Boolean get() = childIds.size < maxChildren

    /** Free and at the free limit: "add child" leads to the paywall instead. */
    val addChildNeedsSubscription: Boolean get() = !isFull && !canAddChild

    /** Full and at the full limit: "add child" is hidden. */
    val atFullLimit: Boolean get() = isFull && !canAddChild

    fun isChildLocked(childId: String): Boolean = childId !in unlockedChildIds

    /** The 7-day chart and week totals on the Activity screen. Today's list is always free. */
    val showsWeeklyActivity: Boolean get() = isFull

    /** The child that child mode falls back to when the active one is locked. */
    val firstChildId: String? get() = childIds.firstOrNull()
}

class ObserveAccessUseCase(
    private val billing: BillingRepository,
    private val profiles: ProfileRepository,
) {
    operator fun invoke(): Flow<Access> =
        combine(billing.observeIsFull(), profiles.observeProfiles()) { isFull, children ->
            Access(isFull, children.map { it.id })
        }

    suspend fun current(): Access = invoke().first()
}

/**
 * When the app may talk to the store (RevenueCat). Apple's Kids rules: no device information to
 * third parties, even in sections for adults, unless the parent explicitly agreed.
 *
 * - Never before the parent's consent ([SettingsRepository.consentGiven]).
 * - Parent mode (dashboard, paywall): connects and asks for the latest status.
 * - Child mode: only to re-check a cached subscription whose period has ended, so a renewal
 *   keeps it on and a cancellation turns it off. Otherwise child mode reads the on-device cache.
 */
class ConnectBillingUseCase(
    private val billing: BillingRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    /** Parent mode. Returns false (and does nothing) when the parent has not agreed yet. */
    suspend fun forParent(): Boolean {
        if (!settings.consentGiven.first()) return false
        connect()
        return true
    }

    /** The paywall's consent card: records the parent's agreement, then connects. */
    suspend fun agreeAndConnect() {
        settings.setConsentGiven(true)
        connect()
    }

    /** App start and back to the foreground: only when the cached subscription has ended. */
    suspend fun ifSubscriptionEnded() {
        val current = billing.entitlement.first { it != Entitlement.Unknown }
        val endsAt = (current as? Entitlement.Full)?.expiresAt ?: return
        if (endsAt > clock.now()) return
        if (!settings.consentGiven.first()) return
        connect()
    }

    private suspend fun connect() {
        // start() loads the status itself; an already started connection is asked again.
        if (billing.isStarted) billing.refresh() else billing.start()
    }
}
