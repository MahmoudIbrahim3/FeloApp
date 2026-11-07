package com.mahmoud.clipkid.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.mahmoud.clipkid.core.domain.parent.ParentSession
import com.mahmoud.clipkid.feature.onboarding.OnboardingEffect
import com.mahmoud.clipkid.feature.onboarding.OnboardingMode
import com.mahmoud.clipkid.feature.onboarding.OnboardingRoute
import com.mahmoud.clipkid.feature.onboarding.OnboardingStep
import com.mahmoud.clipkid.feature.parent.activity.ActivityEffect
import com.mahmoud.clipkid.feature.parent.activity.ActivityRoute
import com.mahmoud.clipkid.feature.parent.dashboard.DashboardEffect
import com.mahmoud.clipkid.feature.parent.dashboard.DashboardRoute
import com.mahmoud.clipkid.feature.parent.gate.GateEffect
import com.mahmoud.clipkid.feature.parent.gate.GateRoute
import com.mahmoud.clipkid.feature.parent.grant.GrantEffect
import com.mahmoud.clipkid.feature.parent.grant.GrantRoute
import com.mahmoud.clipkid.feature.parent.paywall.PaywallEffect
import com.mahmoud.clipkid.feature.parent.paywall.PaywallRoute

/** True for every destination inside [ParentGraph] (dashboard, activity, add child, ...). */
internal fun NavBackStackEntry.isInParentMode(): Boolean =
    destination.hierarchy.any { it.hasRoute<ParentGraph>() }

// region Onboarding

/** Welcome → PIN → child → collections. The steps share one ViewModel scoped to the graph. */
internal fun NavGraphBuilder.onboardingGraph(nav: NavHostController, onFinished: (childId: String) -> Unit) {
    navigation<OnboardingGraph>(startDestination = OnboardingWelcomeDest) {
        composable<OnboardingWelcomeDest> { entry -> OnboardingStepHost(nav, entry, OnboardingStep.Welcome, onFinished) }
        composable<OnboardingPinDest> { entry -> OnboardingStepHost(nav, entry, OnboardingStep.Pin, onFinished) }
        composable<OnboardingChildDest> { entry -> OnboardingStepHost(nav, entry, OnboardingStep.Child, onFinished) }
        composable<OnboardingCollectionsDest> { entry -> OnboardingStepHost(nav, entry, OnboardingStep.Collections, onFinished) }
    }
}

@Composable
private fun OnboardingStepHost(
    nav: NavHostController,
    entry: NavBackStackEntry,
    step: OnboardingStep,
    onFinished: (String) -> Unit,
) {
    val graphEntry = remember(entry) { nav.getBackStackEntry<OnboardingGraph>() }
    OnboardingRoute(
        step = step,
        viewModel = rememberOnboardingViewModel(graphEntry, OnboardingMode.FirstRun),
        onEffect = { effect ->
            when (effect) {
                OnboardingEffect.OpenPin -> nav.navigate(OnboardingPinDest) { launchSingleTop = true }
                OnboardingEffect.OpenChild -> nav.navigate(OnboardingChildDest) { launchSingleTop = true }
                OnboardingEffect.OpenCollections -> nav.navigate(OnboardingCollectionsDest) { launchSingleTop = true }
                OnboardingEffect.Back -> nav.popBackStack()
                is OnboardingEffect.Finished -> onFinished(effect.childId)
            }
        },
    )
}

// endregion

// region Parental gate

/** The one gate route. Closing it returns to the caller; passing it calls [onPassed]. */
internal fun NavGraphBuilder.parentalGate(nav: NavHostController, onPassed: (GateDest) -> Unit) {
    composable<GateDest> { entry ->
        val dest = entry.toRoute<GateDest>()
        GateRoute(
            viewModel = rememberGateViewModel(),
            onEffect = { effect ->
                when (effect) {
                    GateEffect.Passed -> onPassed(dest)
                    GateEffect.Closed -> nav.popBackStack()
                }
            },
        )
    }
}

// endregion

// region Parent mode

internal fun NavGraphBuilder.parentGraph(nav: NavHostController, session: ParentSession) {
    navigation<ParentGraph>(startDestination = DashboardDest) {
        composable<DashboardDest> {
            ParentScreen(session) {
                DashboardRoute(
                    viewModel = rememberDashboardViewModel(),
                    onEffect = { effect ->
                        when (effect) {
                            DashboardEffect.OpenAddChild -> nav.navigate(AddChildGraph) { launchSingleTop = true }
                            is DashboardEffect.OpenActivity -> nav.navigate(ActivityDest(effect.childId)) { launchSingleTop = true }
                            DashboardEffect.OpenSubscription -> nav.navigate(SubscriptionDest) { launchSingleTop = true }
                            // ParentModeGuard sees the lock and goes back to child Home.
                            DashboardEffect.Lock -> session.lock()
                        }
                    },
                )
            }
        }
        composable<ActivityDest> { entry ->
            val dest = entry.toRoute<ActivityDest>()
            ParentScreen(session) {
                ActivityRoute(
                    viewModel = rememberActivityViewModel(dest.childId),
                    onEffect = { effect ->
                        when (effect) {
                            ActivityEffect.Back -> nav.popBackStack()
                            ActivityEffect.OpenSubscription -> nav.navigate(SubscriptionDest) { launchSingleTop = true }
                        }
                    },
                )
            }
        }
        composable<SubscriptionDest> {
            val uriHandler = LocalUriHandler.current
            ParentScreen(session) {
                PaywallRoute(
                    viewModel = rememberPaywallViewModel(),
                    onEffect = { effect ->
                        when (effect) {
                            PaywallEffect.Close -> nav.popBackStack()
                            // Store, terms, privacy: the parent is already past the PIN here.
                            is PaywallEffect.OpenUrl -> uriHandler.openUri(effect.url)
                        }
                    },
                )
            }
        }
        composable<GrantDest> { entry ->
            val dest = entry.toRoute<GrantDest>()
            ParentScreen(session) {
                GrantRoute(
                    viewModel = rememberGrantViewModel(dest.childId),
                    onEffect = { effect ->
                        when (effect) {
                            GrantEffect.Done -> session.lock()
                        }
                    },
                )
            }
        }
        navigation<AddChildGraph>(startDestination = AddChildDetailsDest) {
            composable<AddChildDetailsDest> { entry -> AddChildStepHost(nav, entry, session, OnboardingStep.Child) }
            composable<AddChildCollectionsDest> { entry -> AddChildStepHost(nav, entry, session, OnboardingStep.Collections) }
        }
    }
}

/** "+" on the dashboard: the onboarding child and collections steps, without the PIN. */
@Composable
private fun AddChildStepHost(nav: NavHostController, entry: NavBackStackEntry, session: ParentSession, step: OnboardingStep) {
    val graphEntry = remember(entry) { nav.getBackStackEntry<AddChildGraph>() }
    ParentScreen(session) {
        OnboardingRoute(
            step = step,
            viewModel = rememberOnboardingViewModel(graphEntry, OnboardingMode.AddChild),
            onEffect = { effect ->
                when (effect) {
                    OnboardingEffect.OpenCollections -> nav.navigate(AddChildCollectionsDest) { launchSingleTop = true }
                    OnboardingEffect.Back -> nav.popBackStack()
                    // The new child is now the active one; the dashboard shows it.
                    is OnboardingEffect.Finished -> nav.popBackStack<AddChildGraph>(inclusive = true)
                    OnboardingEffect.OpenPin, OnboardingEffect.OpenChild -> Unit
                }
            },
        )
    }
}

/**
 * Wraps every parent screen: shows nothing unless the session is open (e.g. a parent screen
 * restored after the process was killed), and counts every touch as activity for the
 * 2-minute auto-lock.
 */
@Composable
private fun ParentScreen(session: ParentSession, content: @Composable () -> Unit) {
    val unlocked by session.isUnlocked.collectAsState()
    if (!unlocked) return
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(session) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        session.touch()
                    }
                }
            },
    ) { content() }
}

/**
 * Keeps parent mode closed whenever the parent is not in it:
 * - leaving the parent graph by any route (system back, a button) locks the session;
 * - a locked session (lock button, grant done, 2 minutes idle, app in background) while a
 *   parent screen is shown goes to child Home, clearing the parent screens from the stack.
 */
@Composable
internal fun ParentModeGuard(nav: NavHostController, session: ParentSession, onLeave: () -> Unit) {
    LaunchedEffect(nav) {
        var wasInParent = false
        nav.currentBackStackEntryFlow.collect { entry ->
            val inParent = entry.isInParentMode()
            if (wasInParent && !inParent) {
                session.lock()
                onLeave()
            }
            wasInParent = inParent
        }
    }
    LaunchedEffect(session) {
        session.isUnlocked.collect { unlocked ->
            if (!unlocked && nav.currentBackStackEntry?.isInParentMode() == true) onLeave()
        }
    }
}

// endregion
