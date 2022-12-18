val detektVersion: String by project
val ktlintVersion: String by project
val kotlinLoggingVersion: String by project
val testcontainersVersion: String by project
val kotestVersion: String by project
val springCloudVersion: String by project
val telegramBotsVersion: String by project
val jsoupVersion: String by project
val mockkVersion: String by project
val exposedVersion: String by project
val jacocoVersion: String by project

plugins {
    jacoco

    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.google.cloud.tools.jib")
    id("com.github.ben-manes.versions")
    id("com.gorylenko.gradle-git-properties")

    kotlin("jvm")
    kotlin("plugin.spring")
}

tasks {
    detekt {
        toolVersion = detektVersion
        input = files("src/main/kotlin")
        config = files("detekt.yml")
        reports {
            html {
                enabled = true
                destination = file("detekt-report.html")
            }
        }
    }
    ktlint {
        version.set(ktlintVersion)
        verbose.set(true)
        coloredOutput.set(true)
    }
    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
            jvmTarget = "11"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
            jvmTarget = "11"
        }
    }
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed", "standard_out", "standard_error")
            showStandardStreams = true
            setExceptionFormat("full")
        }
        setFinalizedBy(setOf(jacocoTestReport))
    }
    jacocoTestReport {
        classDirectories.setFrom(
            sourceSets.main.get().output.asFileTree.matching {
                val excludes = listOf(
                    "com/hometech/**/configuration/**",
                    "com/hometech/**/*Application.kt",
                    "com/hometech/**/extensions/**"
                )
                this.exclude(excludes)
            }
        )
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        dependsOn(test)
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.flywaydb:flyway-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql")

    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("org.telegram:telegrambots-spring-boot-starter:$telegramBotsVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "com.vaadin.external.google", module = "android-json")
        exclude(module = "mockito-core")
    }
    testImplementation("com.ninja-squad:springmockk:$mockkVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

jacoco {
    toolVersion = jacocoVersion
}
