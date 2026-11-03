package com.mahmoud.clipkid.di

import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class KoinGraphTest {

    @Test
    fun everyGradleModuleContributesOneKoinModule() {
        // designsystem, domain, data, player, billing, onboarding, child, parent
        assertEquals(8, appModules.size)
        assertEquals(appModules.size, appModules.toSet().size)
    }

    @Test
    fun graphLoads() {
        val app = koinApplication { modules(appModules) }
        try {
            // Resolving every definition catches missing bindings once modules gain content.
            app.koin.getAll<Any>()
        } finally {
            app.close()
        }
    }
}
