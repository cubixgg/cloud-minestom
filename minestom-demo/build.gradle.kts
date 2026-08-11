plugins {
    application
}

description = "Runnable example server exercising every feature cloud-minestom ships (spec.md §13)."

dependencies {
    implementation(project(":cloud-minestom"))
    implementation(libs.cloud.annotations)
}

application {
    mainClass = "gg.cubix.cloudminestom.demo.Main"
}
