package ai.promptscan.example;

import ai.promptscan.sdk.PromptScanSDK;
import ai.promptscan.sdk.client.type.*;
import com.apollographql.apollo3.api.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.LogManager;

public class PromptScanExample {
    private static final Logger logger = LoggerFactory.getLogger(PromptScanExample.class);

    public static void main(String[] args) throws IOException {
        InputStream configFile = PromptScanExample.class.getClassLoader().getResourceAsStream("logging.properties");
        LogManager.getLogManager().readConfiguration(configFile);

        logger.info("Starting PromptScan SDK example");

        String apiKey = System.getenv("PROMPTSCAN_API_KEY");
        if (apiKey == null) {
            apiKey = "project-f47ac10b-58cc-4372-a567-0e02b2c3d479";
        }
        
        String baseUrl = System.getenv("PROMPTSCAN_BASE_URL");
        if (baseUrl == null) {
            baseUrl = "http://localhost:8020/graphql/";
        }

        PromptScanSDK sdk = PromptScanSDK.Companion.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .debug(true)
            .build();

        GenerationInput input = new GenerationInput(
            Optional.Companion.present(UUID.randomUUID().toString()),
            Optional.Companion.present(UUID.randomUUID().toString()),
            "gpt-4o-mini",
            Arrays.asList(
                new GenerationMessageInput(
                    "system",
                    "You are a helpful assistant!",
                    Optional.Companion.absent()
                ),
                new GenerationMessageInput(
                    "user",
                    "Hi!",
                    Optional.Companion.absent()
                )
            ),
            Optional.present(new UsageInput(
                Optional.Companion.present(20),
                Optional.Companion.present(20),
                Optional.Companion.present(40),
                Optional.Companion.present(new PromptTokensDetailsInput(
                    Optional.Companion.present(0),
                    Optional.Companion.present(0)
                )),
                Optional.Companion.present(new CompletionTokensDetailsInput(
                    Optional.Companion.present(0),
                    Optional.Companion.present(0)
                ))
            )),
            Optional.Companion.present(List.of(new KeyValuePairInput(
                "user_id",
                "xyz"
            ))),
            Optional.present(OffsetDateTime.now()),
            Optional.present(0.125),
            Optional.present(0.450)
        );

        sdk.collectGeneration(input);

        sdk.close();
    }
}
