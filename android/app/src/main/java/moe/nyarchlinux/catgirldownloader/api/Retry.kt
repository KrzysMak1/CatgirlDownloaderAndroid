package moe.nyarchlinux.catgirldownloader.api

import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException

private const val MAX_DELAY_MS = 10_000L

/** Rate limits (429), server errors (5xx) and network failures are worth another try. */
internal fun isRetryable(e: Throwable): Boolean = when (e) {
    is HttpStatusException -> e.code == 429 || e.code in 500..599
    is NoImageException -> false
    is IOException -> true
    else -> false
}

/** Waits the server's Retry-After if given, otherwise 1 s, 2 s, 4 s, ... capped at 10 s. */
internal fun retryDelayMillis(e: Throwable, attempt: Int): Long {
    val retryAfter = (e as? HttpStatusException)?.retryAfterSeconds
    val millis = retryAfter?.let { it * 1000L } ?: (1000L shl attempt.coerceAtMost(4))
    return millis.coerceIn(0L, MAX_DELAY_MS)
}

/** Runs [block], retrying transient failures; the last failure is rethrown. */
suspend fun <T> withRetry(
    maxAttempts: Int = 4,
    sleep: suspend (Long) -> Unit = { delay(it) },
    block: suspend () -> T,
): T {
    var attempt = 0
    while (true) {
        try {
            return block()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            attempt++
            if (attempt >= maxAttempts || !isRetryable(e)) throw e
            sleep(retryDelayMillis(e, attempt - 1))
        }
    }
}

/** Short, user-facing reason for a failed load, e.g. "HTTP 429" or "timeout". */
fun describeError(e: Throwable): String = when (e) {
    is HttpStatusException -> "HTTP ${e.code}"
    is SocketTimeoutException -> "timeout"
    is NoImageException -> e.message ?: "no image"
    is java.net.UnknownHostException -> "no connection"
    else -> e.javaClass.simpleName
}
