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

group = "site.gutschi.medium.compare"
version = "0.0.1-SNAPSHOT"
val javaVersion = JavaVersion.VERSION_17

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.rest-assured:spring-mock-mvc-kotlin-extensions:4.3.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

allOpen {
    annotations("jakarta.persistence.Entity")
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

repositories {
    mavenCentral()
    mavenLocal()
}

sourceSets {
    main {
        kotlin {
            srcDir(layout.buildDirectory.dir("generate-resources/main/src/main/kotlin"))
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = javaVersion.toString()
    kotlinOptions.javaParameters = true
    kotlinOptions.freeCompilerArgs += "-Xjsr305=strict"
    dependsOn(tasks.openApiGenerate)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

hibernate {
    enhancement {
        enableAssociationManagement.set(true)
    }
}
