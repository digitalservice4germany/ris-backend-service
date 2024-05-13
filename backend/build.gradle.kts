import com.adarshr.gradle.testlogger.theme.ThemeType
import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.CsvReportRenderer
import com.github.jk1.license.render.ReportRenderer
import io.franzbecker.gradle.lombok.task.DelombokTask
import org.flywaydb.gradle.task.FlywayMigrateTask
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    id("com.diffplug.spotless") version "6.25.0"
    id("org.sonarqube") version "5.0.0.4638"
    id("com.github.jk1.dependency-license-report") version "2.7"
    id("com.gorylenko.gradle-git-properties") version "2.4.1"
    id("com.adarshr.test-logger") version "4.0.0"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("io.franzbecker.gradle-lombok") version "5.0.0"
    id("org.flywaydb.flyway") version "10.12.0"
}

group = "de.bund.digitalservice"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven {
        setUrl("https://plugins.gradle.org/m2/")
    }
    maven {
        url = uri("https://maven.pkg.github.com/digitalservicebund/neuris-juris-xml-export")
        credentials {
            username = System.getenv("GH_PACKAGES_REPOSITORY_USER")
            password = System.getenv("GH_PACKAGES_REPOSITORY_TOKEN")
        }
    }
}

sourceSets {
    create("migration") {
        java.srcDir("src/main/java")
        java.include("db/migration/**/*.java")
    }
}

jacoco {
    toolVersion = "0.8.8"
}

lombok {
    version = "1.18.32"
}

springBoot {
    buildInfo()
}

testlogger {
    theme = ThemeType.MOCHA
}

configurations {
    compileOnly {
        extendsFrom(annotationProcessor.get())
    }
}

spotless {
    java {
        removeUnusedImports()
        googleJavaFormat()
        custom("Refuse wildcard imports") {
            // Wildcard imports can't be resolved by spotless itself.
            // This will require the developer themselves to adhere to best practices.
            if (it.contains("\nimport .*\\*;".toRegex())) {
                throw AssertionError("Do not use wildcard imports. 'spotlessApply' cannot resolve this issue.")
            }
            it
        }
    }
    format("misc") {
        target(
            "**/*.js",
            "**/*.json",
            "**/*.md",
            "**/*.properties",
            "**/*.sh",
            "**/*.sql",
            "**/*.yaml",
            "**/*.yml"
        )
        targetExclude("frontend/**", "**/dist/**", "**/static/**")
        // spotless:off
        prettier(
            mapOf(
                "prettier" to "2.8.4",
                "prettier-plugin-properties" to "0.2.0",
                "prettier-plugin-sh" to "0.12.8",
                "prettier-plugin-sql" to "0.13.0"
            )
        ).config(
            mapOf(
                "keySeparator" to "=", // for prettier-plugin-properties
                "language" to "postgresql" // for prettier-plugin-sql
            )
        )
        // spotless:on
    }
}

licenseReport {
    allowedLicensesFile = File("$projectDir/../allowed-licenses.json")
    renderers = arrayOf<ReportRenderer>(CsvReportRenderer("backend-licence-report.csv"))
    filters = arrayOf<DependencyFilter>(
        LicenseBundleNormalizer(
            "$projectDir/license-normalizer-bundle.json",
            true
        )
    )
}

sonar {
    properties {
        property("sonar.projectKey", "digitalservicebund_ris-backend-service")
        property("sonar.organization", "digitalservicebund")
        property("sonar.host.url", "https://sonarcloud.io")
        property(
            "sonar.coverage.exclusions",
            "**/config/**,**/S3AsyncMockClient.java,**/Application.java"
        )
    }
}

dependencies {
    val testContainersVersion = "1.19.7"

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-resource-server:6.2.3")

    // CVE-2024-22262
    implementation("org.springframework:spring-web:6.1.6")

    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.5.0")

    implementation("org.springframework.cloud:spring-cloud-starter-kubernetes-client-config:3.1.1")

    // CVE-2024-26308
    implementation("org.apache.commons:commons-compress:1.26.1")
    // CVE-2022-3171
    implementation("com.google.protobuf:protobuf-java:4.26.1")
    // CVE-2023-52428 in spring-boot-starter-oauth2-client:3.2.3
    implementation("com.nimbusds:nimbus-jose-jwt:9.38")
    // CVE-2023-31582
    implementation("org.bitbucket.b_c:jose4j:0.9.6")

    implementation("org.postgresql:postgresql:42.7.3")

    implementation("com.sendinblue:sib-api-v3-sdk:7.0.0")
    // CVE-2022-4244
    implementation("org.codehaus.plexus:plexus-utils:4.0.1")
    // CVE-2024-29025
    implementation("io.netty:netty-codec-http:4.1.109.Final")
    implementation("io.netty:netty-codec-http2:4.1.109.Final")

    implementation(platform("software.amazon.awssdk:bom:2.25.38"))
    implementation("software.amazon.awssdk:netty-nio-client")
    implementation("software.amazon.awssdk:s3")

    implementation("org.docx4j:docx4j-JAXB-ReferenceImpl:11.4.11")
    implementation("org.freehep:freehep-graphicsio-emf:2.4")

    implementation("jakarta.mail:jakarta.mail-api:2.1.3")
    implementation("org.eclipse.angus:angus-mail:2.0.3")
    implementation("com.icegreen:greenmail:2.1.0-alpha-4")

    // package served by private repo, requires authentication:
    implementation("de.bund.digitalservice:neuris-juris-xml-export:0.8.40") {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
    // for local development:
    // implementation(files("../../neuris-juris-xml-export/build/libs/neuris-juris-xml-export-0.8.38.jar"))

    implementation("de.bund.digitalservice:neuris-caselaw-migration-schema:0.0.4")
    // for local development:
    // implementation(files("../../ris-data-migration/schema/build/libs/schema-0.0.4.jar"))

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.17.0")

    implementation("io.micrometer:micrometer-registry-prometheus:1.12.5")
    implementation("io.micrometer:micrometer-core:1.12.5")

    implementation(platform("io.sentry:sentry-bom:7.9.0"))
    implementation("io.sentry:sentry-spring-boot-starter-jakarta")
    implementation("io.sentry:sentry-logback")

    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1")
    // => CVE-2023-2976
    implementation("com.google.guava:guava:33.2.0-jre")

    var flywayCore = "org.flywaydb:flyway-core:10.12.0"
    implementation(flywayCore)
    "migrationImplementation"(flywayCore)
    runtimeOnly("org.flywaydb:flyway-database-postgresql:10.12.0")

    implementation("io.getunleash:unleash-client-java:9.2.0")
    implementation("org.apache.commons:commons-text:1.12.0")
    implementation("org.jsoup:jsoup:1.17.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")

    testImplementation("io.projectreactor:reactor-test:3.6.5")
    testImplementation("org.springframework.security:spring-security-test:6.2.4")
    testImplementation("org.springframework.security:spring-security-test:6.2.4")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:postgresql:$testContainersVersion")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

project.tasks.sonar {
    dependsOn("jacocoTestReport")
}

tasks {
    register<FlywayMigrateTask>("migrateDatabaseForERD") {
        url = System.getenv("DB_URL")
        user = System.getenv("DB_USER")
        password = System.getenv("DB_PASSWORD")
        locations = arrayOf(
            "filesystem:src/main/resources/db/migration/",
            "classpath:db/migration"
        )
        dependsOn("compileMigrationJava")
    }

    jar {
        enabled = false
    }

    withType<Test> {
        maxParallelForks = Runtime.getRuntime().availableProcessors().div(2)
    }

    getByName<Test>("test") {
        useJUnitPlatform {
            excludeTags("integration", "manual")
        }
    }

    task<Test>("integrationTest") {
        description = "Runs the integration tests."
        group = "verification"
        useJUnitPlatform {
            includeTags("integration")
            excludeTags("manual")
        }

        // So that running integration test require running unit tests first,
        // and we won"t even attempt running integration tests when there are
        // failing unit tests.
        dependsOn("test")
        finalizedBy("jacocoTestReport")
    }

    check {
        dependsOn("integrationTest")
    }

    jacocoTestReport {
        // Jacoco hooks into all tasks of type: Test automatically, but results for each of these
        // tasks are kept separately and are not combined out of the box. we want to gather
        // coverage of our unit and integration tests as a single report!
        dependsOn("integrationTest")
        executionData(fileTree(project.buildDir.absolutePath).include("jacoco/*.exec"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude("**/config/**")
                }
            })
        )
    }

    bootBuildImage {
        val containerRegistry = System.getenv("CONTAINER_REGISTRY") ?: "ghcr.io"
        val containerImageName =
            System.getenv("CONTAINER_IMAGE_NAME")
                ?: "digitalservicebund/${rootProject.name}"
        val containerImageVersion = System.getenv("CONTAINER_IMAGE_VERSION") ?: "latest"

        imageName.set("$containerRegistry/$containerImageName:$containerImageVersion")
        builder.set("paketobuildpacks/builder-jammy-tiny")
        publish.set(false)

        docker {
            publishRegistry {
                username.set(System.getenv("CONTAINER_REGISTRY_USER") ?: "")
                password.set(System.getenv("CONTAINER_REGISTRY_PASSWORD") ?: "")
                url.set("https://$containerRegistry")
            }
        }
    }

    val delombok by registering(DelombokTask::class) {
        dependsOn(compileJava)
        mainClass.set("lombok.launch.Main")
        val outputDir by extra { file("$buildDir/delombok") }
        outputs.dir(outputDir)
        sourceSets["main"].java.srcDirs.forEach {
            inputs.dir(it)
            args(it, "-d", outputDir)
        }
        doFirst {
            outputDir.delete()
        }
    }

    javadoc {
        dependsOn(delombok)
        val outputDir: File by delombok.get().extra
        source = fileTree(outputDir)
        isFailOnError = false
    }
}
