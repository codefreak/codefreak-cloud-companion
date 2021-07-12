import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("com.google.cloud.tools:jib-spring-boot-extension-gradle:0.1.0")
    }
}

plugins {
    id("org.springframework.boot") version "2.5.2"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.jetbrains.kotlin.kapt") version "1.5.20"
    id("com.google.cloud.tools.jib") version "3.1.1"
    id("com.diffplug.spotless") version "5.14.1"
    kotlin("jvm") version "1.5.20"
    kotlin("plugin.spring") version "1.5.20"
}

group = "org.codefreak.cloud"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("commons-io:commons-io:2.10.0")
    implementation("org.apache.tika:tika-core:1.26")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jib {
    to {
        image = "ghcr.io/henningcash/codefreak-cloud-companion"
    }
    container {
        volumes = listOf(
            "/code"
        )
        labels.put("org.opencontainers.image.source", "https://github.com/HenningCash/codefreak-cloud-companion")
    }
    pluginExtensions {
        pluginExtension {
            implementation = "com.google.cloud.tools.jib.gradle.extension.springboot.JibSpringBootExtension"
        }
    }
}

spotless {
    kotlin {
        ktlint()
    }
}