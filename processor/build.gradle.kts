import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
    kotlin("jvm") version "1.8.10"

    `maven-publish`
}

group = "dev.ruffrick"
version = "0.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":api"))

    // https://mvnrepository.com/artifact/com.google.devtools.ksp/symbol-processing-api
    implementation("com.google.devtools.ksp:symbol-processing-api:1.8.10-1.0.9")

    // https://mvnrepository.com/artifact/com.squareup/kotlinpoet
    implementation("com.squareup:kotlinpoet:1.12.0")

    // https://mvnrepository.com/artifact/com.squareup/kotlinpoet-ksp
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")

    // https://mvnrepository.com/artifact/net.dv8tion/JDA
    implementation("net.dv8tion:JDA:5.0.0-beta.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val sourcesJar = task<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            artifactId = "jda-commands-processor"
            from(components["kotlin"])
            artifact(sourcesJar)
        }
    }
}
