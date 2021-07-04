pluginManagement {
    val kotlinVersion: String by settings
    val springBootVersion: String by settings
    val springDependencyManagement: String by settings
    val jibVersion: String by settings
    val ktlintPluginVersion: String by settings
    val detektVersion: String by settings
    val gitPropertiesVersion: String by settings
    val versionsVersion: String by settings

    plugins {
        id("org.springframework.boot") version springBootVersion
        id("io.spring.dependency-management") version springDependencyManagement
        id("com.google.cloud.tools.jib") version jibVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintPluginVersion
        id("io.gitlab.arturbosch.detekt") version detektVersion
        id("com.github.ben-manes.versions") version versionsVersion
        id("com.gorylenko.gradle-git-properties") version gitPropertiesVersion

        kotlin("plugin.spring") version kotlinVersion
        kotlin("jvm") version kotlinVersion
    }
    repositories {
        gradlePluginPortal()
    }
}
rootProject.name = "monitoring"
