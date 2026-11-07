package com.mahmoud.clipkid.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mahmoud.clipkid.core.domain.parent.ParentSession
import com.mahmoud.clipkid.core.domain.repository.BillingRepository
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import com.mahmoud.clipkid.core.domain.usecase.ObserveAccessUseCase
import com.mahmoud.clipkid.feature.child.collection.CollectionEffect
import com.mahmoud.clipkid.feature.child.collection.CollectionRoute
import com.mahmoud.clipkid.feature.child.common.ChildTab
import com.mahmoud.clipkid.feature.child.done.DoneTodayEffect
import com.mahmoud.clipkid.feature.child.done.DoneTodayRoute
import com.mahmoud.clipkid.feature.child.favorites.FavoritesEffect
import com.mahmoud.clipkid.feature.child.favorites.FavoritesRoute
import com.mahmoud.clipkid.feature.child.home.HomeEffect
import com.mahmoud.clipkid.feature.child.home.HomeRoute
import com.mahmoud.clipkid.feature.child.player.PlayerEffect
import com.mahmoud.clipkid.feature.child.player.PlayerRoute
import com.mahmoud.clipkid.feature.child.upnext.UpNextEffect
import com.mahmoud.clipkid.feature.child.upnext.UpNextRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * The whole app's navigation: onboarding (first launch), child mode, the parental gate and
 * parent mode. Parent screens are reachable only through the gate, and leaving parent mode
 * (back, lock, 2 minutes idle, app in background) always lands on child Home with nothing
 * below it, so a child can never go back into a parent screen.
 */
@Composable
fun ClipKidNavHost() {
    val koin = remember { KoinPlatform.getKoin() }
    val appStart by produceState<AppStart?>(initialValue = null) {
        value = prepareApp(koin)
    }
    val start = appStart ?: return

    val nav = rememberNavController()
    val session = remember { koin.get<ParentSession>() }
    val settings = remember { koin.get<SettingsRepository>() }
    val billing = remember { koin.get<BillingRepository>() }
    val observeAccess = remember { koin.get<ObserveAccessUseCase>() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    // Goes to Home and clears the stack, so back from Home never lands on a game screen.
    fun goHome(id: String) {
        nav.navigate(HomeDest(id)) { popUpTo(nav.graph.id) { inclusive = false } }
    }

    // Child mode for the active child (the dashboard may have switched it).
    fun showChildHome() {
        scope.launch {
            val id = settings.activeChildId.first() ?: return@launch
            val top = nav.currentBackStackEntry
            val atHome = top != null && top.destination.hasRoute<HomeDest>() && top.toRoute<HomeDest>().childId == id
            if (!atHome) goHome(id)
        }
    }

    // The parent passed the gate: replace the gate with what its caller asked for.
    fun onGatePassed(dest: GateDest) {
        fun replaceGateWith(route: Any) = nav.navigate(route) { popUpTo<GateDest> { inclusive = true } }
        when (dest.purpose) {
            GatePurpose.ParentDashboard -> {
                session.unlock()
                replaceGateWith(ParentGraph)
            }
            GatePurpose.GrantExtras -> {
                val childId = dest.childId
                if (childId == null) {
                    nav.popBackStack()
                } else {
                    session.unlock()
                    replaceGateWith(GrantDest(childId))
                }
            }
            GatePurpose.ExternalLink -> {
                nav.popBackStack()
                dest.url?.let(uriHandler::openUri)
            }
        }
    }

    ParentModeGuard(nav, session, onLeave = ::showChildHome)
    // Leaving the app (home button, app switcher) closes parent mode, except while a store
    // purchase sheet started from the paywall is open (it can send the app to the background).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { session.onAppStopped() }
    // Back in the foreground: renewals, cancellations and refunds show up at once.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { scope.launch { billing.refresh() } }

    // Free again with more than one child: child mode moves to the first child. The other
    // profiles keep their settings and history and come back with the subscription.
    LaunchedEffect(observeAccess) {
        observeAccess().collect { access ->
            val active = settings.activeChildId.first() ?: return@collect
            val first = access.firstChildId ?: return@collect
            if (access.isChildLocked(active) && !session.isUnlocked.value) {
                settings.setActiveChildId(first)
                val inChildMode = nav.currentBackStackEntry?.let { !it.isInParentMode() && !it.destination.hasRoute<GateDest>() } == true
                if (inChildMode) goHome(first)
            }
        }
    }

    val startDestination: Any = when (start) {
        AppStart.Onboarding -> OnboardingGraph
        is AppStart.Child -> HomeDest(start.childId)
    }

    fun openTab(tab: ChildTab, id: String) {
        when (tab) {
            ChildTab.Home -> goHome(id)
            ChildTab.Favorites -> nav.navigate(FavoritesDest(id)) { popUpTo(nav.graph.id) { inclusive = false } }
        }
    }

    NavHost(navController = nav, startDestination = startDestination) {
        onboardingGraph(nav, onFinished = { childId ->
            nav.navigate(HomeDest(childId)) { popUpTo<OnboardingGraph> { inclusive = true } }
        })
        parentalGate(nav, onPassed = ::onGatePassed)
        parentGraph(nav, session)

        composable<HomeDest> { entry ->
            val dest = entry.toRoute<HomeDest>()
            HomeRoute(
                viewModel = rememberHomeViewModel(dest.childId),
                onEffect = { effect ->
                    when (effect) {
                        is HomeEffect.OpenVideo -> nav.navigate(
                            PlayerDest(dest.childId, effect.youtubeId, effect.source),
                        )
                        is HomeEffect.OpenCollection -> nav.navigate(CollectionDest(dest.childId, effect.collectionId))
                        HomeEffect.OpenGate -> nav.navigate(GateDest(GatePurpose.ParentDashboard))
                    }
                },
                onSelectTab = { tab -> openTab(tab, dest.childId) },
            )
        }

        composable<FavoritesDest> { entry ->
            val dest = entry.toRoute<FavoritesDest>()
            FavoritesRoute(
                viewModel = rememberFavoritesViewModel(dest.childId),
                onEffect = { effect ->
                    when (effect) {
                        is FavoritesEffect.OpenVideo -> nav.navigate(
                            PlayerDest(dest.childId, effect.youtubeId, effect.source),
                        )
                    }
                },
                onSelectTab = { tab -> openTab(tab, dest.childId) },
                onLock = { nav.navigate(GateDest(GatePurpose.ParentDashboard)) },
            )
        }

        composable<CollectionDest> { entry ->
            val dest = entry.toRoute<CollectionDest>()
            CollectionRoute(
                viewModel = rememberCollectionViewModel(dest.childId, dest.collectionId),
                onEffect = { effect ->
                    when (effect) {
                        is CollectionEffect.OpenVideo -> nav.navigate(
                            PlayerDest(dest.childId, effect.youtubeId, effect.source),
                        )
                        CollectionEffect.Back -> nav.popBackStack()
                    }
                },
            )
        }

        composable<PlayerDest> { entry ->
            val dest = entry.toRoute<PlayerDest>()
            PlayerRoute(
                viewModel = rememberPlayerViewModel(dest.childId, dest.youtubeId, dest.source),
                onEffect = { effect ->
                    when (effect) {
                        PlayerEffect.OpenDoneToday -> nav.navigate(DoneTodayDest(dest.childId)) {
                            popUpTo<PlayerDest> { inclusive = true }
                        }
                        is PlayerEffect.OpenUpNext -> nav.navigate(UpNextDest(dest.childId, effect.finishedYoutubeId)) {
                            popUpTo<PlayerDest> { inclusive = true }
                        }
                        PlayerEffect.Back -> nav.popBackStack()
                        // Logo, title, "Watch on YouTube", end screen, ads: only past the gate.
                        is PlayerEffect.OpenExternalLink -> nav.navigate(
                            GateDest(GatePurpose.ExternalLink, childId = dest.childId, url = effect.url),
                        )
                    }
                },
            )
        }

        composable<UpNextDest> { entry ->
            val dest = entry.toRoute<UpNextDest>()
            UpNextRoute(
                viewModel = rememberUpNextViewModel(dest.childId, dest.finishedYoutubeId),
                onEffect = { effect ->
                    when (effect) {
                        UpNextEffect.OpenDoneToday -> nav.navigate(DoneTodayDest(dest.childId)) {
                            popUpTo<UpNextDest> { inclusive = true }
                        }
                        is UpNextEffect.OpenPlayer -> nav.navigate(
                            PlayerDest(dest.childId, effect.youtubeId, effect.source),
                        ) {
                            popUpTo<UpNextDest> { inclusive = true }
                        }
                        UpNextEffect.Home -> goHome(dest.childId)
                    }
                },
            )
        }

        composable<DoneTodayDest> { entry ->
            val dest = entry.toRoute<DoneTodayDest>()
            DoneTodayRoute(
                viewModel = rememberDoneTodayViewModel(dest.childId),
                onEffect = { effect ->
                    when (effect) {
                        DoneTodayEffect.Home -> goHome(dest.childId)
                        DoneTodayEffect.OpenGate -> nav.navigate(GateDest(GatePurpose.GrantExtras, childId = dest.childId))
                    }
                },
            )
        }
    }
}
