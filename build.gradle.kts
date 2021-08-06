import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("com.google.cloud.tools:jib-spring-boot-extension-gradle:0.1.0")
    }
}

plugins {
    id("org.springframework.boot") version "2.5.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.jetbrains.kotlin.kapt") version "1.5.21"
    id("com.google.cloud.tools.jib") version "3.1.2"
    id("com.diffplug.spotless") version "5.14.2"
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
    maven(url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    maven(url = "https://repo.spring.io/snapshot") // for graphql-spring-boot-starter until it's in stable
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.experimental:graphql-spring-boot-starter:1.0.0-SNAPSHOT")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.apache.tika:tika-core:1.26")
    // https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/org/jetbrains/pty4j/pty4j/
    implementation("org.jetbrains.pty4j:pty4j:0.11.5")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("org.springframework.graphql:spring-graphql-test:1.0.0-SNAPSHOT")
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
    from {
        //image = "replco/polygott:c82c08a720ba1fd537d4fba17eed883ab87c0fd7"
    }
    to {
        image = "ghcr.io/codefreak/codefreak-cloud-companion"
    }
    container {
        volumes = listOf(
            "/code"
        )
        labels.put("org.opencontainers.image.source", "https://github.com/codefreak/codefreak-cloud-companion")
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