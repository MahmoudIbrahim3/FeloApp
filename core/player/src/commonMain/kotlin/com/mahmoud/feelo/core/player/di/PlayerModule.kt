package com.mahmoud.feelo.core.player.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The YouTube player needs no DI bindings: [com.mahmoud.feelo.core.player.YouTubePlayer] is a
 * composable and each screen creates its own [com.mahmoud.feelo.core.player.YouTubePlayerState].
 */
val playerModule: Module = module { }
