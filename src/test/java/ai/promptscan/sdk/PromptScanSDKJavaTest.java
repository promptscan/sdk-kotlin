package ai.promptscan.sdk;

import ai.promptscan.sdk.client.type.*;
import com.apollographql.apollo3.api.Optional;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PromptScanSDKJavaTest {

    private PromptScanSDK sdk;

    @Test
    public void testGenerationsCollection() throws InterruptedException {
        sdk.collectGeneration(createGeneration());
        assertEquals(1, sdk.estimateGenerationsInFlightCount());

        BuildersKt.runBlocking(Dispatchers.getDefault(), (scope, continuation) -> {
            return sdk.flush(continuation);
        });

        assertEquals(0, sdk.estimateGenerationsInFlightCount());
    }

    @BeforeEach
    public void setUp() {
        sdk = PromptScanSDK.Companion.builder()
            .apiKey("project-f47ac10b-58cc-4372-a567-0e02b2c3d479")
            .baseUrl("http://localhost:8020/graphql/")
            .debug(true)
            .build();
    }

    @AfterEach
    public void tearDown() {
        sdk.close();
    }

    public GenerationInput createGeneration() {
        return new GenerationInput(
            Optional.present(UUID.randomUUID().toString()),
            Optional.present(UUID.randomUUID().toString()),
            "gpt-4o-mini",
            Arrays.asList(
                new GenerationMessageInput("system", "You are a helpful assistant!", Optional.absent()),
                new GenerationMessageInput("user", "Hi!", Optional.absent())
            ),
            Optional.present(new UsageInput(
                Optional.present(20),
                Optional.present(20),
                Optional.present(40),
                Optional.present(new PromptTokensDetailsInput(
                    Optional.present(0),
                    Optional.present(0)
                )),
                Optional.present(new CompletionTokensDetailsInput(
                    Optional.present(0),
                    Optional.present(0)
                ))
            )),
            Optional.present(List.of(new KeyValuePairInput("user_id", "xyz"))),
            Optional.present(OffsetDateTime.now()),
            Optional.present(0.125),
            Optional.present(0.450)
        );
    }
}
