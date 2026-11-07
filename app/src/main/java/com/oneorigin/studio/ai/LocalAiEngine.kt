package com.oneorigin.studio.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** The state the UI may render. There is no hidden "maybe ready" state. */
sealed interface LocalAiState {
    data class Unsupported(val reason: String) : LocalAiState
    data object Idle : LocalAiState
    data class Downloading(val fraction: Float, val downloaded: Long, val total: Long) : LocalAiState
    data object Loading : LocalAiState
    data class Ready(val modelId: String) : LocalAiState
    data object Running : LocalAiState
    data class Failed(val message: String) : LocalAiState
}

data class LocalAiProgress(val downloaded: Long, val total: Long) {
    val fraction: Float
        get() = if (total <= 0L) 0f else (downloaded.toDouble() / total).toFloat().coerceIn(0f, 1f)
}

/**
 * The app-owned boundary around a local model.
 *
 * Everything above this interface (editor, diff review, permissions) is independent of
 * the engine. QVAC is the reference implementation; replacing it later means another
 * implementation, not a rewrite. Prompts never receive network access and a model can
 * only propose text — the app decides what is applied to a file.
 */
interface LocalAiEngine {
    val state: StateFlow<LocalAiState>

    /** Download (if needed) and load the model. Safe to call repeatedly. */
    suspend fun prepare(onProgress: (LocalAiProgress) -> Unit = {}): Result<Unit>

    /** Stream an answer. The caller must collect; cancelling the collection stops it. */
    fun complete(system: String, prompt: String): Flow<String>

    suspend fun cancel()

    suspend fun close()
}
