plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
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

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "org.example.Bot"
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
}
