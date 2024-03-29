plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.asarkar.gradle.build-time-tracker") version "4.3.0"
    id("com.autonomousapps.dependency-analysis") version "1.18.0"
    id("com.github.ben-manes.versions") version "0.44.0"
    id("com.gradle.plugin-publish") version "1.1.0"
    id("io.github.ngyewch.git-describe") version "0.2.0"
    id("se.ascp.gradle.gradle-versions-filter") version "0.1.16"
}

group = "io.github.ngyewch.gradle"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(gradleApi())

    implementation("org.apache.commons:commons-lang3:3.12.0")
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("gradle-projectset-plugin") {
            id = "io.github.ngyewch.projectset"
            displayName = "Gradle project set plugin"
            description = "Gradle project set plugin."
            implementationClass = "com.github.ngyewch.gradle.projectset.ProjectSetPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/ngyewch/gradle-projectset-plugin"
    vcsUrl = "https://github.com/ngyewch/gradle-projectset-plugin.git"
    tags = listOf("git")
}
