package com.mahmoud.clipkid.core.data.di

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataConfigTest {

    @Test
    fun supabaseIsUsedOnlyWithAUrlAndAKey() {
        assertTrue(DataConfig(useFakeCatalog = false, supabaseUrl = "https://x.supabase.co", supabasePublishableKey = "sb_publishable_x").usesSupabase)
        assertFalse(DataConfig(useFakeCatalog = false, supabaseUrl = "", supabasePublishableKey = "sb_publishable_x").usesSupabase)
        assertFalse(DataConfig(useFakeCatalog = false, supabaseUrl = "https://x.supabase.co", supabasePublishableKey = " ").usesSupabase)
    }

    @Test
    fun fakesWinWhenAskedFor() {
        assertFalse(DataConfig(useFakeCatalog = true, supabaseUrl = "https://x.supabase.co", supabasePublishableKey = "k").usesSupabase)
    }

    @Test
    fun theRealCatalogIsTheDefault() {
        assertFalse(DataConfig().useFakeCatalog)
    }
}
