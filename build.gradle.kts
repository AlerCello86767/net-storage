plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.4.2"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    // ✅ 添加 SQLite 驱动依赖 - 使用 implementation 打包进插件
    //implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    implementation("com.h2database:h2:2.2.224")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    // ✅ 配置 shadowJar 打包依赖
    shadowJar {
        relocate("org.h2", "com.AlerCello86767.net_storage.libs.h2")
        archiveClassifier.set("")
        mergeServiceFiles()
    }

    runServer {
        minecraftVersion("1.21.1")
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}