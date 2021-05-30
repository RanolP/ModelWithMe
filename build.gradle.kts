import kr.entree.spigradle.kotlin.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    id("kr.entree.spigradle") version "2.2.3"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "io.github.ranolp.mwm"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    papermc()
    protocolLib()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(paper("1.16.5"))
    implementation(protocolLib())
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        dependencies {
            include(dependency("org.jetbrains.kotlin::"))
        }
        relocate("kotlin", "io.github.ranolp.mwm.kotlin")
    }
}

artifacts {
    archives(tasks.shadowJar)
}

spigot {
    authors = listOf("Ranol_")
    depends = listOf("ProtocolLib")
    apiVersion = "1.16"
    commands {
        create("mwm") {}
    }
    debug {
        buildVersion = "1.16.5"

    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        useIR = true
        jvmTarget = "11"
    }
}
