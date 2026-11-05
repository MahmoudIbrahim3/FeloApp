package com.mahmoud.clipkid.core.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * The app's only backend client: PostgREST with the publishable key (role `anon`). No Auth,
 * Realtime, Storage or Functions plugin: v1 has no accounts and only reads the catalog and
 * inserts anonymous reports.
 */
internal fun createClipKidSupabase(url: String, publishableKey: String): SupabaseClient =
    createSupabaseClient(supabaseUrl = url, supabaseKey = publishableKey) {
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
        // App start waits for the first sync on a fresh install: never longer than this per request.
        requestTimeout = 10.seconds
        install(Postgrest)
    }
