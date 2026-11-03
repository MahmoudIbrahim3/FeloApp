package com.mahmoud.clipkid.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mahmoud.clipkid.gallery.ComponentGallery
import kotlinx.serialization.Serializable

/** Temporary start destination until onboarding exists. */
@Serializable
data object GalleryRoute

@Composable
fun ClipKidNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = GalleryRoute) {
        composable<GalleryRoute> { ComponentGallery() }
    }
}
