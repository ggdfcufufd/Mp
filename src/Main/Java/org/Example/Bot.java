plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.telegram:telegrambots:6.9.7.1")
    implementation("org.telegram:telegrambots-meta:6.9.7.1")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.example.Bot"
    }
}
