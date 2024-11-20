## Adding PromptScan SDK to your project

Add the following to your `build.gradle` file:

```groovy
dependencies {
    implementation 'ai.promptscan.sdk:sdk-kotlin:0.1.0'
}
```

For Kotlin DSL (`build.gradle.kts`):

```kotlin
dependencies {
    implementation("ai.promptscan.sdk:sdk-kotlin:0.1.0")
}
```

## Maven

Add the following to your `pom.xml` file:

```xml
<dependency>
    <groupId>ai.promptscan</groupId>
    <artifactId>promptscan-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Requirements

- Java 17 or later
- Kotlin 1.9.10 or later


### Dependencies

The SDK includes the following main dependencies:

- Apollo GraphQL Client 3.8.5
- Kotlin Coroutines 1.7.3
- SLF4J API 2.0.9 (for logging)


## Using SDK with Java

Configuring SDK:

```java
import ai.promptscan.sdk.PromptScanSDK;

// When using SaaS with default endpoint
PromptScanSDK sdk = PromptScanSDK.Companion.builder().apiKey("<API-KEY>").build();

// When using enterprise deployment
PromptScanSDK sdk = PromptScanSDK.Companion.builder().apiKey("<API-KEY>").baseUrl("<BASE_URL>").build();
```
Sending generation to the server:

```java
import ai.promptscan.sdk.client.type.GenerationInput;
import ai.promptscan.sdk.client.type.GenerationMessageInput;
import ai.promptscan.sdk.client.type.KeyValuePairInput;
import ai.promptscan.sdk.client.type.UsageInput;
import com.apollographql.apollo3.api.Optional;

GenerationInput generation = new GenerationInput(
    Optional.present("xyz"),
    Optional.present("b"),
    "gpt-4o-mini",
    Arrays.asList(
        new GenerationMessageInput("system", "You are a helpful assistant!", Optional.absent()),
        new GenerationMessageInput("user", "Hi!", Optional.absent())
    ),
    Optional.present(
        new UsageInput(
            Optional.present(20),
            Optional.present(20),
            Optional.present(40),
            Optional.absent(),
            Optional.absent()
        )
    ),
    Optional.present(Arrays.asList(new KeyValuePairInput("user_id", "xyz"))),
    Optional.present(OffsetDateTime.now()),
    Optional.present(0.125),
    Optional.present(0.450)
);

sdk.collectGeneration(generation);
```

You can collect generations to different projects by using different project API keys:

```java
sdk.collectGeneration(generation, projectApiKey);
```

## Advanced collector configuration

By default SDK will flush all generations to the server every 5 seconds.
SDK registers a shutdown hook to flush generations remaining in the queue when JVM shuts down.

Both behaviors can be changed configured. For example, you can disable automatic flushing and control it your code:

```java
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

PromptScanSDK sdk = PromptScanSDK.Companion.builder.autoFlush(false).build();

// Flush records from your own code.
BuildersKt.runBlocking(Dispatchers.getDefault(), (scope, continuation) -> {
    return sdk.flush(continuation);
});


```