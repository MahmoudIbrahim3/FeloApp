package com.mahmoud.clipkid.feature.onboarding.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * No DI registrations of its own: [com.mahmoud.clipkid.feature.onboarding.OnboardingViewModel]
 * is built in the composition root (shared) from use cases in core:domain.
 */
val onboardingFeatureModule: Module = module { }
