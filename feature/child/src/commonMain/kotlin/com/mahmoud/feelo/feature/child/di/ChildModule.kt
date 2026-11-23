package com.mahmoud.feelo.feature.child.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The child feature has no DI registrations of its own: its ViewModels are built in the
 * composition root (shared/navigation/ChildViewModels.kt) from use cases in core:domain.
 */
val childFeatureModule: Module = module { }
