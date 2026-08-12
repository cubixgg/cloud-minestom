// Parent build only - no sources of its own (spec.md §3). Real modules are cloud-minestom,
// cloud-minestom-bom and minestom-demo; this file just applies shared config to all of them.

// Reusable library modules published to Reposilite (maven.cubix.gg) on release - not minestom-demo,
// which is a runnable application, not a consumable dependency (docs/decisions/0006).
val publishedModules = setOf("cloud-minestom", "cloud-minestom-bom")

subprojects {
    group = "gg.cubix.cloudminestom"
    version = "1.1.0" // x-release-please-version

    repositories {
        mavenCentral()
    }

    // Skips cloud-minestom-bom on purpose: the java-platform plugin it uses is mutually exclusive
    // with java/java-library, so it never applies JavaBasePlugin and this block simply doesn't run
    // for it.
    plugins.withType<JavaBasePlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
            }
        }
    }

    if (name !in publishedModules) {
        return@subprojects
    }

    apply(plugin = "maven-publish")

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "reposilite"
                url = uri("https://maven.cubix.gg/public-releases")
                credentials {
                    username = System.getenv("REPOSILITE_USERNAME")
                    password = System.getenv("REPOSILITE_PASSWORD")
                }
            }
        }
    }

    // Deferred (plugins.withType, not an immediate components["java"] lookup) for the same reason
    // the toolchain block above is: whether this subproject's own plugins {} block has already run
    // by the time this subprojects {} closure executes isn't something to rely on. cloud-minestom
    // applies java-library (registers the "java" component); cloud-minestom-bom applies java-platform
    // (registers "javaPlatform") - the two publishedModules entries never apply both.
    plugins.withType<JavaLibraryPlugin> {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
        }
    }
    plugins.withType<JavaPlatformPlugin> {
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["javaPlatform"])
                }
            }
        }
    }
}
