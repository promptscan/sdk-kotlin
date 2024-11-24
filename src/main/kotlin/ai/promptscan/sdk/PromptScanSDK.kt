package ai.promptscan.sdk

import ai.promptscan.sdk.client.CollectGenerationsMutation
import ai.promptscan.sdk.client.type.GenerationInput
import ai.promptscan.sdk.client.type.KeyValuePairInput
import ai.promptscan.sdk.graphql.GraphQLClient
import com.apollographql.apollo3.api.Optional
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
    private var enabled: Boolean,
    val defaultMeta: Map<String, String>
) : Closeable {
    companion object {
        class Builder {
            private var apiKey: String? = System.getenv("PROMPTSCAN_API_KEY")
            private var baseUrl: String = System.getenv("PROMPTSCAN_BASE_URL") ?: "https://api.promptscan.ai/graphql/"
            private var flushIntervalMillis: Long = 5000L
            private var autoFlush: Boolean = true
            private var maxRetries: Int = 3
            private var enabled: Boolean = true
            private var defaultMeta: Map<String, String> = emptyMap()

            fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
            fun baseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }
            fun flushIntervalMillis(flushIntervalMillis: Long) = apply { this.flushIntervalMillis = flushIntervalMillis }
            fun autoFlush(autoFlush: Boolean) = apply { this.autoFlush = autoFlush }
            fun enabled(enabled: Boolean) = apply { this.enabled = enabled }
            fun maxRetries(maxRetries: Int) = apply { this.maxRetries = maxRetries }
            fun defaultMeta(defaultMeta: Map<String, String>) = apply { this.defaultMeta = defaultMeta }

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
                    enabled,
                    defaultMeta
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
            .map {
                val meta = mutableMapOf<String, String>(*defaultMeta.toList().toTypedArray())
                if (it.generation.meta.getOrNull() != null) {
                    for (pair in it.generation.meta.getOrNull()!!) {
                        meta[pair.key] = pair.value
                    }
                }

                it.generation = it.generation.copy(meta = Optional.present(meta.toList().map { (key, value) ->
                    KeyValuePairInput(key, value)
                }))

                it
            }
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
    var generation: GenerationInput,
    val apiKey: String?
) {
    var retries = 0
}