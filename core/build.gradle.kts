plugins {
    kotlin("jvm")
}

// The Origin Core is deliberately a plain JVM library, not part of the Android module.
// The constitution requires a small, portable trusted core; keeping it free of Android
// imports is what makes it testable on the JVM and reusable by the reference host.
kotlin {
    jvmToolchain(21)
}

dependencies {
    // Android already ships org.json at runtime. compileOnly keeps the library out of the
    // APK while still letting the core compile; the JVM tests provide a real implementation.
    compileOnly("org.json:json:20240303")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed")
    }
}
