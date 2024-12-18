plugins {
    kotlin("jvm") version "1.9.10"
    id("com.apollographql.apollo3") version "3.8.5"
    `maven-publish`
    signing
    java
}

group = "ai.promptscan"
version = "0.1.2"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.apollographql.apollo3:apollo-runtime:3.8.5")
    implementation("com.apollographql.apollo3:apollo-api:3.8.5")

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")

    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.slf4j:slf4j-jdk14:2.0.16")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

apollo {
    service("api") {
        mapScalar("UUID", "java.util.UUID", "ai.promptscan.sdk.graphql.UUIDAdapter")
        mapScalar("DateTime", "java.time.OffsetDateTime", "ai.promptscan.sdk.graphql.DateTimeAdapter")

        srcDir("src/main/graphql")
        packageName.set("ai.promptscan.sdk.client")
        generateKotlinModels.set(true)
        generateDataBuilders.set(true)
        generateOptionalOperationVariables.set(false)
        introspection {
            endpointUrl.set("http://localhost:8020/graphql/")
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
        
        includes.add("**/*.gql")
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // Set to your desired JDK version
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.test {
    useJUnitPlatform()
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/promptscan/sdk-kotlin")
            credentials {
                username = (project.findProperty("gpr.user") as? String) ?: System.getenv("USERNAME") ?: System.getenv("GITHUB_ACTOR")
                password = (project.findProperty("gpr.token") as? String) ?: System.getenv("TOKEN") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("gpr") {
            groupId = "ai.promptscan"
            artifactId = "promptscan-sdk"
            version = project.version.toString()
            from(components["java"])

            pom {
                name.set("PromptScan SDK Kotlin")
                description.set("Kotlin SDK for PromptScan API")
                url.set("https://github.com/promptscan/sdk-kotlin")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}
