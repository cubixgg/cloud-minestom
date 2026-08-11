plugins {
    application
}

description = "Runnable example server exercising every feature cloud-minestom ships (spec.md §13)."

dependencies {
    implementation(project(":cloud-minestom"))
    implementation(libs.cloud.annotations)
}

// application.mainClass is wired up in P11 once minestom-demo's Main class exists.
