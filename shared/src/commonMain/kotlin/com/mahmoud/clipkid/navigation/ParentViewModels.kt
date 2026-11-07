package com.mahmoud.clipkid.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mahmoud.clipkid.core.domain.parent.CreateAdultChallengeUseCase
import com.mahmoud.clipkid.core.domain.parent.ParentSession
import com.mahmoud.clipkid.core.domain.parent.ResetPinUseCase
import com.mahmoud.clipkid.core.domain.parent.UnlockWithPinUseCase
import com.mahmoud.clipkid.core.domain.repository.BillingRepository
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import com.mahmoud.clipkid.core.domain.usecase.AddChildUseCase
import com.mahmoud.clipkid.core.domain.usecase.CompleteOnboardingUseCase
import com.mahmoud.clipkid.core.domain.usecase.ConnectBillingUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetCollectionsForAgeUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetWeeklyActivityUseCase
import com.mahmoud.clipkid.core.domain.usecase.GrantExtraVideosUseCase
import com.mahmoud.clipkid.core.domain.usecase.HideVideoUseCase
import com.mahmoud.clipkid.core.domain.usecase.ObserveAccessUseCase
import com.mahmoud.clipkid.core.domain.usecase.UpdateChildSettingsUseCase
import com.mahmoud.clipkid.feature.onboarding.OnboardingMode
import com.mahmoud.clipkid.feature.onboarding.OnboardingViewModel
import com.mahmoud.clipkid.feature.parent.activity.ActivityViewModel
import com.mahmoud.clipkid.feature.parent.dashboard.DashboardViewModel
import com.mahmoud.clipkid.feature.parent.gate.GateViewModel
import com.mahmoud.clipkid.feature.parent.grant.GrantViewModel
import com.mahmoud.clipkid.feature.parent.paywall.PaywallViewModel
import kotlin.time.Clock
import org.koin.mp.KoinPlatform

// Composition root for onboarding and parent mode, same pattern as ChildViewModels.kt.

/** Scoped to [owner] (the onboarding or add-child graph entry), so all steps share it. */
@Composable
fun rememberOnboardingViewModel(owner: ViewModelStoreOwner, mode: OnboardingMode): OnboardingViewModel {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        viewModelStoreOwner = owner,
        key = "onboarding/${mode.name}",
        factory = viewModelFactory {
            initializer {
                OnboardingViewModel(
                    mode = mode,
                    getCollectionsForAge = koin.get<GetCollectionsForAgeUseCase>(),
                    completeOnboarding = koin.get<CompleteOnboardingUseCase>(),
                    addChild = koin.get<AddChildUseCase>(),
                )
            }
        },
    )
}

@Composable
fun rememberGateViewModel(): GateViewModel {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        key = "gate",
        factory = viewModelFactory {
            initializer {
                GateViewModel(
                    unlockWithPin = koin.get<UnlockWithPinUseCase>(),
                    resetPin = koin.get<ResetPinUseCase>(),
                    createChallenge = koin.get<CreateAdultChallengeUseCase>(),
                    settings = koin.get<SettingsRepository>(),
                    clock = koin.get<Clock>(),
                )
            }
        },
    )
}

@Composable
fun rememberDashboardViewModel(): DashboardViewModel {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        key = "dashboard",
        factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    profiles = koin.get<ProfileRepository>(),
                    settings = koin.get<SettingsRepository>(),
                    getQuota = koin.get<GetDailyQuotaUseCase>(),
                    getCollectionsForAge = koin.get<GetCollectionsForAgeUseCase>(),
                    updateChild = koin.get<UpdateChildSettingsUseCase>(),
                    grantExtras = koin.get<GrantExtraVideosUseCase>(),
                    observeAccess = koin.get<ObserveAccessUseCase>(),
                    connectBilling = koin.get<ConnectBillingUseCase>(),
                )
            }
        },
    )
}

@Composable
fun rememberPaywallViewModel(): PaywallViewModel {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        key = "paywall",
        factory = viewModelFactory {
            initializer {
                PaywallViewModel(
                    billing = koin.get<BillingRepository>(),
                    connectBilling = koin.get<ConnectBillingUseCase>(),
                    session = koin.get<ParentSession>(),
                )
            }
        },
    )
}

@Composable
fun rememberActivityViewModel(childId: String): ActivityViewModel {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        key = "activity/$childId",
        factory = viewModelFactory {
            initializer {
                ActivityViewModel(
                    childId = childId,
                    getActivity = koin.get<GetWeeklyActivityUseCase>(),
                    hideVideo = koin.get<HideVideoUseCase>(),
                    observeAccess = koin.get<ObserveAccessUseCase>(),
                )
            }
        },
    )
}

@Composable
fun rememberGrantViewModel(childId: String): GrantViewModel {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        key = "grant/$childId",
        factory = viewModelFactory {
            initializer {
                GrantViewModel(childId, koin.get<ProfileRepository>(), koin.get<GrantExtraVideosUseCase>())
            }
        },
    )
}
