import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
    kotlin("jvm") version "1.5.20"

    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
    kotlin("plugin.serialization") version "1.5.20"

    `maven-publish`
}

group = "dev.ruffrick"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib
    implementation(kotlin("stdlib"))

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    implementation(kotlin("reflect"))

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")

    // https://github.com/DV8FromTheWorld/JDA/
    compileOnly("net.dv8tion:JDA:4.3.0_285")

    // https://mvnrepository.com/artifact/org.reflections/reflections
    implementation("org.reflections:reflections:0.9.12")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

publishing {
    publications {
        create<MavenPublication>("Release") {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String

            from(components["kotlin"])
        }
    }
}
