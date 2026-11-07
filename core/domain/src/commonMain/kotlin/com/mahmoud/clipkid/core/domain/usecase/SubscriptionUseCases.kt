package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.repository.BillingRepository
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.observeIsFull
import com.mahmoud.clipkid.core.model.SubscriptionLimits
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
