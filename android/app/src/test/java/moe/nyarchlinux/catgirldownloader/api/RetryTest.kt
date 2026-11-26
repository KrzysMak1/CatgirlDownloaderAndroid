package moe.nyarchlinux.catgirldownloader.api

import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class RetryTest {
    private val url = "https://example.com/x".toHttpUrl()

    @Test
    fun retriesTransientErrorsUntilSuccess() = runBlocking {
        val delays = mutableListOf<Long>()
        var calls = 0
        val result = withRetry(maxAttempts = 4, sleep = { delays += it }) {
            calls++
            if (calls < 3) throw HttpStatusException(429, url, retryAfterSeconds = null)
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(3, calls)
        assertEquals(listOf(1000L, 2000L), delays)
    }

    @Test
    fun honoursRetryAfterWithCap() = runBlocking {
        val delays = mutableListOf<Long>()
        var calls = 0
        withRetry(maxAttempts = 3, sleep = { delays += it }) {
            calls++
            when (calls) {
                1 -> throw HttpStatusException(429, url, retryAfterSeconds = 3)
                2 -> throw HttpStatusException(503, url, retryAfterSeconds = 120)
                else -> Unit
            }
        }
        assertEquals(listOf(3000L, 10_000L), delays)
    }

    @Test
    fun givesUpAfterMaxAttempts() = runBlocking {
        var calls = 0
        try {
            withRetry(maxAttempts = 3, sleep = {}) {
                calls++
                throw SocketTimeoutException("slow")
            }
            fail("expected exception")
        } catch (e: SocketTimeoutException) {
            assertEquals(3, calls)
        }
    }

    @Test
    fun doesNotRetryPermanentErrors() = runBlocking {
        var calls = 0
        try {
            withRetry(maxAttempts = 4, sleep = {}) {
                calls++
                throw NoImageException("no posts")
            }
            fail("expected exception")
        } catch (e: NoImageException) {
            assertEquals(1, calls)
        }
    }

    @Test
    fun classifiesErrors() {
        assertTrue(isRetryable(HttpStatusException(429, url, null)))
        assertTrue(isRetryable(HttpStatusException(502, url, null)))
        assertTrue(isRetryable(IOException("reset")))
        assertFalse(isRetryable(HttpStatusException(404, url, null)))
        assertFalse(isRetryable(NoImageException("none")))
        assertFalse(isRetryable(IllegalStateException()))
    }

    @Test
    fun describesErrorsForUsers() {
        assertEquals("HTTP 429", describeError(HttpStatusException(429, url, null)))
        assertEquals("timeout", describeError(SocketTimeoutException()))
    }
}
