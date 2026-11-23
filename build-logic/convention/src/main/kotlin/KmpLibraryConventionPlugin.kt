import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Base setup for every Feelo KMP module:
 * Android (AGP 9 `com.android.kotlin.multiplatform.library`) + iosArm64 + iosSimulatorArm64,
 * shared SDK levels from the version catalog, and the common test stack.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        kotlin.apply {
            iosArm64()
            iosSimulatorArm64()

            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                namespace = feeloNamespace
                compileSdk = libs.version("android-compileSdk").toInt()
                minSdk = libs.version("android-minSdk").toInt()
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
                // Runs commonTest on the JVM as part of `check`.
                withHostTest { }
            }

            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }

            sourceSets.apply {
                getByName("commonTest").dependencies {
                    implementation(libs.lib("kotlin-test"))
                    implementation(libs.lib("kotlinx-coroutines-test"))
                    implementation(libs.lib("turbine"))
                }
            }
        }
    }
}
