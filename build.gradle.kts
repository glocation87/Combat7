plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "io.github.glocation87"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")

    testImplementation("io.papermc.paper:paper-api:26.2.build.129-stable")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    runServer {
        minecraftVersion("26.2")
        jvmArgs("-Dcom.mojang.eula.agree=true")
        // 25565 is the shared test-server, 25566 is Nature7
        // offline mode so the test bots can join without accounts
        args("--port", "25567", "--online-mode", "false")
        // mineflayer only speaks 26.1, Via translates for it
        downloadPlugins {
            url("https://cdn.modrinth.com/data/P1OZGk5p/versions/TEgYlalY/ViaVersion-5.12.1-SNAPSHOT.jar")
            url("https://cdn.modrinth.com/data/NpvuJQoq/versions/w6P38zDf/ViaBackwards-5.12.1-SNAPSHOT.jar")
        }
    }

    test {
        useJUnitPlatform()
        // MockBukkit's ByteBuddy still uses sun.misc.Unsafe
        jvmArgs("--sun-misc-unsafe-memory-access=allow")
    }
}
