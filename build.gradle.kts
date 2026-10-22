import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("com.diffplug.spotless") version "8.10.4"
}

group = "adp"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    annotationProcessor(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("info.picocli:picocli-spring-boot-starter:4.7.7")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("tools.jackson.core:jackson-databind")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName = "adp-forecast.jar"
    // sqlite-jdbc loads a native library; without this JDK 25 warns on every run.
    manifest {
        attributes("Enable-Native-Access" to "ALL-UNNAMED")
    }
}

// Only the executable jar is needed.
tasks.jar {
    enabled = false
}

spotless {
    java {
        // 1.37.0 fails inside Spotless 8.10.4; 1.36.1 is its bundled default.
        googleJavaFormat("1.36.1")
    }
}
