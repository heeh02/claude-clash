plugins {
    kotlin("android")
    id("com.android.library")
    id("kotlinx-serialization")
}

android {
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/prebuilt")
        }
    }
}

dependencies {
    implementation(project(":common"))

    implementation(libs.androidx.core)
    implementation(libs.kotlin.coroutine)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.yaml.snakeyaml)
}

val fetchNativeCore = tasks.register("fetchNativeCore", Exec::class) {
    workingDir = rootProject.rootDir
    inputs.file(rootProject.file("scripts/fetch-native-core.sh"))
    outputs.dir(rootProject.file("core/src/main/prebuilt"))
    commandLine("bash", rootProject.file("scripts/fetch-native-core.sh").absolutePath)
}

tasks.named("preBuild") {
    dependsOn(fetchNativeCore)
}
