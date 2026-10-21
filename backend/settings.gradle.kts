plugins {
    // Downloads JDK 25 automatically when it isn't installed (build.gradle.kts asks for a 25 toolchain).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "liftoff"
