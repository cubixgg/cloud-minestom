plugins {
    `java-platform`
}

description = "Version-aligned BOM for cloud-minestom and the cloud-* versions it requires."

dependencies {
    constraints {
        // project(...), not a hardcoded "gg.cubix.cloudminestom:cloud-minestom:1.0-SNAPSHOT" - picks
        // up this build's own group/version from the root build.gradle.kts `subprojects` block
        // instead of duplicating it here, so a version bump can't drift the BOM out of sync with the
        // artifact it's supposed to pin.
        api(project(":cloud-minestom"))
        api(libs.cloud.core)
        api(libs.cloud.annotations)
        api(libs.cloud.minecraft.extras)
    }
}
