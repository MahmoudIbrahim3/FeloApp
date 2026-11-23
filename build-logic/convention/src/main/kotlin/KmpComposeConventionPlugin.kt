import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `feelo.kmp.library` + Compose Multiplatform: compiler, core UI artifacts,
 * Compose resources (module-private `Res` class) and multiplatform @Preview support.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("feelo.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        (kotlin as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
            // Required so Compose resources are packaged into the Android library.
            androidResources { enable = true }
        }

        kotlin.sourceSets.apply {
            getByName("commonMain").dependencies {
                implementation(libs.lib("compose-runtime"))
                implementation(libs.lib("compose-foundation"))
                implementation(libs.lib("compose-ui"))
                implementation(libs.lib("compose-components-resources"))
                implementation(libs.lib("compose-uiToolingPreview"))
            }
            getByName("androidMain").dependencies {
                implementation(libs.lib("compose-uiTooling"))
            }
        }

        extensions.getByType<ComposeExtension>().extensions.configure<ResourcesExtension> {
            packageOfResClass = "$feeloNamespace.resources"
            publicResClass = false
            generateResClass = ResourcesExtension.ResourceClassGeneration.Auto
        }
    }
}
