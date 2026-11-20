// Unit tests without the IDE, for the pull request check: only the classes that need no IntelliJ Platform.
// The full build (plugin, verifier, platform tests) runs locally from the root project.
plugins {
    kotlin("jvm") version "2.4.0"
}

repositories { mavenCentral() }

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation("junit:junit:4.13.2")
}

kotlin { jvmToolchain(21) }

sourceSets {
    main {
        kotlin {
            srcDir("stubs")
            srcDir("../src/main/kotlin")
            include("**/model/**", "**/store/ProfileStore.kt", "**/layout/Geometry.kt", "com/intellij/**")
        }
    }
    test {
        kotlin {
            srcDir("../src/test/kotlin")
            include("**/store/**", "**/layout/GeometryTest.kt")
        }
    }
}
