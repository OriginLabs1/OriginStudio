package com.oneorigin.studio.ai

import android.content.Context
import android.os.StatFs
import io.tether.qvac.sdk.QvacClient
import io.tether.qvac.sdk.QvacCompletionEvent
import io.tether.qvac.sdk.QvacCompletionOptions
import io.tether.qvac.sdk.QvacCompletionRun
import io.tether.qvac.sdk.QvacGenerationOptions
import io.tether.qvac.sdk.QvacMessage
import io.tether.qvac.sdk.barekit.AndroidServiceTransport
import io.tether.qvac.sdk.completion
import io.tether.qvac.sdk.generated.ModelConstant
import io.tether.qvac.sdk.generated.Models
import io.tether.qvac.sdk.QvacProgressEvent
import io.tether.qvac.sdk.models
import io.tether.qvac.sdk.run
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * QVAC implementation of [LocalAiEngine].
 *
 * The worker runs in `:qvac_worker`; this class only holds a Binder transport and the
 * conversation history. It refuses to construct a client unless [LocalAiGate] allows it,
 * so a 32-bit or pre-API-29 process never loads a native library.
 */
class QvacEngine(
    context: Context,
    private val model: ModelConstant = Models.QWEN3_600M_INST_Q4,
) : LocalAiEngine {

    private val appContext = context.applicationContext
    private val mutex = Mutex()

    private val _state = MutableStateFlow<LocalAiState>(
        if (LocalAiGate.isSupported()) {
            LocalAiState.Idle
        } else {
            LocalAiState.Unsupported(LocalAiGate.unsupportedReason())
        },
    )
    override val state: StateFlow<LocalAiState> = _state.asStateFlow()

    @Volatile private var transport: AndroidServiceTransport? = null
    @Volatile private var client: QvacClient? = null
    @Volatile private var loadedModelId: String? = null
    @Volatile private var activeRun: QvacCompletionRun? = null

    override suspend fun prepare(onProgress: (LocalAiProgress) -> Unit): Result<Unit> = mutex.withLock {
        if (!LocalAiGate.isSupported()) {
            _state.value = LocalAiState.Unsupported(LocalAiGate.unsupportedReason())
            return@withLock Result.failure(IllegalStateException("Local AI is not supported on this device"))
        }
        loadedModelId?.let {
            _state.value = LocalAiState.Ready(it)
            return@withLock Result.success(Unit)
        }

        try {
            requireFreeSpace()
            _state.value = LocalAiState.Downloading(0f, 0L, model.expectedSize)
            val client = connect()

            client.models.downloadWithProgress(model.src).collect { event ->
                when (event) {
                    is QvacProgressEvent.Progress -> {
                        val downloaded = event.value.downloaded.toLong()
                        val total = event.value.total.toLong().takeIf { it > 0L } ?: model.expectedSize
                        _state.value = LocalAiState.Downloading(
                            fraction = (event.value.percentage / 100.0).toFloat().coerceIn(0f, 1f),
                            downloaded = downloaded,
                            total = total,
                        )
                        onProgress(LocalAiProgress(downloaded, total))
                    }

                    is QvacProgressEvent.Result -> {
                        val response = event.value
                        if (!response.success) {
                            throw LocalAiException(response.error ?: "The model download failed")
                        }
                    }
                }
            }

            _state.value = LocalAiState.Loading
            val loaded = client.models.load(model)
            val id = loaded.modelId
                ?: throw LocalAiException("QVAC loaded the model without returning an id")
            loadedModelId = id
            _state.value = LocalAiState.Ready(id)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            _state.value = LocalAiState.Failed(error.message ?: "Local AI could not start")
            Result.failure(error)
        }
    }

    override fun complete(system: String, prompt: String): Flow<String> = flow {
        val client = client ?: throw LocalAiException("Local AI is not prepared")
        val modelId = loadedModelId ?: throw LocalAiException("No model is loaded")

        _state.value = LocalAiState.Running
        val run = client.completion.run(
            modelId = modelId,
            history = listOf(
                QvacMessage.system(system),
                QvacMessage.user(prompt),
            ),
            options = QvacCompletionOptions(
                generation = QvacGenerationOptions(
                    temperature = 0.2,
                    topP = 0.9,
                    predict = 768,
                ),
                stream = true,
            ),
        )
        activeRun = run
        try {
            run.events.collect { event ->
                when (event) {
                    is QvacCompletionEvent.ContentDelta -> emit(event.text)
                    is QvacCompletionEvent.Done -> {
                        event.error?.let { throw LocalAiException(it) }
                    }

                    else -> Unit
                }
            }
        } finally {
            activeRun = null
            if (_state.value is LocalAiState.Running) {
                val id = loadedModelId
                _state.value = if (id != null) LocalAiState.Ready(id) else LocalAiState.Idle
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun cancel() {
        val run = activeRun ?: return
        withContext(NonCancellable) {
            runCatching { run.cancel() }
        }
    }

    override suspend fun close() = mutex.withLock {
        runCatching { activeRun?.cancel() }
        activeRun = null
        loadedModelId = null
        withContext(NonCancellable) {
            runCatching { transport?.close() }
        }
        transport = null
        client = null
        if (LocalAiGate.isSupported()) {
            _state.value = LocalAiState.Idle
        }
    }

    private suspend fun connect(): QvacClient {
        client?.let { return it }
        val created = AndroidServiceTransport.connect(appContext)
        val qvac = QvacClient(created)
        transport = created
        client = qvac
        return qvac
    }

    private fun requireFreeSpace() {
        val stats = StatFs(appContext.filesDir.absolutePath)
        val available = stats.availableBytes
        val required = maxOf(MIN_FREE_BYTES, model.expectedSize * 2)
        if (available < required) {
            throw LocalAiException(
                "Local AI needs about ${required / GIB} GB free for the model and its cache.",
            )
        }
    }

    private companion object {
        const val GIB = 1L shl 30
        const val MIN_FREE_BYTES = 2L * GIB
    }
}

class LocalAiException(message: String) : Exception(message)

/** Prompts are plain text and contain only what the user is looking at. */
object LocalAiPrompts {
    private const val SYSTEM = """
        You are a precise coding assistant inside Origin Studio, an Android development
        workspace. Answer for the file the user is looking at. Be short, concrete and
        correct. Do not invent APIs. If you propose a change, return the complete
        replacement for the selected text only, with no commentary around it.
    """

    fun explain(path: String, code: String): String = """
        Explain what this file does and point out the one thing most likely to cause a
        bug. Keep it under 200 words.

        File: $path
        ```
        $code
        ```
    """.trimIndent()

    fun fix(path: String, code: String, error: String): String = """
        Find the cause of the error and return only the fixed file content. Preserve the
        existing style and every unrelated line.

        File: $path
        Error:
        ```
        $error
        ```
        Content:
        ```
        $code
        ```
    """.trimIndent()

    fun ask(question: String, path: String, code: String): String = """
        Question: $question

        File: $path
        ```
        $code
        ```
    """.trimIndent()

    fun system(): String = SYSTEM.trimIndent()
}
