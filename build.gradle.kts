plugins {
    kotlin("jvm") version "1.9.10"
    id("com.apollographql.apollo3") version "3.8.5"
    // `maven-publish`
    // signing
}

group = "ai.promptscan"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.apollographql.apollo3:apollo-runtime:3.8.5")
    implementation("com.apollographql.apollo3:apollo-api:3.8.5")

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
    
    // Logging dependencies
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.13")
}

apollo {
    service("api") {
        mapScalar("UUID", "java.util.UUID", "ai.promptscan.sdk.graphql.UUIDAdapter")
        mapScalar("DateTime", "java.time.OffsetDateTime", "ai.promptscan.sdk.graphql.DateTimeAdapter")

        srcDir("src/main/graphql")
        packageName.set("ai.promptscan.sdk.client")
        generateKotlinModels.set(true)
        generateDataBuilders.set(true)
//        generateModelBuilders.set(true)
//        generateKotlinModels.set(false)
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
