package com.mahmoud.clipkid.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mahmoud.clipkid.feature.child.collection.CollectionRoute
import com.mahmoud.clipkid.feature.child.collection.CollectionEffect
import com.mahmoud.clipkid.feature.child.common.ChildTab
import com.mahmoud.clipkid.feature.child.done.DoneTodayEffect
import com.mahmoud.clipkid.feature.child.done.DoneTodayRoute
import com.mahmoud.clipkid.feature.child.favorites.FavoritesEffect
import com.mahmoud.clipkid.feature.child.favorites.FavoritesRoute
import com.mahmoud.clipkid.feature.child.gate.GateScreen
import com.mahmoud.clipkid.feature.child.home.HomeEffect
import com.mahmoud.clipkid.feature.child.home.HomeRoute
import com.mahmoud.clipkid.feature.child.player.PlayerEffect
import com.mahmoud.clipkid.feature.child.player.PlayerRoute
import com.mahmoud.clipkid.feature.child.upnext.UpNextEffect
import com.mahmoud.clipkid.feature.child.upnext.UpNextRoute
import org.koin.mp.KoinPlatform

@Composable
fun ClipKidNavHost() {
    val koin = remember { KoinPlatform.getKoin() }
    val childId by produceState<String?>(initialValue = null) {
        value = prepareChildApp(koin)
    }
    val start = childId ?: return

    val nav = rememberNavController()

    // Goes to Home and clears the stack, so back from Home never lands on a game screen.
    fun goHome(id: String) {
        nav.navigate(HomeDest(id)) { popUpTo(nav.graph.id) { inclusive = false } }
    }

    fun openTab(tab: ChildTab, id: String) {
        when (tab) {
            ChildTab.Home -> goHome(id)
            ChildTab.Favorites -> nav.navigate(FavoritesDest(id)) { popUpTo(nav.graph.id) { inclusive = false } }
        }
    }

    NavHost(navController = nav, startDestination = HomeDest(start)) {
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
                        HomeEffect.OpenGate -> nav.navigate(GateDest)
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
                onLock = { nav.navigate(GateDest) },
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
                        DoneTodayEffect.OpenGate -> nav.navigate(GateDest)
                    }
                },
            )
        }

        composable<GateDest> {
            GateScreen(onBack = { nav.popBackStack() })
        }
    }
}
