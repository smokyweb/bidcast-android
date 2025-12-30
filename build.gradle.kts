plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hiltAndroid) apply false
    alias(libs.plugins.devtools) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

gradle.projectsEvaluated {
    tasks.withType<JavaCompile> {
        val compilerArgs = options.compilerArgs
        compilerArgs.add("-Xlint:deprecation")
        compilerArgs.add("-Xlint:unchecked")
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}