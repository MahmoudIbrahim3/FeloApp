package com.mahmoud.feelo.navigation

import com.mahmoud.feelo.core.model.WatchSource
import kotlinx.serialization.Serializable

// Type-safe routes. Names end in "Dest" so they don't clash with the feature screens (HomeRoute, ...).

// region Child mode

@Serializable
data class HomeDest(val childId: String)

@Serializable
data class FavoritesDest(val childId: String)

@Serializable
data class CollectionDest(val childId: String, val collectionId: String)

@Serializable
data class PlayerDest(val childId: String, val youtubeId: String, val source: WatchSource)

@Serializable
data class UpNextDest(val childId: String, val finishedYoutubeId: String)

@Serializable
data class DoneTodayDest(val childId: String)

// endregion

// region Onboarding (first launch only). One ViewModel is shared by the steps of the graph.

@Serializable
data object OnboardingGraph

@Serializable
data object OnboardingWelcomeDest

@Serializable
data object OnboardingPinDest

/** First run only, right after the PIN: the parent's consent to YouTube's player and the store. */
@Serializable
data object OnboardingConsentDest

@Serializable
data object OnboardingChildDest

@Serializable
data object OnboardingCollectionsDest

// endregion

// region Parental gate

/** What the caller wants to do once the parent passes the gate. */
@Serializable
enum class GatePurpose {
    /** Lock button on child Home / Favorites. */
    ParentDashboard,

    /** "عايز تكمّل؟" on DoneToday: +1 / +3 for [GateDest.childId]. */
    GrantExtras,

    /** Any link that leaves the app, e.g. "Watch on YouTube" (player phase): [GateDest.url]. */
    ExternalLink,
}

/**
 * The one reusable parental gate. It carries the caller's [purpose]; when the parent passes,
 * the gate is replaced by what the purpose asks for (see `onGatePassed` in FeeloNavHost),
 * so the gate itself never stays on the back stack.
 */
@Serializable
data class GateDest(
    val purpose: GatePurpose,
    val childId: String? = null,
    val url: String? = null,
)

// endregion

// region Parent mode: every destination below is inside ParentGraph and reachable only via the gate.

@Serializable
data object ParentGraph

@Serializable
data object DashboardDest

@Serializable
data class ActivityDest(val childId: String)

/** The paywall (Paywall.png). */
@Serializable
data object SubscriptionDest

@Serializable
data class GrantDest(val childId: String)

/** "عن التطبيق والخصوصية": summary, policies, terms, contact (compliance A14). */
@Serializable
data object AboutDest

@Serializable
data object AddChildGraph

@Serializable
data object AddChildDetailsDest

@Serializable
data object AddChildCollectionsDest

// endregion
