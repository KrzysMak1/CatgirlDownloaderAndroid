package moe.nyarchlinux.catgirldownloader.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class RaceTest {
    // Like viewModelScope: a failing candidate must not cancel its siblings.
    private val scope = CoroutineScope(SupervisorJob())

    @Test
    fun returnsFirstSuccessAndLeavesTheOtherRunning(): Unit = runBlocking {
        val slow = CompletableDeferred<String>()
        val fast = scope.async { delay(10); "fast" }
        val (value, winner) = firstSuccess(slow, fast)
        assertEquals("fast", value)
        assertSame(fast, winner)
        assertTrue("loser must not be cancelled", slow.isActive)
        slow.complete("slow")
        Unit
    }

    @Test
    fun skipsFailuresAndUsesTheOtherResult(): Unit = runBlocking {
        val failing = scope.async<String> { throw IOException("boom") }
        val ok = scope.async { delay(20); "ok" }
        val (value, winner) = firstSuccess(failing, ok)
        assertEquals("ok", value)
        assertSame(ok, winner)
    }

    @Test
    fun throwsWhenAllFail(): Unit = runBlocking {
        val a = scope.async<String> { throw IOException("a") }
        val b = scope.async<String> { delay(5); throw IOException("b") }
        try {
            firstSuccess(a, b)
            fail("expected exception")
        } catch (e: IOException) {
            assertEquals("b", e.message)
        }
    }

    @Test
    fun awaitWithinReturnsNullAndKeepsSlowWorkRunning(): Unit = runBlocking {
        val slow = CompletableDeferred<String>()
        assertEquals(null, awaitWithin(slow, 50))
        assertTrue("slow work must keep running after the timeout", slow.isActive)
        slow.complete("done")
        assertEquals("done", awaitWithin(slow, 50)?.getOrNull())
    }

    @Test
    fun awaitWithinReportsFailures(): Unit = runBlocking {
        val failing = scope.async<String> { throw IOException("x") }
        assertEquals("x", awaitWithin(failing, 1000)?.exceptionOrNull()?.message)
        val cancelled = CompletableDeferred<String>().also { it.cancel() }
        assertTrue(awaitWithin(cancelled, 1000)!!.isFailure)
    }
}
