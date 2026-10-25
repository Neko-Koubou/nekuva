pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io") {
            content {
                includeGroupByRegex("com\\.github.*")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        // KCEF (Desktop embedded Chromium) pulls JOGL (org.jogamp:*) which lives on jogamp's repo.
        maven(url = "https://jogamp.org/deployment/maven")
    }
}

val extsDir = file("../nekuva-exts")
if (extsDir.exists() && file("../nekuva-exts/build.gradle.kts").exists()) {
    includeBuild(extsDir) {
        dependencySubstitution {
            substitute(module("com.github.Neko-Koubou:nekuva-exts")).using(project(":"))
        }
    }
}

include(":composeApp")
rootProject.name = "Nekuva"
