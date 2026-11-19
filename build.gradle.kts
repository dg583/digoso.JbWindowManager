import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.intellij.platform") version "2.20.1"
}

group = "com.digoso.jbwindowmanager"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        // riderPath (in ~/.gradle/gradle.properties) = lokale Rider-Installation, sonst Download
        val riderPath = providers.gradleProperty("riderPath").orNull
        if (riderPath != null) local(riderPath) else create(IntelliJPlatformType.Rider, "2026.3") { useInstaller = false }
        // ponytail: nur Rider als Ziel; weitere IDEs später über platform-unabhängige Module
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

kotlin { jvmToolchain(21) }

intellijPlatform {
    pluginConfiguration {
        ideaVersion { sinceBuild = "263" }
    }
    pluginVerification {
        // Interne API (DesktopLayout) ist bewusst akzeptiert (nötig für floatingBounds), nur echte Brüche lassen die Prüfung scheitern
        failureLevel = listOf(
            org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
        )
        ides {
            val riderPath = providers.gradleProperty("riderPath").orNull
            if (riderPath != null) local(riderPath) else create(IntelliJPlatformType.Rider, "2026.3") { useInstaller = false }
        }
    }
}
