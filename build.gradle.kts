plugins {
    kotlin("jvm") version "1.9.10"
    id("org.jetbrains.intellij") version "1.16.0"
}

group = "com.localblocks"
version = "1.0.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

intellij {
    version.set("2023.3")
    type.set("PS")
    sandboxDir.set("/tmp/local-blocks-sandbox3")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

tasks.named("buildSearchableOptions") {
    enabled = false
}

tasks.patchPluginXml {
    sinceBuild.set("233")
    untilBuild.set("253.*")
}

tasks.named<Zip>("buildPlugin") {
    archiveFileName.set("plugin-phpstorm-drafts-${project.version}.zip")
}
