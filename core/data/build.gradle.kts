import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.Properties

plugins {
    alias(libs.plugins.clipkid.kmp.library)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    // Supabase URL + publishable key as build-time constants (from local.properties or env).
    alias(libs.plugins.buildKonfig)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(libs.koin.core)
            // Local cache and history (Room 3 + bundled SQLite on both platforms).
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            // Small settings (onboarding done, active child, catalog sync state).
            implementation(libs.androidx.datastore.preferences.core)
            // JSON columns (tags, age bands, cover) in the Room cache, and Supabase rows.
            implementation(libs.kotlinx.serialization.json)
            // Catalog sync and anonymous reports through PostgREST (no Auth/Realtime/Storage).
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.postgrest)
        }
        androidMain.dependencies {
            // androidContext() for Room, DataStore, the Keystore-backed SecureStore and the NetworkMonitor.
            implementation(libs.koin.android)
            // HTTP engine for supabase-kt (Ktor).
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach { add(it, libs.androidx.room.compiler) }
}

// Supabase config. Never committed: put it in local.properties (gitignored)
//   supabase.url=https://<project-ref>.supabase.co
//   supabase.publishableKey=sb_publishable_...
// or set CLIPKID_SUPABASE_URL / CLIPKID_SUPABASE_PUBLISHABLE_KEY (CI). Empty values still build;
// the app then uses the fake catalog and logs a warning.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun supabaseSetting(property: String, env: String): String =
    providers.environmentVariable(env).orNull?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(property)?.trim().orEmpty()

buildkonfig {
    packageName = "com.mahmoud.clipkid.core.data.config"
    objectName = "SupabaseBuildConfig"
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "SUPABASE_URL", supabaseSetting("supabase.url", "CLIPKID_SUPABASE_URL"))
        buildConfigField(
            FieldSpec.Type.STRING,
            "SUPABASE_PUBLISHABLE_KEY",
            supabaseSetting("supabase.publishableKey", "CLIPKID_SUPABASE_PUBLISHABLE_KEY"),
        )
    }
}
