pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // The Termux terminal libraries live only on JitPack. Keep JitPack in an exclusive
        // content block so no other dependency can ever be substituted from there.
        exclusiveContent {
            forRepository { maven("https://jitpack.io") }
            filter {
                includeGroupByRegex("com\\.github\\.termux\\..*")
            }
        }
    }
}

rootProject.name = "OriginStudio"
include(":app")
include(":core")
