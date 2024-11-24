package ai.promptscan.sdk

import ai.promptscan.sdk.client.ApiKeyQuery
import ai.promptscan.sdk.client.CollectGenerationsMutation
import ai.promptscan.sdk.client.type.*
import ai.promptscan.sdk.graphql.GraphQLClient
import com.apollographql.apollo3.api.Optional
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphQLClientTest {

    private val baseUrl = "http://localhost:8020/graphql/"
    private val defaultApiKey = "project-f47ac10b-58cc-4372-a567-0e02b2c3d479"

    @Test
    fun testQueryMissingApiKey() {
        val graphQLClient = GraphQLClient.create(baseUrl)
        val exception = assertThrows<IllegalArgumentException> {
            runBlocking {
                graphQLClient.query(ApiKeyQuery())
            }
        }
        assertEquals("No API Key was provided", exception.message)
    }

    @Test
    fun testQuerySuccess() = runBlocking {
        val graphQLClient = GraphQLClient.create(baseUrl, defaultApiKey)
        val apiKey = graphQLClient.query(ApiKeyQuery()).apiKey!!
        assertTrue(apiKey.enabled)
        assertEquals("default", apiKey.name)
        assertEquals("project", apiKey.scope)
    }

    @Test
    fun testCollectGeneration() = runBlocking {
        val graphQLClient = GraphQLClient.create(baseUrl, defaultApiKey)
        val res = graphQLClient.mutation(
            CollectGenerationsMutation(
                generations = listOf(
                    GenerationInput(
                        Optional.present("session-id"),
                        Optional.present("generation-id"),
                        "gpt-4o-mini",
                        listOf(GenerationMessageInput(role="user", content="hello")),
                        Optional.present(UsageInput(
                            Optional.present(20),
                            Optional.present(30),
                            Optional.present(50),
                            Optional.present(PromptTokensDetailsInput(
                                Optional.present(0),
                                Optional.present(0)
                            )),
                            Optional.present( CompletionTokensDetailsInput(
                                Optional.present(0),
                                Optional.present(0)
                            ))
                        )),
                        Optional.present(listOf(KeyValuePairInput(key="key", value="value"))),
                        Optional.present(OffsetDateTime.now()),
                        Optional.present(0.245),
                        Optional.present(0.123)
                    )
                ),
                projectId = null
            )
        ).collect
        assertTrue(res.success)
    }
}
