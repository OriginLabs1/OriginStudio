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
        // Two groups live only on JitPack: the Termux terminal libraries and QVAC's
        // bare-rpc-kotlin dependency. Keep JitPack in an exclusive content block so no
        // other dependency can ever be substituted from there.
        exclusiveContent {
            forRepository { maven("https://jitpack.io") }
            filter {
                includeGroupByRegex("com\\.github\\.termux\\..*")
                includeGroupByRegex("com\\.github\\.holepunchto\\..*")
            }
        }
        // The QVAC Maven repository is vendored and hash-verified by
        // tools/fetch-qvac.py, not resolved from a mutable remote.
        val qvac = settingsDir.resolve(".qvac/qvac-maven-repository")
        if (qvac.isDirectory) {
            maven { url = uri(qvac) }
        }
    }
}

rootProject.name = "OriginStudio"
include(":app")
include(":core")
