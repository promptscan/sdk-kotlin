package ai.promptscan.sdk

import ai.promptscan.sdk.client.CollectGenerationsMutation
import ai.promptscan.sdk.client.type.GenerationInput
import ai.promptscan.sdk.client.type.KeyValuePairInput
import ai.promptscan.sdk.graphql.GraphQLClient
import com.apollographql.apollo3.api.Optional
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import java.util.logging.LogManager
import kotlin.test.Test
import kotlin.test.assertEquals


class PromptScanSDKTest {

    @BeforeEach
    fun setUp() {
        val stream = PromptScanSDKTest::class.java.getResourceAsStream("/logging.properties")
        LogManager.getLogManager().readConfiguration(stream)

        unmockkAll()
        mockkConstructor(GraphQLClient::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testCollectGenerationsWithMultipleApiKeys() = runBlocking {
        coEvery {
            anyConstructed<GraphQLClient>().mutation(any<CollectGenerationsMutation>(), any())
        } answers {
            CollectGenerationsMutation.Data(CollectGenerationsMutation.Collect(success = true, error = null))
        }

        val sdk = PromptScanSDK.builder().apiKey("key-default").autoFlush(false).build()

        sdk.collectGeneration(
            GenerationInput(id = Optional.Present("a"), model = "gpt-4o-mini", messages = ArrayList()),
            "key-a"
        )
        sdk.collectGeneration(
            GenerationInput(id = Optional.Present("b"), model = "gpt-4o-mini", messages = ArrayList()),
            "key-b"
        )
        sdk.collectGeneration(
            GenerationInput(id = Optional.Present("c"), model = "gpt-4o-mini", messages = ArrayList()),
            "key-a"
        )
        sdk.collectGeneration(
            GenerationInput(id = Optional.Present("d"), model = "gpt-4o-mini", messages = ArrayList())
        )

        assertEquals(sdk.estimateGenerationsInFlightCount(), 4)
        sdk.flush()

        assertEquals(sdk.estimateGenerationsInFlightCount(), 0)

        coVerify(exactly = 1) { anyConstructed<GraphQLClient>().mutation(any<CollectGenerationsMutation>()) }

        coVerify(exactly = 1) {
            anyConstructed<GraphQLClient>().mutation(
                any<CollectGenerationsMutation>(),
                "key-b"
            )
        }

        coVerify(exactly = 1) {
            anyConstructed<GraphQLClient>().mutation(
                any<CollectGenerationsMutation>(),
                null
            )
        }

        val slot = slot<CollectGenerationsMutation>()
        coVerify(exactly = 1) {
            anyConstructed<GraphQLClient>().mutation(
                capture(slot),
                "key-a"
            )
        }
        assertEquals(setOf("a", "c"), slot.captured.generations.map { it.id.getOrNull() }.toSet())
    }

    @Test
    fun testRetryOnFailedFlush() = runBlocking {
        coEvery {
            anyConstructed<GraphQLClient>().mutation(any<CollectGenerationsMutation>(), any())
        } answers {
            throw Exception("Failed to flush")
        } andThenAnswer {
            CollectGenerationsMutation.Data(CollectGenerationsMutation.Collect(success = false, error = null))
        } andThenAnswer {
            CollectGenerationsMutation.Data(CollectGenerationsMutation.Collect(success = true, error = null))
        }

        val sdk = PromptScanSDK.builder().autoFlush(false).build();
        sdk.collectGeneration(GenerationInput(id = Optional.Present("a"), model = "m", messages = ArrayList()))
        assertEquals(sdk.estimateGenerationsInFlightCount(), 1)

        sdk.flush()
        assertEquals(sdk.estimateGenerationsInFlightCount(), 1)

        sdk.flush()
        assertEquals(sdk.estimateGenerationsInFlightCount(), 1)

        sdk.flush()
        assertEquals(sdk.estimateGenerationsInFlightCount(), 0)
    }

    @Test
    fun testAutoFlush() = runBlocking {
        val sdk = PromptScanSDK.builder().flushIntervalMillis(50L).build()
        mockkObject(sdk)
        coEvery { sdk.flush() } returns listOf<GenerationRecord>()
        clearMocks(sdk)
        delay(100L)
        sdk.close()
        coVerify(atLeast = 1, atMost = 3) { sdk.flush() }
        unmockkObject(sdk)
    }

    @Test
    fun testFlushOnClose() = runBlocking {
        val sdk = PromptScanSDK.builder().autoFlush(false).build()
        mockkObject(sdk)
        coEvery { sdk.flush() } returns listOf<GenerationRecord>()
        sdk.close()
        coVerify(exactly = 1) { sdk.flush() }
        unmockkObject(sdk)
    }

    @Test
    fun testEnabled() = runBlocking {
        coEvery {
            anyConstructed<GraphQLClient>().mutation(any<CollectGenerationsMutation>(), any())
        } answers {
            CollectGenerationsMutation.Data(CollectGenerationsMutation.Collect(success = true, error = null))
        }

        val sdk = PromptScanSDK.builder().enabled(false).autoFlush(false).build()
        sdk.collectGeneration(GenerationInput(id = Optional.Present("a"), model = "m", messages = ArrayList()))
        sdk.flush()

        coVerify(exactly = 0) {
            anyConstructed<GraphQLClient>().mutation(any<CollectGenerationsMutation>())
        }

        sdk.setEnabled(true)
        sdk.collectGeneration(GenerationInput(id = Optional.Present("a"), model = "m", messages = ArrayList()))
        sdk.flush()

        coVerify(exactly = 1) {
            anyConstructed<GraphQLClient>().mutation(any<CollectGenerationsMutation>())
        }

        clearConstructorMockk(GraphQLClient::class, answers = false)
        sdk.setEnabled(false)
        sdk.collectGeneration(GenerationInput(id = Optional.Present("a"), model = "m", messages = ArrayList()))
        sdk.flush()

        coVerify(exactly = 0) {
            anyConstructed<GraphQLClient>().mutation(any<CollectGenerationsMutation>())
        }
    }


    @Test
    fun testDebugLogging() = runBlocking {        
        val logger = LoggerFactory.getLogger(PromptScanSDK::class.java)
        assertTrue(logger.isWarnEnabled)
        assertFalse(logger.isInfoEnabled)
        // TODO: implement dynamic log level change
    }

    @Test
    fun testDefaultMeta() = runBlocking {
        coEvery {
            anyConstructed<GraphQLClient>().mutation(any<CollectGenerationsMutation>(), any())
        } answers {
            CollectGenerationsMutation.Data(CollectGenerationsMutation.Collect(success = true, error = null))
        }

        val defaultTags = mapOf("env" to "test", "version" to "1.0")
        val sdk = PromptScanSDK.builder()
            .apiKey("test-key")
            .defaultTags(defaultTags)
            .autoFlush(false)
            .build()
            
        sdk.collectGeneration(GenerationInput(
            id = Optional.Present("a"),
            model = "m",
            messages = ArrayList(),
            tags = Optional.Present(listOf(KeyValuePairInput("version", "2.0"), KeyValuePairInput("app", "demo")))
        ))

        val generations = sdk.flush()
        assertEquals(generations.size, 1)
        assertEquals(
            generations[0].generation.tags.getOrNull()!!.toTypedArray().associate { it.key to it.value },
            mapOf("env" to "test", "version" to "2.0", "app" to "demo")
        )
    }
}
