package moe.nyarchlinux.catgirldownloader.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Waits for whichever of [candidates] succeeds first and returns its value together with the
 * winning Deferred. The others keep running (the caller decides what to do with them).
 * If every candidate fails, the last failure is rethrown.
 */
suspend fun <T> firstSuccess(vararg candidates: Deferred<T>): Pair<T, Deferred<T>> = coroutineScope {
    val results = Channel<Pair<Deferred<T>, Result<T>>>(candidates.size)
    val watchers = candidates.map { candidate ->
        launch {
            val result = try {
                Result.success(candidate.await())
            } catch (e: Throwable) {
                // A cancelled *candidate* counts as a failure; our own cancellation propagates.
                if (e is CancellationException && !candidate.isCancelled) throw e
                Result.failure(e)
            }
            results.send(candidate to result)
        }
    }
    var lastError: Throwable? = null
    repeat(candidates.size) {
        val (candidate, result) = results.receive()
        result.onSuccess { value ->
            watchers.forEach { it.cancel() }
            return@coroutineScope value to candidate
        }
        lastError = result.exceptionOrNull()
    }
    throw lastError ?: IllegalStateException("No candidates")
}

/**
 * Waits up to [timeoutMs] for [deferred]. Returns null if it is still running (it is left running),
 * otherwise its success or failure. Avoids runCatching, which would swallow the timeout itself.
 */
suspend fun <T> awaitWithin(deferred: Deferred<T>, timeoutMs: Long): Result<T>? =
    withTimeoutOrNull(timeoutMs) {
        try {
            Result.success(deferred.await())
        } catch (e: CancellationException) {
            if (!deferred.isCancelled) throw e // the timeout or our own cancellation
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
