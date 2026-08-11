// Parent build only - no sources of its own (spec.md §3). Real modules are cloud-minestom,
// cloud-minestom-bom and minestom-demo; this file just applies shared config to all of them.

subprojects {
    group = "gg.cubix.cloudminestom"
    version = "1.0-SNAPSHOT"

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
}
