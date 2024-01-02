import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.hibernate.orm") version "6.4.1.Final"
    id("org.graalvm.buildtools.native") version "0.9.28"
    id("org.openapi.generator") version "7.2.0"
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.spring") version "1.9.21"
    kotlin("plugin.jpa") version "1.9.21"
    kotlin("plugin.allopen") version "1.9.21"
}

allOpen {
    annotations("jakarta.persistence.Entity")
}

group = "site.gutschi.medium.compare"
version = "0.0.1-SNAPSHOT"
val javaVersion = JavaVersion.VERSION_21

java {
    sourceCompatibility = javaVersion
}

kotlin {
    jvmToolchain(javaVersion.ordinal + 1)
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        kotlin {
            srcDir(layout.buildDirectory.dir("generate-resources/main/src/main/kotlin"))
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = javaVersion.majorVersion
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

openApiGenerate {
    inputSpec.set("$rootDir/../openapi.yaml")
    generatorName.set("kotlin-spring")
    modelNamePrefix.set("Gen")
    configOptions.put("interfaceOnly", "true")
    configOptions.put("useSpringBoot3", "true")
    configOptions.put("useTags", "true")
    configOptions.put("documentationProvider", "none")
    configOptions.put("useBeanValidation", "false")
    configOptions.put("exceptionHandler", "false")
}
tasks.compileKotlin { dependsOn(tasks.openApiGenerate) }

hibernate {
    enhancement {
        enableAssociationManagement.set(true)
    }
}
