package ai.promptscan.sdk

import ai.promptscan.sdk.client.CollectGenerationsMutation
import ai.promptscan.sdk.client.type.GenerationInput
import ai.promptscan.sdk.graphql.GraphQLClient
import kotlinx.coroutines.*
import java.io.Closeable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(PromptScanSDK::class.java)

class PromptScanSDK private constructor(
    val graphQLClient: GraphQLClient,
    private val generations: ConcurrentLinkedQueue<GenerationRecord>,
    private val flushIntervalMillis: Long,
    private val maxRetries: Int,
    private var autoFlush: Boolean,
    private var enabled: Boolean
) : Closeable {
    companion object {
        private const val DEFAULT_API_KEY_ENV = "PROMPTSCAN_API_KEY"
        private const val DEFAULT_BASE_URL_ENV = "PROMPTSCAN_BASE_URL"
        private const val DEFAULT_BASE_URL = "https://api.promptscan.ai/graphql/"

        class Builder {
            private var apiKey: String? = System.getenv(DEFAULT_API_KEY_ENV)
            private var baseUrl: String = System.getenv(DEFAULT_BASE_URL_ENV) ?: DEFAULT_BASE_URL
            private var flushIntervalMillis: Long = 5000L
            private var autoFlush: Boolean = true
            private var maxRetries: Int = 3
            private var enabled: Boolean = true

            fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
            fun baseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }
            fun flushIntervalMillis(flushIntervalMillis: Long) = apply { this.flushIntervalMillis = flushIntervalMillis }
            fun autoFlush(autoFlush: Boolean) = apply { this.autoFlush = autoFlush }
            fun enabled(enabled: Boolean) = apply { this.enabled = enabled }
            fun maxRetries(maxRetries: Int) = apply { this.maxRetries = maxRetries }

            fun build(): PromptScanSDK {
                if (apiKey == null) {
                    logger.warn("PromptScanSDK is initialized without API key")
                }

                val graphQLCLient = GraphQLClient.create(
                    baseUrl = baseUrl,
                    apiKey = apiKey
                )
                
                return PromptScanSDK(
                    graphQLCLient,
                    ConcurrentLinkedQueue<GenerationRecord>(),
                    flushIntervalMillis,
                    maxRetries,
                    autoFlush,
                    enabled
                )
            }
        }

        fun builder(): Builder = Builder()
    }

    private val isClosed = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var flushJob: Job? = null
    private val shutdownHook = Thread {
        runBlocking {
            close()
        }
    }

    init {
        Runtime.getRuntime().addShutdownHook(shutdownHook)
        setAutoFlush(autoFlush)
    }

    private fun startPeriodicFlush() {
        flushJob = scope.launch {
            while (isActive) {
                flush()
                delay(flushIntervalMillis)
            }
        }
    }

    fun setAutoFlush(autoFlush: Boolean) {
        if (autoFlush && !this.autoFlush) {
            startPeriodicFlush()
        } else if (!autoFlush && this.autoFlush) {
            flushJob?.cancel()
        }
        this.autoFlush = autoFlush
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun collectGeneration(generation: GenerationInput, apikey: String? = null) {
        if (!enabled) {
            return
        }
        generations.offer(GenerationRecord(generation, apikey))
    }

    fun collectGeneration(generation: GenerationInput) {
        if (!enabled) {
            return
        }
        generations.offer(GenerationRecord(generation, null))
    }

    fun estimateGenerationsInFlightCount(): Int {
        return generations.size
    }

    suspend fun flush(): List<GenerationRecord> {
        // Pop all elements from the queue into a local list
        val collectableGenerations = mutableListOf<GenerationRecord>()

        while (true) {
            val gen = generations.poll() ?: break
            collectableGenerations.add(gen)
        }

        if (!enabled) {
            return collectableGenerations
        }

        val flushedGenerations = mutableListOf<GenerationRecord>()

        // Group the generations by API key
        val grouped = collectableGenerations
            .filter { it.retries < maxRetries }
            .groupBy { it.apiKey }
            .map { (key, value) ->  key to value }

        for ((apiKey, values) in grouped) {
            var retry: Boolean
            try {
                val res = graphQLClient.mutation(
                    CollectGenerationsMutation(values.map { it.generation }, null),
                    apiKey)
                retry = !res.collect.success

                if (res.collect.success) {
                    flushedGenerations.addAll(values)
                }
            } catch(e: Exception) {
                logger.error("Failed to collect generations: ${e.message}", e)
                retry = true
            }

            if (retry && !isClosed.get()) {
                // Requeue the generations
                values.forEach {
                    it.retries += 1
                    generations.offer(it)
                }
            }
        }

        return flushedGenerations
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            logger.warn("PromptScanSDK is already closed")
            return
        }

        flushJob?.cancel()
        scope.cancel()

        runBlocking {
            flush()
        }

        this.graphQLClient.close()
    }
}


class GenerationRecord(
    val generation: GenerationInput,
    val apiKey: String?
) {
    var retries = 0
}