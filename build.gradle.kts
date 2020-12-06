import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.4.0"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    id("io.gitlab.arturbosch.detekt") version "1.10.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    id("com.google.cloud.tools.jib") version "2.6.0"
    id("com.gorylenko.gradle-git-properties") version "2.2.2"

    val kotlinVersion = "1.4.10"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
}

ktlint {
    version.set("0.39.0")
    verbose.set(true)
    coloredOutput.set(true)
}

group = "com.hometech.discount"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_11

detekt {
    toolVersion = "1.10.0"
    input = files("src/main/kotlin")
    config = files("detekt.yml")
    reports {
        html {
            enabled = true
            destination = file("detekt-report.html")
        }
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.flywaydb:flyway-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("io.github.microutils:kotlin-logging:1.6.24")
    implementation("org.telegram:telegrambots-spring-boot-starter:5.0.1")
    implementation("org.jsoup:jsoup:1.13.1")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}
