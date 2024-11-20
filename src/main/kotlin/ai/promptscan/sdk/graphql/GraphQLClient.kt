package ai.promptscan.sdk.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import java.io.Closeable

class GraphQLClient private constructor(
    private val apolloClient: ApolloClient,
    private val defaultApiKey: String? = null
) : Closeable {
    companion object {
        fun create(baseUrl: String, apiKey: String? = null): GraphQLClient {
            val apolloClient = ApolloClient.Builder()
                .serverUrl(baseUrl)
//                .addCustomScalarAdapter(CustomScalarType("DateTime", Instant::class.java), DateTimeAdapter)
                .build()
            
            return GraphQLClient(apolloClient, apiKey)
        }
    }

    suspend fun <D : Query.Data> query(operation: Query<D>, apiKey: String? = null): D {
        return apolloClient
            .query(operation)
            .addHttpHeader("Authorization", "Bearer ${ensureApiKey(apiKey)}")
            .execute().data ?: throw IllegalStateException("No data received from the server")
    }

    suspend fun <D : Mutation.Data> mutation(operation: Mutation<D>, apiKey: String? = null): D {
        return apolloClient
            .mutation(operation)
            .addHttpHeader("Authorization", "Bearer ${ensureApiKey(apiKey)}")
            .execute().data ?: throw IllegalStateException("No data received from the server")
    }


    private fun ensureApiKey(apiKey: String?): String {
        if (apiKey != null) {
            return apiKey
        }

        if (defaultApiKey != null) {
            return defaultApiKey
        }

        throw IllegalArgumentException("No API Key was provided")
    }

    override fun close() {
        apolloClient.close()
    }
}
