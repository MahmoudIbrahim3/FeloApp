package com.mahmoud.clipkid.feature.child.testing

import com.mahmoud.clipkid.core.domain.testing.Catalog
import com.mahmoud.clipkid.core.domain.testing.FakeCatalogRepository
import com.mahmoud.clipkid.core.domain.testing.FakeFavoritesRepository
import com.mahmoud.clipkid.core.domain.testing.FakeHiddenVideosRepository
import com.mahmoud.clipkid.core.domain.testing.FakeProfileRepository
import com.mahmoud.clipkid.core.domain.testing.FakeWatchHistoryRepository
import com.mahmoud.clipkid.core.domain.testing.KUWAIT
import com.mahmoud.clipkid.core.domain.testing.SequentialIdGenerator
import com.mahmoud.clipkid.core.domain.testing.TestClock
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetHomeFeedUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetUpNextUseCase
import com.mahmoud.clipkid.core.domain.usecase.StartWatchUseCase
import com.mahmoud.clipkid.core.domain.usecase.ToggleFavoriteUseCase
import com.mahmoud.clipkid.core.domain.usecase.UpdateWatchProgressUseCase
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.DailyLimit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Runs a ViewModel test with [Dispatchers.Main] replaced by the test scheduler, so the
 * `viewModelScope` work runs when the test advances time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runViewModelTest(block: suspend TestScope.() -> Unit) {
    val dispatcher: CoroutineDispatcher = StandardTestDispatcher()
    Dispatchers.setMain(dispatcher)
    try {
        runTest(dispatcher) { block() }
    } finally {
        Dispatchers.resetMain()
    }
}

/** Fake repositories and the real use cases on top of them, at Wed 4 Nov 2026, 17:00 in Kuwait. */
class ChildFixture(scope: TestScope, profile: ChildProfile = child(DailyLimit.Videos(3))) {
    val origin = kuwait(2026, 11, 4, 17, 0)
    val clock = TestClock(scope.testScheduler, origin)
    val catalog = FakeCatalogRepository(Catalog.collections, Catalog.videos)
    val profiles = FakeProfileRepository(profile)
    val history = FakeWatchHistoryRepository()
    val hidden = FakeHiddenVideosRepository()
    val favorites = FakeFavoritesRepository()
    val ids = SequentialIdGenerator()

    val quota = GetDailyQuotaUseCase(profiles, history, clock, KUWAIT)
    val homeFeed = GetHomeFeedUseCase(catalog, profiles, history, hidden, clock, KUWAIT)
    val startWatch = StartWatchUseCase(profiles, history, ids, clock, KUWAIT)
    val updateProgress = UpdateWatchProgressUseCase(history)
    val toggleFavorite = ToggleFavoriteUseCase(favorites, clock)
    val upNext = GetUpNextUseCase(catalog, profiles, history, hidden, quota, clock, KUWAIT)
}
