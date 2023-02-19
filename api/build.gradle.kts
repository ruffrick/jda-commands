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
    // https://mvnrepository.com/artifact/net.dv8tion/JDA
    compileOnly("net.dv8tion:JDA:5.0.0-beta.3")

    // https://github.com/ruffrick/jda-kotlinx
    api("com.github.ruffrick:jda-kotlinx:9c48cfb")
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
            artifactId = "jda-commands-api"
            from(components["kotlin"])
            artifact(sourcesJar)
        }
    }
}
