plugins {
    `java-platform`
}

description = "Version-aligned BOM for cloud-minestom and the cloud-* versions it requires."

dependencies {
    constraints {
        // Filled in P10 (spec.md §3.2): cloud-minestom, cloud-core, cloud-annotations,
        // cloud-minecraft-extras, all pinned from gradle/libs.versions.toml.
    }
}
