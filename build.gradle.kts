import kr.entree.spigradle.kotlin.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.5.10"
    id("kr.entree.spigradle") version "2.2.3"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "io.github.ranolp.mwm"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    papermc()
    protocolLib()
    maven {
        name = "Minecraft Libraries"
        url = URI("https://libraries.minecraft.net")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(paper("1.16.5"))
    implementation("com.destroystokyo.paper", "paper-mojangapi", "1.16.5-R0.1-20210531.062257-331")
    implementation("com.mojang", "brigadier", "1.0.17")
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
        jvmTarget = "11"
    }
}
