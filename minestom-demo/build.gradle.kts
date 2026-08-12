plugins {
    application
}

description = "Runnable example server exercising every feature cloud-minestom ships (spec.md §13)."

dependencies {
    implementation(project(":cloud-minestom"))
    implementation(libs.cloud.annotations)

    // cloud-minestom and Minestom itself only depend on slf4j-api; without a concrete binding on the
    // classpath, every log call is a silent no-op. This is the runnable demo, not the library, so it
    // picks one - slf4j-simple needs no config file and logs straight to the console.
    runtimeOnly(libs.slf4j.simple)
}

application {
    mainClass = "gg.cubix.cloudminestom.demo.Main"
}
