package com.mahmoud.clipkid.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mahmoud.clipkid.core.domain.repository.BillingRepository
import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.FavoritesRepository
import com.mahmoud.clipkid.core.domain.repository.NetworkMonitor
import com.mahmoud.clipkid.core.domain.usecase.GetCollectionUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetFavoritesUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetHomeFeedUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetResumePositionUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetUpNextUseCase
import com.mahmoud.clipkid.core.domain.usecase.ReportVideoUseCase
import com.mahmoud.clipkid.core.domain.usecase.StartWatchUseCase
import com.mahmoud.clipkid.core.domain.usecase.ToggleFavoriteUseCase
import com.mahmoud.clipkid.core.domain.usecase.UpdateWatchProgressUseCase
import com.mahmoud.clipkid.core.model.WatchSource
import com.mahmoud.clipkid.feature.child.collection.CollectionViewModel
import com.mahmoud.clipkid.feature.child.done.DoneTodayViewModel
import com.mahmoud.clipkid.feature.child.favorites.FavoritesViewModel
import com.mahmoud.clipkid.feature.child.home.HomeViewModel
import com.mahmoud.clipkid.feature.child.player.PlayerViewModel
import com.mahmoud.clipkid.feature.child.upnext.UpNextViewModel
import org.koin.mp.KoinPlatform

// The composition root: ViewModels are built here from Koin-provided use cases, so the feature
// modules never depend on each other or on the DI graph. Keys include the child and screen args
// so each destination keeps its own ViewModel.

@Composable
fun rememberHomeViewModel(childId: String): HomeViewModel {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        key = "home/$childId",
        factory = viewModelFactory {
            initializer {
                HomeViewModel(childId, koin.get<GetHomeFeedUseCase>(), koin.get<GetDailyQuotaUseCase>())
            }
        },
    )
}

@Composable
fun rememberFavoritesViewModel(childId: String): FavoritesViewModel {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        key = "favorites/$childId",
        factory = viewModelFactory {
            initializer {
                FavoritesViewModel(childId, koin.get<GetFavoritesUseCase>(), koin.get<ToggleFavoriteUseCase>())
            }
        },
    )
}

@Composable
fun rememberCollectionViewModel(childId: String, collectionId: String): CollectionViewModel {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        key = "collection/$childId/$collectionId",
        factory = viewModelFactory {
            initializer {
                CollectionViewModel(childId, collectionId, koin.get<GetCollectionUseCase>())
            }
        },
    )
}

@Composable
fun rememberPlayerViewModel(childId: String, youtubeId: String, source: WatchSource): PlayerViewModel {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        key = "player/$childId/$youtubeId/${source.name}",
        factory = viewModelFactory {
            initializer {
                PlayerViewModel(
                    childId = childId,
                    youtubeId = youtubeId,
                    source = source,
                    // Keeps the playback's event id and progress across the gate, rotation and process death.
                    savedState = createSavedStateHandle(),
                    startWatch = koin.get<StartWatchUseCase>(),
                    updateProgress = koin.get<UpdateWatchProgressUseCase>(),
                    getResumePosition = koin.get<GetResumePositionUseCase>(),
                    reportVideo = koin.get<ReportVideoUseCase>(),
                    network = koin.get<NetworkMonitor>(),
                    toggleFavorite = koin.get<ToggleFavoriteUseCase>(),
                    catalog = koin.get<CatalogRepository>(),
                    favorites = koin.get<FavoritesRepository>(),
                    billing = koin.get<BillingRepository>(),
                )
            }
        },
    )
}

@Composable
fun rememberUpNextViewModel(childId: String, finishedYoutubeId: String): UpNextViewModel {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        key = "upnext/$childId/$finishedYoutubeId",
        factory = viewModelFactory {
            initializer {
                UpNextViewModel(
                    childId = childId,
                    finishedYoutubeId = finishedYoutubeId,
                    getUpNext = koin.get<GetUpNextUseCase>(),
                    getQuota = koin.get<GetDailyQuotaUseCase>(),
                    catalog = koin.get<CatalogRepository>(),
                )
            }
        },
    )
}

@Composable
fun rememberDoneTodayViewModel(childId: String): DoneTodayViewModel {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        key = "done/$childId",
        factory = viewModelFactory {
            initializer { DoneTodayViewModel(childId, koin.get<GetDailyQuotaUseCase>()) }
        },
    )
}
