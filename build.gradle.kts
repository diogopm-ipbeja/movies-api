@file:OptIn(OpenApiPreview::class)

import io.ktor.plugin.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

repositories {
    mavenCentral()
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    sourceSets.all {
        languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        languageSettings.optIn("kotlin.io.path.ExperimentalPathApi")
        languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
        languageSettings.optIn("kotlin.time.ExperimentalTime")
    }

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-opt-in=kotlin.io.path.ExperimentalPathApi")
        freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
}

ktor {
    openApi {
        title = "Movies API"
        version = "1.0.0"
        description = "API"
        target = project.layout.buildDirectory.file("resources/openapi/documentation.json")
    }
}




dependencies {
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:1.0.0-rc-2")
    // implementation("org.jetbrains.exposed:exposed-java-time:1.0.0-rc-2")
    // https://mvnrepository.com/artifact/at.favre.lib/bcrypt
    implementation("at.favre.lib:bcrypt:0.10.2")

    implementation("org.postgresql:postgresql:42.7.8")
    // https://mvnrepository.com/artifact/io.r2dbc/r2dbc-postgresql
    // implementation("io.r2dbc:r2dbc-postgresql:0.8.13.RELEASE")
    // implementation("io.github.smiley4:ktor-openapi:5.4.0")
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.sse)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.exposed.core)
    implementation(libs.exposed.json)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.r2dbc)
    implementation(libs.exposed.dao)
    implementation(libs.ktor.server.metrics)

    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.double.receive)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.cors)
    implementation(libs.kotlin.asyncapi.ktor)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
