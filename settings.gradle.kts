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
