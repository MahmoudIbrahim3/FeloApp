package com.mahmoud.clipkid.feature.parent.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * No DI registrations of its own: the parent ViewModels are built in the composition root
 * (shared) from use cases in core:domain.
 */
val parentFeatureModule: Module = module { }
