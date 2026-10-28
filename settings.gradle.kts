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
        exclusiveContent {
            forRepository {
                maven {
                    name = "LibXposedMavenCentral"
                    url = uri("https://repo.maven.apache.org/maven2")
                }
            }
            filter {
                includeGroup("io.github.libxposed")
            }
        }
        exclusiveContent {
            forRepository {
                maven {
                    name = "MiuixGitHubPackages"
                    url = uri("https://maven.pkg.github.com/compose-miuix-ui/miuix")
                    credentials {
                        val miuixToken =
                            providers.environmentVariable("MIUIX_GITHUB_TOKEN").orNull?.takeIf { it.isNotBlank() }
                        val githubToken =
                            providers.environmentVariable("GITHUB_TOKEN").orNull?.takeIf { it.isNotBlank() }
                        username = when {
                            miuixToken != null ->
                                providers.environmentVariable("MIUIX_GITHUB_ACTOR").orNull?.takeIf { it.isNotBlank() }
                                    ?: providers.environmentVariable("GITHUB_ACTOR").orNull?.takeIf { it.isNotBlank() }
                            githubToken != null ->
                                providers.environmentVariable("GITHUB_ACTOR").orNull?.takeIf { it.isNotBlank() }
                                    ?: providers.environmentVariable("MIUIX_GITHUB_ACTOR").orNull?.takeIf { it.isNotBlank() }
                            else -> providers.gradleProperty("gpr.user").orNull?.takeIf { it.isNotBlank() }
                        }
                        password = miuixToken ?: githubToken
                            ?: providers.gradleProperty("gpr.key").orNull?.takeIf { it.isNotBlank() }
                    }
                }
            }
            filter {
                includeGroup("top.yukonga.miuix.kmp")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "Guiyuan"
include(":app")

// Opt-in source-matched OS4 graph. The default app still uses published MIUIX packages.
providers.gradleProperty("gy.os4.miuixDir").orNull?.takeIf(String::isNotBlank)?.let { source ->
    val miuix = file(source)
    require(miuix.resolve("miuix-blur/build.gradle.kts").isFile &&
        miuix.resolve("miuix-ui/build.gradle.kts").isFile
    ) { "gy.os4.miuixDir must point to a complete MIUIX source checkout" }

    includeBuild(miuix) {
        dependencySubstitution {
            substitute(module("top.yukonga.miuix.kmp:miuix-core-android")).using(project(":miuix-core"))
            substitute(module("top.yukonga.miuix.kmp:miuix-shader-android")).using(project(":miuix-shader"))
            substitute(module("top.yukonga.miuix.kmp:miuix-squircle-android")).using(project(":miuix-squircle"))
            substitute(module("top.yukonga.miuix.kmp:miuix-ui-android")).using(project(":miuix-ui"))
            substitute(module("top.yukonga.miuix.kmp:miuix-blur-android")).using(project(":miuix-blur"))
            substitute(module("top.yukonga.miuix.kmp:miuix-preference-android")).using(project(":miuix-preference"))
            substitute(module("top.yukonga.miuix.kmp:miuix-icons-android")).using(project(":miuix-icons"))
            substitute(module("top.yukonga.miuix.kmp:miuix-nav-android")).using(project(":miuix-nav"))
        }
    }
}
