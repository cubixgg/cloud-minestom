plugins {
    `java-library`
}

description = "Cloud v2 command framework integration for Minestom."

dependencies {
    api(libs.cloud.core)
    api(libs.minestom)
    api(libs.cloud.minecraft.extras)

    // Optional for consumers (spec.md §10): cloud-annotations is platform-agnostic and works against
    // MinestomCommandManager without any Minestom-specific glue, so it's compileOnly here rather than
    // api - projects that don't use annotation-declared commands don't need it on their classpath.
    compileOnly(libs.cloud.annotations)

    implementation(libs.slf4j.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.cloud.annotations)
    testImplementation(libs.minestom.testing)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()

    // net.minestom:testing's @EnvTest asserts on this flag (ConnectionManager#createPlayer) before
    // it will let a virtual player connect outside a virtual thread; mirrors Minestom's own build.
    jvmArgs("-Dminestom.inside-test=true")
}
